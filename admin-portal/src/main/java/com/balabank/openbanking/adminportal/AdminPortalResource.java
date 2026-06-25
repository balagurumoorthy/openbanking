package com.balabank.openbanking.adminportal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Thin admin portal over APISIX: tier (consumer group) + per-minute limit come from the
 * APISIX Admin API; live usage comes from the Redis counters APISIX's limit-count writes.
 * "Upgrade" simply moves the consumer to a higher consumer-group via the Admin API — no
 * counting or limit logic lives here or in the ASPSP.
 */
@Path("/")
public class AdminPortalResource {

    private static final List<String> TIERS = List.of("silver", "gold", "diamond");

    @Inject
    Template admin; // templates/admin.qute.html

    @Inject
    ApisixAdminClient apisix;

    @Inject
    RedisDataSource redis;

    @ConfigProperty(name = "portal.consumer")
    String consumer;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance page() {
        return admin.data(state());
    }

    @GET
    @Path("/api/state")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> apiState() {
        return state();
    }

    @POST
    @Path("/upgrade")
    public Response upgrade(@FormParam("tier") String tier) {
        if (!TIERS.contains(tier)) {
            throw new BadRequestException("unknown tier: " + tier);
        }
        // Read the consumer, change only its group_id, and PUT it back (preserves jwt-auth).
        JsonNode value = apisix.getValue("/apisix/admin/consumers/" + consumer);
        ObjectNode body = (ObjectNode) value;
        body.put("group_id", tier);
        body.remove("update_time");
        body.remove("create_time");
        apisix.put("/apisix/admin/consumers", body);
        return Response.seeOther(URI.create("/")).build();
    }

    private Map<String, Object> state() {
        String tier = apisix.getValue("/apisix/admin/consumers/" + consumer).path("group_id").asText("silver");
        int limit = apisix.getValue("/apisix/admin/consumer_groups/" + tier)
                .path("plugins").path("limit-count").path("count").asInt(0);
        int used = currentUsage();
        int remaining = Math.max(0, limit - used);
        int pct = limit == 0 ? 0 : Math.min(100, used * 100 / limit);
        return Map.of(
                "consumer", consumer,
                "tier", tier.toUpperCase(),
                "tierRaw", tier,
                "limit", limit,
                "used", used,
                "remaining", remaining,
                "pct", pct,
                "tiers", TIERS);
    }

    /** Sum APISIX limit-count counters for this consumer across routes (current window). */
    private int currentUsage() {
        var keyCmds = redis.key();
        var valCmds = redis.value(String.class);
        int total = 0;
        for (String k : keyCmds.keys("*" + consumer)) {
            String v = valCmds.get(k);
            if (v != null) {
                try {
                    total += Integer.parseInt(v.trim());
                } catch (NumberFormatException ignored) {
                    // limit-count may store a struct on some versions; skip non-numeric
                }
            }
        }
        return total;
    }
}

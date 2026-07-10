package com.balabank.openbanking.balabank.events;

import com.balabank.openbanking.balabank.domain.CallbackUrlEntity;
import com.balabank.openbanking.balabank.domain.EventSubscriptionEntity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event Notification API, v3.1 subset (task 6.5): event-subscriptions, callback-urls,
 * and the aggregated polling {@code /events} endpoint. Subscriptions/callback-urls are
 * entity-backed (representative persistence); the polling endpoint returns a possibly-empty
 * OBIE Security Event Token set (no real event producer is wired up in this reference app).
 */
@Path("/open-banking/v3.1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("payments")
public class EventNotificationResource {

    public record EventSubscriptionRequest(String callbackUrl, List<String> eventTypes, String version) {}

    public record CallbackUrlRequest(String url, String version) {}

    private static final String DEFAULT_CLIENT_ID = "default-client";

    // ---- event-subscriptions ----

    @POST
    @Path("/event-subscriptions")
    @Transactional
    public Map<String, Object> createEventSubscription(EventSubscriptionRequest req) {
        EventSubscriptionEntity s = new EventSubscriptionEntity();
        s.subscriptionId = "evtsub-" + UUID.randomUUID();
        s.clientId = DEFAULT_CLIENT_ID;
        s.callbackUrl = req.callbackUrl();
        s.eventTypes = req.eventTypes() == null ? "" : String.join(",", req.eventTypes());
        s.version = req.version() == null ? "3.1" : req.version();
        s.persist();
        return Map.of("Data", toSubscriptionData(s));
    }

    @GET
    @Path("/event-subscriptions")
    public Map<String, Object> listEventSubscriptions() {
        List<Map<String, Object>> items = EventSubscriptionEntity.<EventSubscriptionEntity>listAll().stream()
                .map(this::toSubscriptionData)
                .toList();
        return Map.of("Data", Map.of("EventSubscription", items));
    }

    @PUT
    @Path("/event-subscriptions/{subscriptionId}")
    @Transactional
    public Map<String, Object> updateEventSubscription(
            @PathParam("subscriptionId") String subscriptionId, EventSubscriptionRequest req) {
        EventSubscriptionEntity s = EventSubscriptionEntity.findById(subscriptionId);
        if (s == null) {
            throw new NotFoundException("Unknown event subscription");
        }
        if (req.callbackUrl() != null) {
            s.callbackUrl = req.callbackUrl();
        }
        if (req.eventTypes() != null) {
            s.eventTypes = String.join(",", req.eventTypes());
        }
        return Map.of("Data", toSubscriptionData(s));
    }

    @DELETE
    @Path("/event-subscriptions/{subscriptionId}")
    @Transactional
    public void deleteEventSubscription(@PathParam("subscriptionId") String subscriptionId) {
        EventSubscriptionEntity s = EventSubscriptionEntity.findById(subscriptionId);
        if (s == null) {
            throw new NotFoundException("Unknown event subscription");
        }
        s.delete();
    }

    private Map<String, Object> toSubscriptionData(EventSubscriptionEntity s) {
        return Map.of(
                "EventSubscriptionId", s.subscriptionId,
                "CallbackUrl", s.callbackUrl,
                "Version", s.version,
                "EventTypes", s.eventTypes == null || s.eventTypes.isBlank()
                        ? List.of() : List.of(s.eventTypes.split(",")));
    }

    // ---- callback-urls ----

    @POST
    @Path("/callback-urls")
    @Transactional
    public Map<String, Object> createCallbackUrl(CallbackUrlRequest req) {
        CallbackUrlEntity c = new CallbackUrlEntity();
        c.callbackUrlId = "cburl-" + UUID.randomUUID();
        c.clientId = DEFAULT_CLIENT_ID;
        c.url = req.url();
        c.version = req.version() == null ? "3.1" : req.version();
        c.persist();
        return Map.of("Data", toCallbackData(c));
    }

    @GET
    @Path("/callback-urls")
    public Map<String, Object> listCallbackUrls() {
        List<Map<String, Object>> items = CallbackUrlEntity.<CallbackUrlEntity>listAll().stream()
                .map(this::toCallbackData)
                .toList();
        return Map.of("Data", Map.of("CallbackUrl", items));
    }

    @PUT
    @Path("/callback-urls/{callbackUrlId}")
    @Transactional
    public Map<String, Object> updateCallbackUrl(
            @PathParam("callbackUrlId") String callbackUrlId, CallbackUrlRequest req) {
        CallbackUrlEntity c = CallbackUrlEntity.findById(callbackUrlId);
        if (c == null) {
            throw new NotFoundException("Unknown callback url");
        }
        if (req.url() != null) {
            c.url = req.url();
        }
        return Map.of("Data", toCallbackData(c));
    }

    @DELETE
    @Path("/callback-urls/{callbackUrlId}")
    @Transactional
    public void deleteCallbackUrl(@PathParam("callbackUrlId") String callbackUrlId) {
        CallbackUrlEntity c = CallbackUrlEntity.findById(callbackUrlId);
        if (c == null) {
            throw new NotFoundException("Unknown callback url");
        }
        c.delete();
    }

    private Map<String, Object> toCallbackData(CallbackUrlEntity c) {
        return Map.of("CallbackUrlId", c.callbackUrlId, "Url", c.url, "Version", c.version);
    }

    // ---- aggregated polling ----

    @POST
    @Path("/events")
    public Map<String, Object> pollEvents() {
        // Representative: no event producer wired up yet, so this reference implementation
        // always returns an empty aggregated polling response (a valid OBIE outcome).
        return Map.of(
                "sets", Map.of(),
                "moreAvailable", false,
                "polledAt", String.valueOf(OffsetDateTime.now()));
    }
}

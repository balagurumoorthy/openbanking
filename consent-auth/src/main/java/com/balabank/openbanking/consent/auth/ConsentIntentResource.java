package com.balabank.openbanking.consent.auth;

import com.balabank.openbanking.common.ConsentStatus;
import com.balabank.openbanking.consent.domain.Consent;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Account-access consent intent. The TPP calls this to register the permissions it wants
 * before redirecting the user to {@code /authorize}. Returns the {@code consentId}.
 */
@Path("/account-access-consents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConsentIntentResource {

    public record IntentRequest(String clientId, List<String> permissions) {}

    @POST
    @Transactional
    public Map<String, Object> create(IntentRequest req) {
        Consent c = new Consent();
        c.consentId = "acon-" + UUID.randomUUID();
        c.clientId = req.clientId();
        c.requestedPermissions = String.join(" ", req.permissions());
        c.status = ConsentStatus.AWAITING_AUTHORISATION;
        c.persist();
        return Map.of("Data", Map.of("ConsentId", c.consentId, "Status", c.status.name()));
    }

    /**
     * Revokes an account-access consent (TPP or PSU initiated). Per OBIE, revoking a consent
     * that is already terminal (revoked/rejected/expired/consumed) is idempotent and returns
     * its current state rather than erroring.
     */
    @DELETE
    @Path("/{consentId}")
    @Transactional
    public Response revoke(@PathParam("consentId") String consentId) {
        Consent c = Consent.findById(consentId);
        if (c == null) {
            throw new NotFoundException("Unknown consent_id");
        }
        if (c.status != ConsentStatus.REVOKED
                && c.status != ConsentStatus.REJECTED
                && c.status != ConsentStatus.EXPIRED) {
            c.status = ConsentStatus.REVOKED;
        }
        return Response.ok(Map.of("Data", Map.of("ConsentId", c.consentId, "Status", c.status.name()))).build();
    }
}

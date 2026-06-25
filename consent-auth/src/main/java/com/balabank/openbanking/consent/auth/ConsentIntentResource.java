package com.balabank.openbanking.consent.auth;

import com.balabank.openbanking.common.ConsentStatus;
import com.balabank.openbanking.consent.domain.Consent;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
}

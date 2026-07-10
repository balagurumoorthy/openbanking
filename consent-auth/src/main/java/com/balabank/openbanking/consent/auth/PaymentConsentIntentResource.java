package com.balabank.openbanking.consent.auth;

import com.balabank.openbanking.common.ConsentStatus;
import com.balabank.openbanking.consent.domain.Consent;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.UUID;

/**
 * Payment (PISP) consent intent. A TPP calls this before redirecting the user to
 * {@code /authorize} to authorise a domestic payment. The consent carries no account-info
 * permissions, so at approval {@code grantedPermissions} stays blank and
 * {@link TokenService} mints a token with the {@code payments} scope (the group/role the
 * ASPSP's PISP endpoints require). Payment instruction details (creditor/amount) live in the
 * ASPSP's own payment-consent store — this record just drives the user authorisation + token.
 */
@Path("/domestic-payment-consents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PaymentConsentIntentResource {

    public record PaymentIntentRequest(String clientId) {}

    @POST
    @Transactional
    public Map<String, Object> create(PaymentIntentRequest req) {
        Consent c = new Consent();
        c.consentId = "pcon-" + UUID.randomUUID();
        c.clientId = req.clientId();
        c.requestedPermissions = "";   // blank => payments scope at token issuance
        c.status = ConsentStatus.AWAITING_AUTHORISATION;
        c.persist();
        return Map.of("Data", Map.of("ConsentId", c.consentId, "Status", c.status.name()));
    }
}

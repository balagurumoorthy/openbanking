package com.balabank.openbanking.balabank.cbpii;

import com.balabank.openbanking.balabank.domain.AccountEntity;
import com.balabank.openbanking.balabank.domain.BalanceEntity;
import com.balabank.openbanking.balabank.domain.FundsConfirmationConsentEntity;
import com.balabank.openbanking.common.ConsentStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Confirmation of Funds (CBPII) API, v3.1 subset (task 6.4).
 *
 * <p>NOTE (scope prerequisite): these endpoints are {@code @RolesAllowed("fundsconfirmations")}
 * per {@link com.balabank.openbanking.common.Permission.Scope#FUNDSCONFIRMATIONS}, but consent-auth
 * may not yet issue tokens carrying that scope/role - the endpoints, entity, and OBIE bodies are
 * implemented here regardless so wiring can be completed independently once the scope is issued.
 */
@Path("/open-banking/v3.1/cbpii")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("fundsconfirmations")
public class FundsConfirmationResource {

    public record FundsConfirmationConsentRequest(String debtorAccountId, OffsetDateTime expirationDateTime) {}

    public record FundsConfirmationRequest(String consentId, BigDecimal amount, String currency) {}

    @POST
    @Path("/funds-confirmation-consents")
    @Transactional
    public Map<String, Object> createConsent(FundsConfirmationConsentRequest req) {
        FundsConfirmationConsentEntity c = new FundsConfirmationConsentEntity();
        c.consentId = "fccon-" + UUID.randomUUID();
        c.debtorAccountId = req.debtorAccountId();
        c.expirationDateTime = req.expirationDateTime();
        c.status = ConsentStatus.AWAITING_AUTHORISATION;
        c.persist();
        return Map.of("Data", Map.of(
                "ConsentId", c.consentId,
                "Status", c.status.name(),
                "ExpirationDateTime", String.valueOf(c.expirationDateTime)));
    }

    @GET
    @Path("/funds-confirmation-consents/{consentId}")
    public Map<String, Object> getConsent(@PathParam("consentId") String consentId) {
        FundsConfirmationConsentEntity c = FundsConfirmationConsentEntity.findById(consentId);
        if (c == null) {
            throw new NotFoundException("Unknown funds confirmation consent");
        }
        return Map.of("Data", Map.of("ConsentId", c.consentId, "Status", c.status.name()));
    }

    @DELETE
    @Path("/funds-confirmation-consents/{consentId}")
    @Transactional
    public void deleteConsent(@PathParam("consentId") String consentId) {
        FundsConfirmationConsentEntity c = FundsConfirmationConsentEntity.findById(consentId);
        if (c == null) {
            throw new NotFoundException("Unknown funds confirmation consent");
        }
        c.status = ConsentStatus.REVOKED;
    }

    @POST
    @Path("/funds-confirmations")
    @Transactional
    public Map<String, Object> confirmFunds(FundsConfirmationRequest req) {
        FundsConfirmationConsentEntity c = FundsConfirmationConsentEntity.findById(req.consentId());
        if (c == null) {
            throw new BadRequestException("Unknown funds confirmation consent");
        }
        if (c.status != ConsentStatus.AUTHORISED) {
            throw new ForbiddenException("Funds confirmation consent is not authorised");
        }
        // Representative check against the debtor account's interim available balance.
        boolean fundsAvailable = false;
        AccountEntity account = AccountEntity.findById(c.debtorAccountId);
        if (account != null) {
            BalanceEntity balance = BalanceEntity.find("accountId", c.debtorAccountId).firstResult();
            if (balance != null && req.amount() != null) {
                fundsAvailable = balance.amount.compareTo(req.amount()) >= 0;
            }
        }
        return Map.of("Data", Map.of(
                "FundsConfirmationId", "fcr-" + UUID.randomUUID(),
                "ConsentId", c.consentId,
                "FundsAvailableResult", Map.of(
                        "FundsAvailableDateTime", String.valueOf(OffsetDateTime.now()),
                        "FundsAvailable", fundsAvailable)));
    }
}

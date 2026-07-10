package com.balabank.openbanking.balabank.aisp;

import com.balabank.openbanking.balabank.aisp.dto.AccountAccessConsentDto;
import com.balabank.openbanking.balabank.domain.AccountAccessConsentEntity;
import com.balabank.openbanking.balabank.security.ConsentContext;
import com.balabank.openbanking.common.dto.ObResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;

/**
 * OBIE account-access-consent management (ASPSP side): retrieve or revoke a consent
 * previously granted for account information access.
 */
@Path("/open-banking/v3.1/aisp/account-access-consents")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("accounts")
public class AccountAccessConsentResource {

    @Inject
    ConsentContext consent;

    @GET
    @Path("/{consentId}")
    public ObResponse<Map<String, Object>> getConsent(@PathParam("consentId") String consentId) {
        AccountAccessConsentEntity c = ensureOwned(consentId);
        return ObResponse.ofResource("Consent", toDto(c),
                "/open-banking/v3.1/aisp/account-access-consents/" + consentId);
    }

    @DELETE
    @Path("/{consentId}")
    @Transactional
    public Response revokeConsent(@PathParam("consentId") String consentId) {
        AccountAccessConsentEntity c = ensureOwned(consentId);
        c.status = "Revoked";
        c.statusUpdateDateTime = OffsetDateTime.now();
        return Response.noContent().build();
    }

    private AccountAccessConsentEntity ensureOwned(String consentId) {
        AccountAccessConsentEntity c = AccountAccessConsentEntity.findByConsentId(consentId);
        if (c == null || !c.customerId.equals(consent.customerId())) {
            throw new NotFoundException("Unknown account access consent: " + consentId);
        }
        return c;
    }

    private AccountAccessConsentDto toDto(AccountAccessConsentEntity c) {
        return new AccountAccessConsentDto(c.consentId, c.creationDateTime, c.status, c.statusUpdateDateTime,
                Arrays.asList(c.permissions.split(",")), c.expirationDateTime,
                c.transactionFromDateTime, c.transactionToDateTime);
    }
}

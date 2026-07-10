package com.balabank.openbanking.balabank.pisp;

import com.balabank.openbanking.balabank.domain.InternationalPaymentConsentEntity;
import com.balabank.openbanking.balabank.domain.InternationalPaymentEntity;
import com.balabank.openbanking.common.ConsentStatus;
import com.balabank.openbanking.common.PaymentStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * OBIE Payment Initiation (PISP) international-payment endpoints, v3.1 subset (task 6.3).
 * New resource class - does not modify {@code PaymentInitiationResource}.
 */
@Path("/open-banking/v3.1/pisp")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("payments")
public class InternationalPaymentResource {

    public record InternationalPaymentConsentRequest(
            String debtorAccountId, String creditorAccountIdentification, String creditorAccountName,
            String creditorAgentBic, BigDecimal instructedAmount, String instructedCurrency, String reference) {}

    public record InternationalPaymentRequest(String consentId) {}

    @POST
    @Path("/international-payment-consents")
    @Transactional
    public Map<String, Object> createConsent(InternationalPaymentConsentRequest req) {
        InternationalPaymentConsentEntity c = new InternationalPaymentConsentEntity();
        c.consentId = "ipcon-" + UUID.randomUUID();
        c.debtorAccountId = req.debtorAccountId();
        c.creditorAccountIdentification = req.creditorAccountIdentification();
        c.creditorAccountName = req.creditorAccountName();
        c.creditorAgentBic = req.creditorAgentBic();
        c.instructedAmount = req.instructedAmount();
        c.instructedCurrency = req.instructedCurrency();
        c.reference = req.reference();
        c.status = ConsentStatus.AWAITING_AUTHORISATION;
        c.persist();
        return Map.of("Data", Map.of("ConsentId", c.consentId, "Status", c.status.name()));
    }

    @GET
    @Path("/international-payment-consents/{consentId}")
    public Map<String, Object> getConsent(@PathParam("consentId") String consentId) {
        InternationalPaymentConsentEntity c = InternationalPaymentConsentEntity.findById(consentId);
        if (c == null) {
            throw new NotFoundException("Unknown international payment consent");
        }
        return Map.of("Data", Map.of("ConsentId", c.consentId, "Status", c.status.name()));
    }

    @POST
    @Path("/international-payments")
    @Transactional
    public Map<String, Object> executePayment(InternationalPaymentRequest req) {
        InternationalPaymentConsentEntity c = InternationalPaymentConsentEntity.findById(req.consentId());
        if (c == null) {
            throw new BadRequestException("Unknown international payment consent");
        }
        if (c.status != ConsentStatus.AUTHORISED) {
            throw new ForbiddenException("International payment consent is not authorised");
        }
        InternationalPaymentEntity p = new InternationalPaymentEntity();
        p.internationalPaymentId = "ipmt-" + UUID.randomUUID();
        p.consentId = c.consentId;
        p.status = PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS;
        p.persist();
        c.status = ConsentStatus.CONSUMED;
        return Map.of("Data", Map.of(
                "InternationalPaymentId", p.internationalPaymentId,
                "ConsentId", c.consentId,
                "Status", p.status.obieValue()));
    }

    @GET
    @Path("/international-payments/{internationalPaymentId}")
    public Map<String, Object> getPayment(@PathParam("internationalPaymentId") String internationalPaymentId) {
        InternationalPaymentEntity p = InternationalPaymentEntity.findById(internationalPaymentId);
        if (p == null) {
            throw new NotFoundException("Unknown international payment");
        }
        return Map.of("Data", Map.of(
                "InternationalPaymentId", p.internationalPaymentId,
                "ConsentId", p.consentId,
                "Status", p.status.obieValue()));
    }
}

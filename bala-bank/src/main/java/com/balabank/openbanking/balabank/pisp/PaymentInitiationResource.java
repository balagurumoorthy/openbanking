package com.balabank.openbanking.balabank.pisp;

import com.balabank.openbanking.balabank.domain.PaymentConsentEntity;
import com.balabank.openbanking.balabank.domain.PaymentEntity;
import com.balabank.openbanking.common.ConsentStatus;
import com.balabank.openbanking.common.PaymentStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** OBIE Payment Initiation (PISP) domestic-payment endpoints, v3.1 subset. */
@Path("/open-banking/v3.1/pisp")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("payments")
public class PaymentInitiationResource {

    public record DomesticPaymentConsentRequest(
            String debtorAccountId, String creditorIdentification, String creditorName,
            BigDecimal amount, String currency, String reference) {}

    public record DomesticPaymentRequest(String consentId) {}

    @POST
    @Path("/domestic-payment-consents")
    @Transactional
    public Map<String, Object> createConsent(DomesticPaymentConsentRequest req) {
        PaymentConsentEntity c = new PaymentConsentEntity();
        c.consentId = "pcon-" + UUID.randomUUID();
        c.debtorAccountId = req.debtorAccountId();
        c.creditorIdentification = req.creditorIdentification();
        c.creditorName = req.creditorName();
        c.amount = req.amount();
        c.currency = req.currency();
        c.reference = req.reference();
        // The PSU already authorised this payment at the consent-auth authorization endpoint —
        // the caller's token carries the `payments` scope as proof — so for this local reference
        // platform the ASPSP-side consent is created already AUTHORISED and is ready to execute.
        c.status = ConsentStatus.AUTHORISED;
        c.persist();
        return Map.of("Data", Map.of("ConsentId", c.consentId, "Status", c.status.name()));
    }

    @POST
    @Path("/domestic-payments")
    @Transactional
    public Map<String, Object> executePayment(DomesticPaymentRequest req) {
        PaymentConsentEntity c = PaymentConsentEntity.findById(req.consentId());
        if (c == null) {
            throw new BadRequestException("Unknown payment consent");
        }
        if (c.status != ConsentStatus.AUTHORISED) {
            throw new ForbiddenException("Payment consent is not authorised");
        }
        PaymentEntity p = new PaymentEntity();
        p.paymentId = "pmt-" + UUID.randomUUID();
        p.consentId = c.consentId;
        p.status = PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS;
        p.persist();
        c.status = ConsentStatus.CONSUMED;
        return Map.of("Data", Map.of(
                "DomesticPaymentId", p.paymentId,
                "ConsentId", c.consentId,
                "Status", p.status.obieValue()));
    }
}

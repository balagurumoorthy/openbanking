package com.balabank.openbanking.balabank.pisp;

import com.balabank.openbanking.balabank.domain.PaymentConsentEntity;
import com.balabank.openbanking.balabank.domain.PaymentEntity;
import com.balabank.openbanking.common.ConsentStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * OBIE PISP supplementary endpoints for the existing domestic-payment resources (task 6.3):
 * payment-details and the payment-consent funds-confirmation check. New resource class -
 * does not modify {@code PaymentInitiationResource}.
 */
@Path("/open-banking/v3.1/pisp")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("payments")
public class PaymentSupplementaryResource {

    @GET
    @Path("/domestic-payments/{paymentId}/payment-details")
    public Map<String, Object> getPaymentDetails(@PathParam("paymentId") String paymentId) {
        PaymentEntity p = PaymentEntity.findById(paymentId);
        if (p == null) {
            throw new NotFoundException("Unknown domestic payment");
        }
        // Representative: single LocalDetails entry reflecting current payment status.
        Map<String, Object> detail = Map.of(
                "Status", p.status.obieValue(),
                "StatusUpdateDateTime", String.valueOf(java.time.OffsetDateTime.now()),
                "PaymentTransactionId", p.paymentId);
        return Map.of("Data", Map.of("PaymentStatus", List.of(detail)));
    }

    @GET
    @Path("/domestic-payment-consents/{consentId}/funds-confirmation")
    public Map<String, Object> getConsentFundsConfirmation(@PathParam("consentId") String consentId) {
        PaymentConsentEntity c = PaymentConsentEntity.findById(consentId);
        if (c == null) {
            throw new NotFoundException("Unknown payment consent");
        }
        // Representative: consents that are authorised are treated as having sufficient funds.
        boolean fundsAvailable = c.status == ConsentStatus.AUTHORISED || c.status == ConsentStatus.CONSUMED;
        return Map.of("Data", Map.of("FundsAvailable", fundsAvailable));
    }
}

package com.balabank.openbanking.balabank.pisp;

import com.balabank.openbanking.balabank.domain.ScheduledPaymentConsentEntity;
import com.balabank.openbanking.balabank.domain.DomesticScheduledPaymentEntity;
import com.balabank.openbanking.common.ConsentStatus;
import com.balabank.openbanking.common.PaymentStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * OBIE Payment Initiation (PISP) domestic-scheduled-payment endpoints, v3.1 subset (task 6.3).
 * New resource class - does not modify {@code PaymentInitiationResource}.
 */
@Path("/open-banking/v3.1/pisp")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("payments")
public class ScheduledPaymentResource {

    public record DomesticScheduledPaymentConsentRequest(
            String debtorAccountId, String creditorIdentification, String creditorName,
            BigDecimal amount, String currency, String reference, OffsetDateTime requestedExecutionDateTime) {}

    public record DomesticScheduledPaymentRequest(String consentId) {}

    @POST
    @Path("/domestic-scheduled-payment-consents")
    @Transactional
    public Map<String, Object> createConsent(DomesticScheduledPaymentConsentRequest req) {
        ScheduledPaymentConsentEntity c = new ScheduledPaymentConsentEntity();
        c.consentId = "spcon-" + UUID.randomUUID();
        c.debtorAccountId = req.debtorAccountId();
        c.creditorIdentification = req.creditorIdentification();
        c.creditorName = req.creditorName();
        c.amount = req.amount();
        c.currency = req.currency();
        c.reference = req.reference();
        c.requestedExecutionDateTime = req.requestedExecutionDateTime();
        c.status = ConsentStatus.AWAITING_AUTHORISATION;
        c.persist();
        return Map.of("Data", Map.of(
                "ConsentId", c.consentId,
                "Status", c.status.name(),
                "RequestedExecutionDateTime", String.valueOf(c.requestedExecutionDateTime)));
    }

    @GET
    @Path("/domestic-scheduled-payment-consents/{consentId}")
    public Map<String, Object> getConsent(@PathParam("consentId") String consentId) {
        ScheduledPaymentConsentEntity c = ScheduledPaymentConsentEntity.findById(consentId);
        if (c == null) {
            throw new NotFoundException("Unknown scheduled payment consent");
        }
        return Map.of("Data", Map.of("ConsentId", c.consentId, "Status", c.status.name()));
    }

    @POST
    @Path("/domestic-scheduled-payments")
    @Transactional
    public Map<String, Object> executePayment(DomesticScheduledPaymentRequest req) {
        ScheduledPaymentConsentEntity c = ScheduledPaymentConsentEntity.findById(req.consentId());
        if (c == null) {
            throw new BadRequestException("Unknown scheduled payment consent");
        }
        if (c.status != ConsentStatus.AUTHORISED) {
            throw new ForbiddenException("Scheduled payment consent is not authorised");
        }
        DomesticScheduledPaymentEntity p = new DomesticScheduledPaymentEntity();
        p.scheduledPaymentId = "spmt-" + UUID.randomUUID();
        p.consentId = c.consentId;
        p.status = PaymentStatus.PENDING;
        p.persist();
        c.status = ConsentStatus.CONSUMED;
        return Map.of("Data", Map.of(
                "DomesticScheduledPaymentId", p.scheduledPaymentId,
                "ConsentId", c.consentId,
                "Status", p.status.obieValue()));
    }

    @GET
    @Path("/domestic-scheduled-payments/{scheduledPaymentId}")
    public Map<String, Object> getPayment(@PathParam("scheduledPaymentId") String scheduledPaymentId) {
        DomesticScheduledPaymentEntity p = DomesticScheduledPaymentEntity.findById(scheduledPaymentId);
        if (p == null) {
            throw new NotFoundException("Unknown scheduled payment");
        }
        return Map.of("Data", Map.of(
                "DomesticScheduledPaymentId", p.scheduledPaymentId,
                "ConsentId", p.consentId,
                "Status", p.status.obieValue()));
    }
}

package com.balabank.openbanking.balabank.pisp;

import com.balabank.openbanking.balabank.domain.StandingOrderConsentEntity;
import com.balabank.openbanking.balabank.domain.DomesticStandingOrderEntity;
import com.balabank.openbanking.common.ConsentStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * OBIE Payment Initiation (PISP) domestic-standing-order endpoints, v3.1 subset (task 6.3).
 * New resource class - does not modify {@code PaymentInitiationResource}.
 */
@Path("/open-banking/v3.1/pisp")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("payments")
public class StandingOrderResource {

    public record DomesticStandingOrderConsentRequest(
            String debtorAccountId, String creditorIdentification, String creditorName,
            BigDecimal firstPaymentAmount, String currency, String reference,
            String frequency, Integer numberOfPayments) {}

    public record DomesticStandingOrderRequest(String consentId) {}

    @POST
    @Path("/domestic-standing-order-consents")
    @Transactional
    public Map<String, Object> createConsent(DomesticStandingOrderConsentRequest req) {
        StandingOrderConsentEntity c = new StandingOrderConsentEntity();
        c.consentId = "socon-" + UUID.randomUUID();
        c.debtorAccountId = req.debtorAccountId();
        c.creditorIdentification = req.creditorIdentification();
        c.creditorName = req.creditorName();
        c.firstPaymentAmount = req.firstPaymentAmount();
        c.currency = req.currency();
        c.reference = req.reference();
        c.frequency = req.frequency();
        c.numberOfPayments = req.numberOfPayments();
        c.status = ConsentStatus.AWAITING_AUTHORISATION;
        c.persist();
        return Map.of("Data", Map.of(
                "ConsentId", c.consentId,
                "Status", c.status.name(),
                "Frequency", String.valueOf(c.frequency)));
    }

    @GET
    @Path("/domestic-standing-order-consents/{consentId}")
    public Map<String, Object> getConsent(@PathParam("consentId") String consentId) {
        StandingOrderConsentEntity c = StandingOrderConsentEntity.findById(consentId);
        if (c == null) {
            throw new NotFoundException("Unknown standing order consent");
        }
        return Map.of("Data", Map.of("ConsentId", c.consentId, "Status", c.status.name()));
    }

    @POST
    @Path("/domestic-standing-orders")
    @Transactional
    public Map<String, Object> createStandingOrder(DomesticStandingOrderRequest req) {
        StandingOrderConsentEntity c = StandingOrderConsentEntity.findById(req.consentId());
        if (c == null) {
            throw new BadRequestException("Unknown standing order consent");
        }
        if (c.status != ConsentStatus.AUTHORISED) {
            throw new ForbiddenException("Standing order consent is not authorised");
        }
        DomesticStandingOrderEntity so = new DomesticStandingOrderEntity();
        so.standingOrderId = "so-" + UUID.randomUUID();
        so.consentId = c.consentId;
        so.status = "Active";
        so.persist();
        c.status = ConsentStatus.CONSUMED;
        return Map.of("Data", Map.of(
                "DomesticStandingOrderId", so.standingOrderId,
                "ConsentId", c.consentId,
                "Status", so.status));
    }

    @GET
    @Path("/domestic-standing-orders/{standingOrderId}")
    public Map<String, Object> getStandingOrder(@PathParam("standingOrderId") String standingOrderId) {
        DomesticStandingOrderEntity so = DomesticStandingOrderEntity.findById(standingOrderId);
        if (so == null) {
            throw new NotFoundException("Unknown standing order");
        }
        return Map.of("Data", Map.of(
                "DomesticStandingOrderId", so.standingOrderId,
                "ConsentId", so.consentId,
                "Status", so.status));
    }
}

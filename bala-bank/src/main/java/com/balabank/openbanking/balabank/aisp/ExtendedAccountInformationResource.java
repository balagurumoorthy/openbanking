package com.balabank.openbanking.balabank.aisp;

import com.balabank.openbanking.balabank.aisp.dto.*;
import com.balabank.openbanking.balabank.domain.*;
import com.balabank.openbanking.balabank.security.ConsentContext;
import com.balabank.openbanking.common.Permission;
import com.balabank.openbanking.common.dto.Money;
import com.balabank.openbanking.common.dto.ObResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * Remaining OBIE Account Information (AISP) read endpoints: beneficiaries, direct debits,
 * standing orders, scheduled payments, statements, party, products, offers.
 */
@Path("/open-banking/v3.1/aisp")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("accounts")
public class ExtendedAccountInformationResource {

    @Inject
    ConsentContext consent;

    // --- Beneficiaries ---

    @GET
    @Path("/accounts/{accountId}/beneficiaries")
    public ObResponse<Map<String, Object>> accountBeneficiaries(@PathParam("accountId") String accountId) {
        consent.require(Permission.READ_BENEFICIARIES_DETAIL);
        consent.requireAccount(accountId);
        ensureOwned(accountId);
        List<BeneficiaryDto> items = BeneficiaryEntity.<BeneficiaryEntity>list("accountId", accountId).stream()
                .map(this::toDto).toList();
        return ObResponse.ofResource("Beneficiary", items,
                "/open-banking/v3.1/aisp/accounts/" + accountId + "/beneficiaries");
    }

    @GET
    @Path("/beneficiaries")
    public ObResponse<Map<String, Object>> beneficiaries() {
        consent.require(Permission.READ_BENEFICIARIES_DETAIL);
        List<BeneficiaryDto> items = BeneficiaryEntity.<BeneficiaryEntity>listAll().stream()
                .filter(b -> consent.consentedAccounts().contains(b.accountId))
                .map(this::toDto).toList();
        return ObResponse.ofResource("Beneficiary", items, "/open-banking/v3.1/aisp/beneficiaries");
    }

    private BeneficiaryDto toDto(BeneficiaryEntity b) {
        return new BeneficiaryDto(b.beneficiaryId, b.accountId, b.reference,
                new BeneficiaryDto.CreditorAccount("UK.OBIE.SortCodeAccountNumber",
                        b.accountIdentification, b.accountName, null));
    }

    // --- Direct debits ---

    @GET
    @Path("/accounts/{accountId}/direct-debits")
    public ObResponse<Map<String, Object>> directDebits(@PathParam("accountId") String accountId) {
        consent.require(Permission.READ_DIRECT_DEBITS);
        consent.requireAccount(accountId);
        ensureOwned(accountId);
        List<DirectDebitDto> items = DirectDebitEntity.<DirectDebitEntity>list("accountId", accountId).stream()
                .map(d -> new DirectDebitDto(d.accountId, d.directDebitId, d.frequency, d.status,
                        d.previousPaymentDate, new Money(d.previousPaymentAmount, d.previousPaymentCurrency), d.name))
                .toList();
        return ObResponse.ofResource("DirectDebit", items,
                "/open-banking/v3.1/aisp/accounts/" + accountId + "/direct-debits");
    }

    // --- Standing orders ---

    @GET
    @Path("/accounts/{accountId}/standing-orders")
    public ObResponse<Map<String, Object>> standingOrders(@PathParam("accountId") String accountId) {
        consent.require(Permission.READ_STANDING_ORDERS_DETAIL);
        consent.requireAccount(accountId);
        ensureOwned(accountId);
        List<StandingOrderDto> items = StandingOrderEntity.<StandingOrderEntity>list("accountId", accountId).stream()
                .map(s -> new StandingOrderDto(s.accountId, s.standingOrderId, s.frequency, s.status,
                        s.nextPaymentDate, new Money(s.nextPaymentAmount, s.nextPaymentCurrency),
                        s.finalPaymentAmount == null ? null : new Money(s.finalPaymentAmount, s.finalPaymentCurrency),
                        new StandingOrderDto.CreditorAccount("UK.OBIE.SortCodeAccountNumber",
                                s.creditorIdentification, s.creditorName)))
                .toList();
        return ObResponse.ofResource("StandingOrder", items,
                "/open-banking/v3.1/aisp/accounts/" + accountId + "/standing-orders");
    }

    // --- Scheduled payments ---

    @GET
    @Path("/accounts/{accountId}/scheduled-payments")
    public ObResponse<Map<String, Object>> scheduledPayments(@PathParam("accountId") String accountId) {
        consent.require(Permission.READ_SCHEDULED_PAYMENTS_DETAIL);
        consent.requireAccount(accountId);
        ensureOwned(accountId);
        List<ScheduledPaymentDto> items = ScheduledPaymentEntity.<ScheduledPaymentEntity>list("accountId", accountId).stream()
                .map(s -> new ScheduledPaymentDto(s.accountId, s.scheduledPaymentId, s.scheduledPaymentDate,
                        s.scheduledType, new Money(s.amount, s.currency),
                        new ScheduledPaymentDto.CreditorAccount("UK.OBIE.SortCodeAccountNumber",
                                s.creditorIdentification, s.creditorName), s.reference))
                .toList();
        return ObResponse.ofResource("ScheduledPayment", items,
                "/open-banking/v3.1/aisp/accounts/" + accountId + "/scheduled-payments");
    }

    // --- Statements ---

    @GET
    @Path("/accounts/{accountId}/statements")
    public ObResponse<Map<String, Object>> statements(@PathParam("accountId") String accountId) {
        consent.require(Permission.READ_STATEMENTS_DETAIL);
        consent.requireAccount(accountId);
        ensureOwned(accountId);
        List<StatementDto> items = StatementEntity.<StatementEntity>list("accountId", accountId).stream()
                .map(s -> new StatementDto(s.accountId, s.statementId, s.statementType,
                        s.startDateTime, s.endDateTime, s.creationDateTime))
                .toList();
        return ObResponse.ofResource("Statement", items,
                "/open-banking/v3.1/aisp/accounts/" + accountId + "/statements");
    }

    // --- Party ---

    @GET
    @Path("/accounts/{accountId}/party")
    public ObResponse<Map<String, Object>> accountParty(@PathParam("accountId") String accountId) {
        consent.require(Permission.READ_PARTY);
        consent.requireAccount(accountId);
        ensureOwned(accountId);
        List<PartyDto> items = PartyEntity.<PartyEntity>list("accountId", accountId).stream()
                .map(this::toDto).toList();
        return ObResponse.ofResource("Party", items,
                "/open-banking/v3.1/aisp/accounts/" + accountId + "/party");
    }

    @GET
    @Path("/party")
    public ObResponse<Map<String, Object>> party() {
        consent.require(Permission.READ_PARTY_PSU);
        List<PartyDto> items = PartyEntity.<PartyEntity>list("customerId", consent.customerId()).stream()
                .map(this::toDto).toList();
        return ObResponse.ofResource("Party", items, "/open-banking/v3.1/aisp/party");
    }

    private PartyDto toDto(PartyEntity p) {
        return new PartyDto(p.partyId, p.partyNumber, p.fullLegalName, p.partyType, p.emailAddress, p.phoneNumber);
    }

    // --- Products ---

    @GET
    @Path("/products")
    public ObResponse<Map<String, Object>> products() {
        consent.require(Permission.READ_PRODUCTS);
        List<ProductDto> items = ProductEntity.<ProductEntity>find("accountId is null").stream()
                .map(this::toDto).toList();
        return ObResponse.ofResource("Product", items, "/open-banking/v3.1/aisp/products");
    }

    @GET
    @Path("/accounts/{accountId}/product")
    public ObResponse<Map<String, Object>> accountProduct(@PathParam("accountId") String accountId) {
        consent.require(Permission.READ_PRODUCTS);
        consent.requireAccount(accountId);
        ensureOwned(accountId);
        List<ProductDto> items = ProductEntity.<ProductEntity>list("accountId", accountId).stream()
                .map(this::toDto).toList();
        return ObResponse.ofResource("Product", items,
                "/open-banking/v3.1/aisp/accounts/" + accountId + "/product");
    }

    private ProductDto toDto(ProductEntity p) {
        return new ProductDto(p.productId, p.productName, p.productType, p.marketingState);
    }

    // --- Offers ---

    @GET
    @Path("/offers")
    public ObResponse<Map<String, Object>> offers() {
        consent.require(Permission.READ_OFFERS);
        List<OfferDto> items = OfferEntity.<OfferEntity>listAll().stream()
                .filter(o -> consent.consentedAccounts().contains(o.accountId))
                .map(this::toDto).toList();
        return ObResponse.ofResource("Offer", items, "/open-banking/v3.1/aisp/offers");
    }

    @GET
    @Path("/accounts/{accountId}/offers")
    public ObResponse<Map<String, Object>> accountOffers(@PathParam("accountId") String accountId) {
        consent.require(Permission.READ_OFFERS);
        consent.requireAccount(accountId);
        ensureOwned(accountId);
        List<OfferDto> items = OfferEntity.<OfferEntity>list("accountId", accountId).stream()
                .map(this::toDto).toList();
        return ObResponse.ofResource("Offer", items,
                "/open-banking/v3.1/aisp/accounts/" + accountId + "/offers");
    }

    private OfferDto toDto(OfferEntity o) {
        return new OfferDto(o.accountId, o.offerId, o.offerType, o.description,
                o.amount == null ? null : new Money(o.amount, o.currency), o.startDateTime, o.endDateTime);
    }

    private void ensureOwned(String accountId) {
        if (AccountEntity.findByIdForCustomer(accountId, consent.customerId()) == null) {
            throw new ForbiddenException("Account not owned by consenting customer");
        }
    }
}

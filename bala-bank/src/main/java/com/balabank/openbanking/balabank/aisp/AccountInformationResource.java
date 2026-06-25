package com.balabank.openbanking.balabank.aisp;

import com.balabank.openbanking.balabank.domain.AccountEntity;
import com.balabank.openbanking.balabank.domain.BalanceEntity;
import com.balabank.openbanking.balabank.domain.TransactionEntity;
import com.balabank.openbanking.balabank.security.ConsentContext;
import com.balabank.openbanking.common.Permission;
import com.balabank.openbanking.common.dto.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/** OBIE Account Information (AISP) endpoints, v3.1 subset. */
@Path("/open-banking/v3.1/aisp")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("accounts")
public class AccountInformationResource {

    @Inject
    ConsentContext consent;

    @GET
    @Path("/accounts")
    public ObResponse<Map<String, Object>> accounts() {
        consent.require(Permission.READ_ACCOUNTS_DETAIL);
        List<AccountDto> accounts = AccountEntity.<AccountEntity>list("customerId", consent.customerId()).stream()
                .filter(a -> consent.consentedAccounts().contains(a.accountId))
                .map(this::toDto)
                .toList();
        return ObResponse.ofResource("Account", accounts, "/open-banking/v3.1/aisp/accounts");
    }

    @GET
    @Path("/accounts/{accountId}/balances")
    public ObResponse<Map<String, Object>> balances(@PathParam("accountId") String accountId) {
        consent.require(Permission.READ_BALANCES);
        consent.requireAccount(accountId);
        ensureOwned(accountId);
        List<BalanceDto> balances = BalanceEntity.<BalanceEntity>list("accountId", accountId).stream()
                .map(b -> new BalanceDto(b.accountId, new Money(b.amount, b.currency),
                        b.creditDebitIndicator, b.type, b.dateTime))
                .toList();
        return ObResponse.ofResource("Balance", balances,
                "/open-banking/v3.1/aisp/accounts/" + accountId + "/balances");
    }

    @GET
    @Path("/accounts/{accountId}/transactions")
    public ObResponse<Map<String, Object>> transactions(@PathParam("accountId") String accountId) {
        consent.require(Permission.READ_TRANSACTIONS_DETAIL);
        consent.requireAccount(accountId);
        ensureOwned(accountId);
        List<TransactionDto> txns = TransactionEntity.<TransactionEntity>list("accountId", accountId).stream()
                .map(t -> new TransactionDto(String.valueOf(t.id), t.accountId, t.creditDebitIndicator,
                        t.status, new Money(t.amount, t.currency), t.bookingDateTime, t.transactionInformation))
                .toList();
        return ObResponse.ofResource("Transaction", txns,
                "/open-banking/v3.1/aisp/accounts/" + accountId + "/transactions");
    }

    private void ensureOwned(String accountId) {
        if (AccountEntity.findByIdForCustomer(accountId, consent.customerId()) == null) {
            throw new ForbiddenException("Account not owned by consenting customer");
        }
    }

    private AccountDto toDto(AccountEntity a) {
        return new AccountDto(a.accountId, a.status, a.currency, a.accountType,
                a.accountSubType, a.nickname,
                List.of(new AccountDto.Identifier("UK.OBIE.SortCodeAccountNumber", a.identification, a.name)));
    }
}

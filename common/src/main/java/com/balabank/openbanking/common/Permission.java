package com.balabank.openbanking.common;

/**
 * UK Open Banking (OBIE) account-access permissions and the OAuth2 scopes they map to.
 * A {@link #scope()} groups the broad API area; the permission is the fine-grained grant
 * the user selects on the consent screen.
 */
public enum Permission {

    // --- Account Information (scope: accounts) ---
    READ_ACCOUNTS_BASIC("ReadAccountsBasic", Scope.ACCOUNTS),
    READ_ACCOUNTS_DETAIL("ReadAccountsDetail", Scope.ACCOUNTS),
    READ_BALANCES("ReadBalances", Scope.ACCOUNTS),
    READ_TRANSACTIONS_BASIC("ReadTransactionsBasic", Scope.ACCOUNTS),
    READ_TRANSACTIONS_DETAIL("ReadTransactionsDetail", Scope.ACCOUNTS),
    READ_TRANSACTIONS_CREDITS("ReadTransactionsCredits", Scope.ACCOUNTS),
    READ_TRANSACTIONS_DEBITS("ReadTransactionsDebits", Scope.ACCOUNTS),
    READ_BENEFICIARIES_DETAIL("ReadBeneficiariesDetail", Scope.ACCOUNTS),
    READ_DIRECT_DEBITS("ReadDirectDebits", Scope.ACCOUNTS),
    READ_STANDING_ORDERS_DETAIL("ReadStandingOrdersDetail", Scope.ACCOUNTS),
    READ_SCHEDULED_PAYMENTS_DETAIL("ReadScheduledPaymentsDetail", Scope.ACCOUNTS),
    READ_PRODUCTS("ReadProducts", Scope.ACCOUNTS),
    READ_OFFERS("ReadOffers", Scope.ACCOUNTS),
    READ_PARTY("ReadParty", Scope.ACCOUNTS),
    READ_PARTY_PSU("ReadPartyPSU", Scope.ACCOUNTS),
    READ_STATEMENTS_DETAIL("ReadStatementsDetail", Scope.ACCOUNTS);

    /** OAuth2 scope groupings used by the gateway for coarse route protection. */
    public enum Scope {
        ACCOUNTS("accounts"),
        PAYMENTS("payments"),
        FUNDSCONFIRMATIONS("fundsconfirmations");

        private final String value;
        Scope(String value) { this.value = value; }
        public String value() { return value; }
    }

    private final String code;
    private final Scope scope;

    Permission(String code, Scope scope) {
        this.code = code;
        this.scope = scope;
    }

    /** OBIE permission code, e.g. {@code ReadAccountsDetail}. */
    public String code() { return code; }

    public Scope scope() { return scope; }

    public static Permission fromCode(String code) {
        for (Permission p : values()) {
            if (p.code.equalsIgnoreCase(code)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown OBIE permission: " + code);
    }
}

package com.balabank.openbanking.balabank.security;

import com.balabank.openbanking.common.ObClaims;
import com.balabank.openbanking.common.Permission;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.HashSet;
import java.util.Set;

/**
 * Per-request view of the consent encoded in the access token. Enforces that the caller
 * holds the required OBIE permission and only touches consented accounts.
 */
@RequestScoped
public class ConsentContext {

    @Inject
    JsonWebToken jwt;

    /** The bank customer the token represents (JWT subject). */
    public String customerId() {
        return jwt.getSubject();
    }

    public String consentId() {
        return jwt.getClaim(ObClaims.CONSENT_ID);
    }

    public Set<String> grantedPermissions() {
        return toStringSet(jwt.getClaim(ObClaims.PERMISSIONS));
    }

    public Set<String> consentedAccounts() {
        return toStringSet(jwt.getClaim(ObClaims.ACCOUNTS));
    }

    /** Throws 403 unless the granted permission set contains {@code required}. */
    public void require(Permission required) {
        if (!grantedPermissions().contains(required.code())) {
            throw new ForbiddenException("Consent does not grant " + required.code());
        }
    }

    /** Throws 403 if the account was not selected by the user during consent. */
    public void requireAccount(String accountId) {
        if (!consentedAccounts().contains(accountId)) {
            throw new ForbiddenException("Account not in consent: " + accountId);
        }
    }

    private static Set<String> toStringSet(Object claim) {
        Set<String> out = new HashSet<>();
        if (claim instanceof Iterable<?> it) {
            for (Object o : it) {
                // SmallRye exposes JSON array elements as jakarta.json.JsonString;
                // unwrap to the raw value rather than its quoted toString().
                if (o instanceof jakarta.json.JsonString js) {
                    out.add(js.getString());
                } else {
                    out.add(String.valueOf(o));
                }
            }
        }
        return out;
    }
}

package com.balabank.openbanking.consent.auth;

import com.balabank.openbanking.common.ObClaims;
import com.balabank.openbanking.common.Permission;
import com.balabank.openbanking.consent.domain.Consent;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Mints signed OBIE access tokens whose claims bind to an approved consent. */
@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "ob.token.ttl", defaultValue = "3600")
    long ttlSeconds;

    public String issueAccessToken(Consent consent) {
        List<String> permissions = splitList(consent.grantedPermissions);
        List<String> accounts = splitList(consent.grantedAccounts);

        // Derive coarse OAuth2 scopes (groups) used as roles by the resource server / gateway.
        Set<String> scopes = new HashSet<>();
        for (String code : permissions) {
            scopes.add(Permission.fromCode(code).scope().value());
        }
        if (consent.grantedPermissions == null || consent.grantedPermissions.isBlank()) {
            scopes.add(Permission.Scope.PAYMENTS.value()); // payment consents carry the payments scope
        }

        return Jwt.issuer(ObClaims.ISSUER)
                .subject(consent.customerId)
                .audience("bala-bank")
                .groups(scopes)
                .claim(ObClaims.CONSENT_ID, consent.consentId)
                .claim(ObClaims.CLIENT_ID, consent.clientId)
                // "key" claim lets the APISIX jwt-auth plugin map the token to its consumer.
                .claim("key", consent.clientId)
                .claim(ObClaims.PERMISSIONS, permissions)
                .claim(ObClaims.ACCOUNTS, accounts)
                .expiresIn(Duration.ofSeconds(ttlSeconds))
                .sign();
    }

    private List<String> splitList(String s) {
        if (s == null || s.isBlank()) {
            return List.of();
        }
        return Arrays.stream(s.trim().split("\\s+")).collect(Collectors.toList());
    }
}

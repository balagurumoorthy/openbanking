package com.balabank.openbanking.consent.auth;

import com.balabank.openbanking.common.ObClaims;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;

/** Minimal OIDC discovery document so the gateway can locate the JWKS and validate tokens. */
@Path("/.well-known/openid-configuration")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @ConfigProperty(name = "ob.issuer-base", defaultValue = "http://consent-auth:8081")
    String base;

    @GET
    public Map<String, Object> config() {
        return Map.of(
                "issuer", ObClaims.ISSUER,
                "authorization_endpoint", base + "/authorize",
                "token_endpoint", base + "/token",
                "jwks_uri", base + "/jwks",
                "response_types_supported", List.of("code"),
                "id_token_signing_alg_values_supported", List.of("RS256"));
    }
}

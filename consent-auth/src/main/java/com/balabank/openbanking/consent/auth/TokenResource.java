package com.balabank.openbanking.consent.auth;

import com.balabank.openbanking.common.ConsentStatus;
import com.balabank.openbanking.consent.domain.AuthorizationCode;
import com.balabank.openbanking.consent.domain.Consent;
import com.balabank.openbanking.consent.domain.RegisteredClient;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Map;

/** OAuth2 token endpoint: exchanges an authorization code for a signed OBIE access token. */
@Path("/token")
public class TokenResource {

    @Inject
    TokenService tokenService;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response token(@FormParam("grant_type") String grantType,
                          @FormParam("code") String code,
                          @FormParam("redirect_uri") String redirectUri,
                          @FormParam("client_id") String clientId,
                          @FormParam("client_secret") String clientSecret) {
        if (!"authorization_code".equals(grantType)) {
            return error(400, "unsupported_grant_type");
        }
        RegisteredClient client = RegisteredClient.findById(clientId);
        if (client == null || !client.clientSecret.equals(clientSecret)) {
            return error(401, "invalid_client");
        }
        AuthorizationCode ac = AuthorizationCode.findById(code);
        if (ac == null || ac.used || ac.expiresAt.isBefore(Instant.now())
                || !ac.clientId.equals(clientId) || !ac.redirectUri.equals(redirectUri)) {
            return error(400, "invalid_grant");
        }
        Consent consent = Consent.findById(ac.consentId);
        if (consent == null || consent.status != ConsentStatus.AUTHORISED) {
            return error(400, "invalid_grant");
        }
        ac.used = true;

        String accessToken = tokenService.issueAccessToken(consent);
        return Response.ok(Map.of(
                "access_token", accessToken,
                "token_type", "Bearer",
                "expires_in", 3600,
                "consent_id", consent.consentId)).build();
    }

    private Response error(int status, String err) {
        return Response.status(status).entity(Map.of("error", err)).build();
    }
}

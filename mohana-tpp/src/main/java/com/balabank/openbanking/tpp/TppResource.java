package com.balabank.openbanking.tpp;

import com.balabank.openbanking.tpp.client.ConsentAuthClient;
import com.balabank.openbanking.tpp.client.GatewayClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MohanaTPP web flow: initiate consent → handle redirect (validate state, exchange token) →
 * consume Open Banking APIs via the gateway.
 */
@Path("/")
public class TppResource {

    @Inject
    @RestClient
    ConsentAuthClient authClient;

    @Inject
    @RestClient
    GatewayClient gateway;

    @ConfigProperty(name = "tpp.client-id") String clientId;
    @ConfigProperty(name = "tpp.client-secret") String clientSecret;
    @ConfigProperty(name = "tpp.redirect-uri") String redirectUri;
    @ConfigProperty(name = "auth.public-base") String authPublicBase;

    /** state -> consentId, awaiting callback. Replace with a real session store in production. */
    private final Map<String, String> pendingState = new ConcurrentHashMap<>();
    /** Issued tokens keyed by an opaque session token returned to the browser. */
    private final Map<String, String> sessionTokens = new ConcurrentHashMap<>();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String home() {
        return "<h1>MohanaTPP</h1><p><a href=\"/connect\">Connect your Bala Bank account</a></p>";
    }

    /** Step 1+2: create an account-access intent, then redirect the user to /authorize. */
    @GET
    @Path("/connect")
    public Response connect() {
        List<String> permissions = List.of("ReadAccountsDetail", "ReadBalances", "ReadTransactionsDetail");
        Map<String, Object> intent = authClient.createIntent(new ConsentAuthClient.IntentRequest(clientId, permissions));
        @SuppressWarnings("unchecked")
        String consentId = (String) ((Map<String, Object>) intent.get("Data")).get("ConsentId");

        String state = UUID.randomUUID().toString();
        pendingState.put(state, consentId);

        String location = authPublicBase + "/authorize"
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&scope=" + enc("accounts")
                + "&state=" + enc(state)
                + "&consent_id=" + enc(consentId);
        return Response.seeOther(URI.create(location)).build();
    }

    /** Step 4+5: validate state, exchange the code for a token. */
    @GET
    @Path("/callback")
    @Produces(MediaType.TEXT_HTML)
    public Response callback(@QueryParam("code") String code,
                             @QueryParam("state") String state,
                             @QueryParam("error") String error) {
        if (error != null) {
            return Response.ok("<h1>Consent denied</h1><p>" + error + "</p>").build();
        }
        if (state == null || !pendingState.containsKey(state)) {
            return Response.status(400).entity("<h1>Invalid state</h1>").build();
        }
        pendingState.remove(state);

        Map<String, Object> tokenResp = authClient.token("authorization_code", code, redirectUri, clientId, clientSecret);
        String accessToken = (String) tokenResp.get("access_token");
        String session = UUID.randomUUID().toString();
        sessionTokens.put(session, accessToken);

        return Response.seeOther(URI.create("/accounts?s=" + session)).build();
    }

    /** Step 6: read account data via the gateway. */
    @GET
    @Path("/accounts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response accounts(@QueryParam("s") String session) {
        String token = sessionTokens.get(session);
        if (token == null) {
            return Response.status(401).entity(Map.of("error", "no_session")).build();
        }
        try {
            return Response.ok(gateway.accounts("Bearer " + token)).build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus())
                    .entity(Map.of("error", "gateway_denied", "status", e.getResponse().getStatus(),
                            "hint", "Re-consent may be required")).build();
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

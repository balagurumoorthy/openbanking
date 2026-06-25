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

        return Response.seeOther(URI.create("/dashboard?s=" + session)).build();
    }

    /** Step 6 (JSON): read account data via the gateway. */
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

    /** JSON: balances for a single consented account via the gateway. */
    @GET
    @Path("/accounts/{accountId}/balances")
    @Produces(MediaType.APPLICATION_JSON)
    public Response balances(@QueryParam("s") String session, @PathParam("accountId") String accountId) {
        String token = sessionTokens.get(session);
        if (token == null) {
            return Response.status(401).entity(Map.of("error", "no_session")).build();
        }
        try {
            return Response.ok(gateway.balances("Bearer " + token, accountId)).build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus())
                    .entity(Map.of("error", "gateway_denied", "status", e.getResponse().getStatus(),
                            "accountId", accountId, "hint", "ReadBalances may not be consented for this account"))
                    .build();
        }
    }

    /** Step 6 (UI): render the consented accounts and their balances as an HTML dashboard. */
    @GET
    @Path("/dashboard")
    @Produces(MediaType.TEXT_HTML)
    public Response dashboard(@QueryParam("s") String session) {
        String token = sessionTokens.get(session);
        if (token == null) {
            return Response.status(401)
                    .entity("<h1>Session expired</h1><p><a href=\"/connect\">Reconnect</a></p>").build();
        }
        String bearer = "Bearer " + token;
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>MohanaTPP — Your Accounts</title>")
                .append("<style>body{font-family:system-ui,sans-serif;max-width:640px;margin:40px auto;color:#1a2b4a}")
                .append(".card{border:1px solid #dde3ec;border-radius:12px;padding:18px 22px;margin:14px 0}")
                .append(".bal{font-size:1.6rem;font-weight:600}.muted{color:#6a7689;font-size:.85rem}")
                .append(".err{color:#b3261e}</style></head><body>")
                .append("<h1>Your Bala Bank accounts</h1>")
                .append("<p class=\"muted\">Fetched live via MohanaTPP → APISIX → Bala Bank using your consent.</p>");

        try {
            List<Map<String, Object>> accounts = extractList(gateway.accounts(bearer), "Account");
            if (accounts.isEmpty()) {
                html.append("<p class=\"muted\">No accounts were shared.</p>");
            }
            for (Map<String, Object> acct : accounts) {
                String accountId = String.valueOf(acct.get("AccountId"));
                String name = accountName(acct, accountId);
                html.append("<div class=\"card\"><h2>").append(esc(name)).append("</h2>")
                        .append("<div class=\"muted\">").append(esc(accountId)).append(" · ")
                        .append(esc(String.valueOf(acct.getOrDefault("AccountSubType", "")))).append("</div>");
                try {
                    List<Map<String, Object>> bals = extractList(gateway.balances(bearer, accountId), "Balance");
                    for (Map<String, Object> b : bals) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> amt = (Map<String, Object>) b.get("Amount");
                        String value = amt == null ? "?" : String.valueOf(amt.get("Amount"));
                        String ccy = amt == null ? "" : String.valueOf(amt.get("Currency"));
                        html.append("<div class=\"bal\">").append(esc(value)).append(" ").append(esc(ccy))
                                .append("</div><div class=\"muted\">").append(esc(String.valueOf(b.getOrDefault("Type", ""))))
                                .append(" · ").append(esc(String.valueOf(b.getOrDefault("CreditDebitIndicator", ""))))
                                .append("</div>");
                    }
                } catch (WebApplicationException e) {
                    html.append("<div class=\"err\">Balance unavailable (")
                            .append(e.getResponse().getStatus()).append(") — ReadBalances not consented for this account.</div>");
                }
                html.append("</div>");
            }
        } catch (WebApplicationException e) {
            html.append("<p class=\"err\">Could not fetch accounts (").append(e.getResponse().getStatus())
                    .append("). Re-consent may be required. <a href=\"/connect\">Reconnect</a></p>");
        }
        html.append("</body></html>");
        return Response.ok(html.toString()).build();
    }

    /** OBIE envelope: {@code {"Data":{"<resource>":[...]},"Links":{},"Meta":{}}}. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractList(Map<String, Object> obResponse, String resource) {
        Object data = obResponse.get("Data");
        if (!(data instanceof Map<?, ?> dataMap)) return List.of();
        Object arr = ((Map<String, Object>) dataMap).get(resource);
        if (arr instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    /** OBIE account name lives in the nested {@code Account[].Name}; fall back to the id. */
    @SuppressWarnings("unchecked")
    private static String accountName(Map<String, Object> acct, String fallback) {
        Object ids = acct.get("Account");
        if (ids instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object name = ((Map<String, Object>) first).get("Name");
            if (name != null) return String.valueOf(name);
        }
        Object nick = acct.get("Nickname");
        return nick != null ? String.valueOf(nick) : fallback;
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

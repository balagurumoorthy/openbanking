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
    /** state -> payment instruction, awaiting callback (the payments-scope authorisation). */
    private final Map<String, PaymentDraft> pendingPayments = new ConcurrentHashMap<>();
    /** Issued tokens keyed by an opaque session token returned to the browser. */
    private final Map<String, String> sessionTokens = new ConcurrentHashMap<>();

    /** A domestic-payment instruction captured on the /pay form, executed after authorisation. */
    record PaymentDraft(String debtorAccountId, String creditorIdentification, String creditorName,
                        String amount, String currency, String reference) {}

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String home() {
        return "<h1>MohanaTPP</h1>"
                + "<p><a href=\"/connect\">Connect your Bala Bank account</a> — view accounts &amp; balances</p>"
                + "<p><a href=\"/pay\">Make a domestic payment</a></p>";
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
        boolean isPayment = pendingPayments.containsKey(state);
        if (state == null || (!pendingState.containsKey(state) && !isPayment)) {
            return Response.status(400).entity("<h1>Invalid state</h1>").build();
        }

        Map<String, Object> tokenResp = authClient.token("authorization_code", code, redirectUri, clientId, clientSecret);
        String accessToken = (String) tokenResp.get("access_token");

        if (isPayment) {
            PaymentDraft draft = pendingPayments.remove(state);
            return executePayment("Bearer " + accessToken, draft);
        }

        pendingState.remove(state);
        String session = UUID.randomUUID().toString();
        sessionTokens.put(session, accessToken);
        return Response.seeOther(URI.create("/dashboard?s=" + session)).build();
    }

    /** Step 1 (payments): show the payment instruction form. */
    @GET
    @Path("/pay")
    @Produces(MediaType.TEXT_HTML)
    public String payForm() {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>MohanaTPP — Make a payment</title>"
                + "<style>body{font-family:system-ui,sans-serif;max-width:480px;margin:40px auto;color:#1a2b4a}"
                + "label{display:block;margin:10px 0 4px}input{width:100%;padding:8px}"
                + ".btn{margin-top:18px;padding:10px 18px;border:0;border-radius:8px;background:#1f6feb;color:#fff;font-size:1rem;cursor:pointer}</style>"
                + "</head><body><h1>Make a domestic payment</h1>"
                + "<p>You'll be sent to Bala Bank to authorise it.</p>"
                + "<form method=\"post\" action=\"/pay/start\">"
                + "<label>From account (debtor)</label><input name=\"debtorAccountId\" value=\"GB-ALICE-001\">"
                + "<label>Payee name</label><input name=\"creditorName\" value=\"Acme Utilities\">"
                + "<label>Payee sort code + account</label><input name=\"creditorIdentification\" value=\"20-00-00 41414141\">"
                + "<label>Amount</label><input name=\"amount\" value=\"25.00\">"
                + "<label>Currency</label><input name=\"currency\" value=\"GBP\">"
                + "<label>Reference</label><input name=\"reference\" value=\"Invoice 42\">"
                + "<button class=\"btn\" type=\"submit\">Authorise payment</button></form></body></html>";
    }

    /** Step 2 (payments): create a payment-consent intent, then redirect to /authorize (payments scope). */
    @POST
    @Path("/pay/start")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response payStart(@FormParam("debtorAccountId") String debtorAccountId,
                             @FormParam("creditorIdentification") String creditorIdentification,
                             @FormParam("creditorName") String creditorName,
                             @FormParam("amount") String amount,
                             @FormParam("currency") String currency,
                             @FormParam("reference") String reference) {
        Map<String, Object> intent = authClient.createPaymentIntent(new ConsentAuthClient.PaymentIntentRequest(clientId));
        @SuppressWarnings("unchecked")
        String consentId = (String) ((Map<String, Object>) intent.get("Data")).get("ConsentId");

        String state = UUID.randomUUID().toString();
        pendingPayments.put(state, new PaymentDraft(debtorAccountId, creditorIdentification, creditorName,
                amount, currency, reference));

        String location = authPublicBase + "/authorize"
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&scope=" + enc("payments")
                + "&state=" + enc(state)
                + "&consent_id=" + enc(consentId);
        return Response.seeOther(URI.create(location)).build();
    }

    /** Step 3 (payments): create the ASPSP payment-consent and execute it via the gateway, render status. */
    private Response executePayment(String bearer, PaymentDraft d) {
        try {
            Map<String, Object> consentBody = Map.of(
                    "debtorAccountId", d.debtorAccountId(), "creditorIdentification", d.creditorIdentification(),
                    "creditorName", d.creditorName(), "amount", d.amount(), "currency", d.currency(),
                    "reference", d.reference());
            Map<String, Object> consentResp = gateway.createPaymentConsent(bearer, consentBody);
            @SuppressWarnings("unchecked")
            String pcon = (String) ((Map<String, Object>) consentResp.get("Data")).get("ConsentId");

            Map<String, Object> payResp = gateway.executePayment(bearer, Map.of("consentId", pcon));
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payResp.get("Data");
            String status = String.valueOf(data.get("Status"));
            String paymentId = String.valueOf(data.get("DomesticPaymentId"));
            return Response.ok("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Payment submitted</title>"
                    + "<style>body{font-family:system-ui,sans-serif;max-width:520px;margin:40px auto;color:#1a2b4a}"
                    + ".ok{font-size:1.4rem;font-weight:600;color:#1a7f37}.muted{color:#6a7689}</style></head><body>"
                    + "<h1>Payment submitted</h1>"
                    + "<div class=\"ok\">" + esc(status) + "</div>"
                    + "<p class=\"muted\">Payment id " + esc(paymentId) + " — " + esc(d.amount()) + " "
                    + esc(d.currency()) + " to " + esc(d.creditorName()) + "</p>"
                    + "<p><a href=\"/\">Back</a></p></body></html>").build();
        } catch (WebApplicationException e) {
            return gatewayError(e, "/pay");
        }
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

    /** 401/403 from the gateway → a friendly page prompting the user to re-consent. */
    private static Response gatewayError(WebApplicationException e, String retryPath) {
        int status = e.getResponse().getStatus();
        String msg = status == 401 ? "Your authorisation has expired or is missing."
                : status == 403 ? "This action isn't covered by your consent (or the consent was revoked)."
                : "The gateway rejected the request (" + status + ").";
        return Response.status(status).entity(
                "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Re-consent needed</title>"
                + "<style>body{font-family:system-ui,sans-serif;max-width:520px;margin:40px auto;color:#1a2b4a}"
                + ".err{color:#b3261e}</style></head><body>"
                + "<h1 class=\"err\">Authorisation needed (" + status + ")</h1>"
                + "<p>" + esc(msg) + "</p>"
                + "<p><a href=\"" + esc(retryPath) + "\">Start again / re-consent</a></p></body></html>").build();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

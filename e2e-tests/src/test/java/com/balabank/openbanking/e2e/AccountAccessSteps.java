package com.balabank.openbanking.e2e;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/** Cucumber step definitions driving the live consent → token → API flow over HTTP. */
public class AccountAccessSteps {

    private final String authBase = System.getProperty("auth.base", "http://localhost:8081");
    private final String gatewayBase = System.getProperty("gateway.base", "http://localhost:9080");
    private final String tppClient = "mohana-tpp";
    private final String tppSecret = "mohana-secret";
    private final String redirectUri = System.getProperty("tpp.redirect", "http://localhost:8080/callback");

    private String consentId;
    private String authCode;
    private String accessToken;
    private Response lastResponse;
    private String lastRedirectLocation;

    @Given("the Open Banking platform is reachable")
    public void platformReachable() {
        int code = given().get(authBase + "/jwks").statusCode();
        assertEquals(200, code, "consent-auth JWKS should be reachable");
    }

    @Given("MohanaTPP initiates an account-access consent for {string}")
    public void initiateConsent(String permsCsv) {
        List<String> perms = List.of(permsCsv.split(","));
        Response r = given().contentType("application/json")
                .body(Map.of("clientId", tppClient, "permissions", perms))
                .post(authBase + "/account-access-consents");
        r.then().statusCode(200);
        consentId = r.jsonPath().getString("Data.ConsentId");
        assertNotNull(consentId);
    }

    @And("customer {string} logs in and approves account {string}")
    public void approve(String username, String accountId) {
        lastRedirectLocation = decision(username, "approve", accountId);
        authCode = queryParam(lastRedirectLocation, "code");
        assertNotNull(authCode, "authorization code expected on approve");
    }

    @When("customer {string} denies the consent")
    public void deny(String username) {
        lastRedirectLocation = decision(username, "deny", "GB-ALICE-001");
    }

    private String decision(String username, String decision, String accountId) {
        return given().redirects().follow(false)
                .contentType("application/x-www-form-urlencoded")
                .formParam("client_id", tppClient)
                .formParam("redirect_uri", redirectUri)
                .formParam("state", "test-state")
                .formParam("consent_id", consentId)
                .formParam("username", username)
                .formParam("password", "pw")
                .formParam("decision", decision)
                .formParam("permissions", "ReadAccountsDetail")
                .formParam("permissions", "ReadBalances")
                .formParam("permissions", "ReadTransactionsDetail")
                .formParam("accounts", accountId)
                .post(authBase + "/authorize/decision")
                .getHeader("Location");
    }

    @When("MohanaTPP exchanges the authorization code for an access token")
    public void exchangeToken() {
        Response r = given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "authorization_code")
                .formParam("code", authCode)
                .formParam("redirect_uri", redirectUri)
                .formParam("client_id", tppClient)
                .formParam("client_secret", tppSecret)
                .post(authBase + "/token");
        r.then().statusCode(200);
        accessToken = r.jsonPath().getString("access_token");
        assertNotNull(accessToken);
    }

    @And("MohanaTPP requests accounts via the gateway")
    public void requestAccounts() {
        lastResponse = given().header("Authorization", "Bearer " + accessToken)
                .get(gatewayBase + "/open-banking/v3.1/aisp/accounts");
    }

    @When("an unauthenticated request is made to the accounts endpoint")
    public void unauthenticated() {
        lastResponse = given().get(gatewayBase + "/open-banking/v3.1/aisp/accounts");
    }

    @Then("the response status is {int}")
    public void statusIs(int expected) {
        assertEquals(expected, lastResponse.statusCode());
    }

    @And("the accounts response contains account {string}")
    public void containsAccount(String accountId) {
        assertTrue(lastResponse.asString().contains(accountId),
                "expected account " + accountId + " in: " + lastResponse.asString());
    }

    @Then("the redirect contains error {string}")
    public void redirectHasError(String error) {
        assertNotNull(lastRedirectLocation);
        assertEquals(error, queryParam(lastRedirectLocation, "error"));
    }

    private static String queryParam(String url, String name) {
        if (url == null) return null;
        int q = url.indexOf('?');
        if (q < 0) return null;
        for (String pair : url.substring(q + 1).split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv[0].equals(name)) {
                return kv.length > 1 ? java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }
}

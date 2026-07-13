package com.balabank.openbanking.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline OBIE conformance check (task 8.4). Drives a real consent → token flow against
 * consent-auth, then validates Bala Bank's AIS response bodies (and an error body) against the
 * OBIE JSON Schemas in {@code src/test/resources/obie-schemas} using the networknt validator.
 * Talks to bala-bank directly (no gateway/Podman needed). Run: {@code mvn -pl e2e-tests -Pconformance test}
 * with consent-auth (:8081) and bala-bank (:8082) up.
 */
class ObieConformanceTest {

    private static final String AUTH = System.getProperty("auth.base", "http://localhost:8081");
    private static final String API = System.getProperty("api.base", "http://localhost:8082");
    private static final String CLIENT = "mohana-tpp";
    private static final String SECRET = "mohana-secret";
    private static final String REDIRECT = System.getProperty("tpp.redirect", "http://localhost:8080/callback");
    private static final String ACCOUNT = "GB-ALICE-001";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private static String token;

    @BeforeAll
    static void obtainToken() {
        // consent → approve → code → token, mirroring the TPP happy path.
        Response consent = given().contentType("application/json")
                .body(Map.of("clientId", CLIENT,
                        "permissions", List.of("ReadAccountsDetail", "ReadBalances", "ReadTransactionsDetail")))
                .post(AUTH + "/account-access-consents");
        consent.then().statusCode(200);
        String consentId = consent.jsonPath().getString("Data.ConsentId");

        String location = given().redirects().follow(false)
                .contentType("application/x-www-form-urlencoded")
                .formParam("client_id", CLIENT)
                .formParam("redirect_uri", REDIRECT)
                .formParam("state", "conf-state")
                .formParam("consent_id", consentId)
                .formParam("username", "alice")
                .formParam("password", "pw")
                .formParam("decision", "approve")
                .formParam("permissions", "ReadAccountsDetail")
                .formParam("permissions", "ReadBalances")
                .formParam("permissions", "ReadTransactionsDetail")
                .formParam("accounts", ACCOUNT)
                .post(AUTH + "/authorize/decision")
                .getHeader("Location");
        String code = queryParam(location, "code");
        assertNotNull(code, "authorization code expected");

        Response tok = given().contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "authorization_code")
                .formParam("code", code)
                .formParam("redirect_uri", REDIRECT)
                .formParam("client_id", CLIENT)
                .formParam("client_secret", SECRET)
                .post(AUTH + "/token");
        tok.then().statusCode(200);
        token = tok.jsonPath().getString("access_token");
        assertNotNull(token, "access token expected");
    }

    @Test
    void accountsConformToObie() {
        Response r = get("/open-banking/v3.1/aisp/accounts");
        r.then().statusCode(200);
        validate("account-response.schema.json", r.asString());
    }

    @Test
    void balancesConformToObie() {
        Response r = get("/open-banking/v3.1/aisp/accounts/" + ACCOUNT + "/balances");
        r.then().statusCode(200);
        validate("balance-response.schema.json", r.asString());
    }

    @Test
    void transactionsConformToObie() {
        Response r = get("/open-banking/v3.1/aisp/accounts/" + ACCOUNT + "/transactions");
        r.then().statusCode(200);
        validate("transaction-response.schema.json", r.asString());
    }

    @Test
    void errorBodyConformsToObie() {
        // A non-consented account yields a 403 OBIE error body.
        Response r = get("/open-banking/v3.1/aisp/accounts/GB-ALICE-002/balances");
        assertEquals(403, r.statusCode(), "expected 403 for a non-consented account");
        validate("error-response.schema.json", r.asString());
    }

    private Response get(String path) {
        return given().header("Authorization", "Bearer " + token).get(API + path);
    }

    /** Validates a response body against an OBIE schema; fails with the collected violations. */
    private void validate(String schemaFile, String body) {
        JsonSchema schema;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("obie-schemas/" + schemaFile)) {
            assertNotNull(in, "schema not found: " + schemaFile);
            schema = FACTORY.getSchema(in);
        } catch (Exception e) {
            throw new AssertionError("could not load schema " + schemaFile, e);
        }
        Set<ValidationMessage> errors = schema.validate(body, InputFormat.JSON);
        assertTrue(errors.isEmpty(),
                () -> "OBIE schema violations for " + schemaFile + ":\n  "
                        + String.join("\n  ", errors.stream().map(ValidationMessage::getMessage).toList())
                        + "\nbody: " + body);
    }

    private static String queryParam(String url, String name) {
        if (url == null) return null;
        int q = url.indexOf('?');
        if (q < 0) return null;
        for (String pair : url.substring(q + 1).split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv[0].equals(name)) {
                return kv.length > 1
                        ? java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }
}

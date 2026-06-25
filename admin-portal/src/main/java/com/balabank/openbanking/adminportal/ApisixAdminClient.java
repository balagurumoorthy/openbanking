package com.balabank.openbanking.adminportal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Thin wrapper over the APISIX Admin API for reading/updating consumer tiers and group limits. */
@ApplicationScoped
public class ApisixAdminClient {

    @ConfigProperty(name = "apisix.admin.url")
    String adminUrl;

    @ConfigProperty(name = "apisix.admin.key")
    String adminKey;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /** The APISIX object body (the `value` field of the admin response). */
    public JsonNode getValue(String path) {
        try {
            HttpResponse<String> r = http.send(req(path).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() >= 300) {
                throw new RuntimeException("Admin GET " + path + " -> " + r.statusCode() + ": " + r.body());
            }
            return mapper.readTree(r.body()).path("value");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** PUT a full object body to the Admin API (e.g. an updated consumer). */
    public void put(String path, JsonNode body) {
        try {
            HttpResponse<String> r = http.send(
                    req(path).PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() >= 300) {
                throw new RuntimeException("Admin PUT " + path + " -> " + r.statusCode() + ": " + r.body());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpRequest.Builder req(String path) {
        return HttpRequest.newBuilder(URI.create(adminUrl + path))
                .header("X-API-KEY", adminKey)
                .header("Content-Type", "application/json");
    }
}

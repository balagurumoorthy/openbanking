package com.balabank.openbanking.consent.auth;

import com.balabank.openbanking.common.ConsentStatus;
import com.balabank.openbanking.consent.domain.AuthorizationCode;
import com.balabank.openbanking.consent.domain.Consent;
import com.balabank.openbanking.consent.domain.Customer;
import com.balabank.openbanking.consent.domain.RegisteredClient;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * OAuth2 authorization endpoint. {@code GET /authorize} validates the client and renders the
 * login + consent screen; {@code POST /authorize/decision} authenticates the user, records the
 * granted permissions/accounts, and redirects back with an authorization code (or access_denied).
 */
@Path("/authorize")
public class AuthorizationResource {

    @Inject
    Template consent; // templates/consent.qute.html

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance authorize(@QueryParam("client_id") String clientId,
                                      @QueryParam("redirect_uri") String redirectUri,
                                      @QueryParam("scope") String scope,
                                      @QueryParam("state") String state,
                                      @QueryParam("consent_id") String consentId) {
        RegisteredClient client = RegisteredClient.findById(clientId);
        if (client == null || !client.redirectUri.equals(redirectUri)) {
            throw new BadRequestException("Invalid client or redirect_uri");
        }
        Consent c = Consent.findById(consentId);
        if (c == null) {
            throw new BadRequestException("Unknown consent_id");
        }
        return consent.data("clientId", clientId, "redirectUri", redirectUri,
                "state", state, "consentId", consentId,
                "permissions", List.of(c.requestedPermissions.split("\\s+")));
    }

    @POST
    @Path("/decision")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response decide(@FormParam("client_id") String clientId,
                           @FormParam("redirect_uri") String redirectUri,
                           @FormParam("state") String state,
                           @FormParam("consent_id") String consentId,
                           @FormParam("username") String username,
                           @FormParam("password") String password,
                           @FormParam("decision") String decision,
                           @FormParam("permissions") List<String> grantedPermissions,
                           @FormParam("accounts") List<String> grantedAccounts) {
        Customer customer = Customer.findByUsername(username);
        if (customer == null || !customer.password.equals(password)) {
            return Response.status(401).entity("Invalid credentials").build();
        }
        Consent c = Consent.findById(consentId);
        if (c == null) {
            throw new BadRequestException("Unknown consent_id");
        }

        if (!"approve".equalsIgnoreCase(decision)) {
            c.status = ConsentStatus.REJECTED;
            return redirect(redirectUri + "?error=access_denied&state=" + enc(state));
        }

        c.customerId = customer.customerId;
        c.grantedPermissions = String.join(" ", grantedPermissions);
        c.grantedAccounts = String.join(" ", grantedAccounts);
        c.status = ConsentStatus.AUTHORISED;
        c.expiresAt = Instant.now().plus(90, ChronoUnit.DAYS);

        AuthorizationCode code = new AuthorizationCode();
        code.code = "ac-" + UUID.randomUUID();
        code.clientId = clientId;
        code.consentId = consentId;
        code.customerId = customer.customerId;
        code.redirectUri = redirectUri;
        code.expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);
        code.persist();

        return redirect(redirectUri + "?code=" + enc(code.code) + "&state=" + enc(state));
    }

    private Response redirect(String location) {
        return Response.seeOther(URI.create(location)).build();
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
    }
}

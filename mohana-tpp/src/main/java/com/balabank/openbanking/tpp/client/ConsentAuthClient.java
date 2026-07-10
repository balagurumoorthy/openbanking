package com.balabank.openbanking.tpp.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.Map;

/** Server-to-server calls to the consent-auth service (intent registration + token exchange). */
@RegisterRestClient(configKey = "consent-auth")
@Path("/")
public interface ConsentAuthClient {

    record IntentRequest(String clientId, List<String> permissions) {}

    record PaymentIntentRequest(String clientId) {}

    @POST
    @Path("/account-access-consents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> createIntent(IntentRequest req);

    @POST
    @Path("/domestic-payment-consents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> createPaymentIntent(PaymentIntentRequest req);

    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> token(@FormParam("grant_type") String grantType,
                              @FormParam("code") String code,
                              @FormParam("redirect_uri") String redirectUri,
                              @FormParam("client_id") String clientId,
                              @FormParam("client_secret") String clientSecret);
}

package com.balabank.openbanking.tpp.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

/** Calls Bala Bank's Open Banking APIs through the APISIX gateway using the access token. */
@RegisterRestClient(configKey = "gateway")
@Path("/open-banking/v3.1")
@Produces(MediaType.APPLICATION_JSON)
public interface GatewayClient {

    @GET
    @Path("/aisp/accounts")
    Map<String, Object> accounts(@HeaderParam("Authorization") String bearer);

    @GET
    @Path("/aisp/accounts/{accountId}/balances")
    Map<String, Object> balances(@HeaderParam("Authorization") String bearer,
                                 @PathParam("accountId") String accountId);

    @GET
    @Path("/aisp/accounts/{accountId}/transactions")
    Map<String, Object> transactions(@HeaderParam("Authorization") String bearer,
                                     @PathParam("accountId") String accountId);

    @POST
    @Path("/pisp/domestic-payment-consents")
    @Consumes(MediaType.APPLICATION_JSON)
    Map<String, Object> createPaymentConsent(@HeaderParam("Authorization") String bearer, Map<String, Object> body);

    @POST
    @Path("/pisp/domestic-payments")
    @Consumes(MediaType.APPLICATION_JSON)
    Map<String, Object> executePayment(@HeaderParam("Authorization") String bearer, Map<String, Object> body);
}

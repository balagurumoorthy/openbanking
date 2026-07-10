package com.balabank.openbanking.balabank.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;

/**
 * OBIE/FAPI transport headers. Echoes the caller's {@code x-fapi-interaction-id} (generating one
 * when absent) so a TPP can correlate request/response, per the Read/Write standard. Applied to
 * every Open Banking response.
 */
@Provider
public class FapiHeadersFilter implements ContainerResponseFilter {

    private static final String INTERACTION_ID = "x-fapi-interaction-id";

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext resp) {
        String interactionId = req.getHeaderString(INTERACTION_ID);
        if (interactionId == null || interactionId.isBlank()) {
            interactionId = UUID.randomUUID().toString();
        }
        resp.getHeaders().putSingle(INTERACTION_ID, interactionId);
    }
}

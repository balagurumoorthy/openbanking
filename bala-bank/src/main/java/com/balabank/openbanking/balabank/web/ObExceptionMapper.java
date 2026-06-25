package com.balabank.openbanking.balabank.web;

import com.balabank.openbanking.common.dto.ObError;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps JAX-RS exceptions to OBIE error bodies with appropriate HTTP status. */
@Provider
public class ObExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException ex) {
        if (ex instanceof ForbiddenException) {
            return build(403, "403 Forbidden", "UK.OBIE.Resource.ConsentMismatch", ex.getMessage());
        }
        if (ex instanceof BadRequestException) {
            return build(400, "400 Bad Request", "UK.OBIE.Field.Invalid", ex.getMessage());
        }
        if (ex instanceof NotFoundException) {
            return build(404, "404 Not Found", "UK.OBIE.Resource.NotFound", ex.getMessage());
        }
        return build(500, "500 Internal Server Error", "UK.OBIE.UnexpectedError", "Unexpected error");
    }

    private Response build(int status, String code, String obieCode, String message) {
        return Response.status(status).entity(ObError.of(code, obieCode, message)).build();
    }
}

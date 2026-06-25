package com.balabank.openbanking.consent.auth;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Publishes the RSA signing public key as a JWKS document so the gateway/resource server
 * can verify access tokens.
 */
@Path("/jwks")
public class JwksResource {

    @ConfigProperty(name = "mp.jwt.verify.publickey.location", defaultValue = "jwt-signing.pub")
    String publicKeyLocation;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> jwks() throws Exception {
        RSAPublicKey key = loadPublicKey();
        Base64.Encoder url = Base64.getUrlEncoder().withoutPadding();
        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", "balabank-consent-1",
                "n", url.encodeToString(toUnsigned(key.getModulus().toByteArray())),
                "e", url.encodeToString(toUnsigned(key.getPublicExponent().toByteArray())));
        return Map.of("keys", List.of(jwk));
    }

    private RSAPublicKey loadPublicKey() throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(publicKeyLocation)) {
            byte[] bytes = (in != null ? in.readAllBytes()
                    : java.nio.file.Files.readAllBytes(java.nio.file.Path.of(publicKeyLocation)));
            String pem = new String(bytes, StandardCharsets.UTF_8)
                    .replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(pem);
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        }
    }

    /** Strip a leading sign byte BigInteger may add, as required by JWK base64url encoding. */
    private static byte[] toUnsigned(byte[] b) {
        if (b.length > 1 && b[0] == 0) {
            byte[] t = new byte[b.length - 1];
            System.arraycopy(b, 1, t, 0, t.length);
            return t;
        }
        return b;
    }
}

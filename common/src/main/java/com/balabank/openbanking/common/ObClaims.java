package com.balabank.openbanking.common;

/** Custom JWT claim names shared between the consent-auth issuer and the resource/gateway verifiers. */
public final class ObClaims {
    private ObClaims() {}

    /** Consent id the access token is bound to. */
    public static final String CONSENT_ID = "consent_id";
    /** OBIE permission codes granted (array claim). */
    public static final String PERMISSIONS = "permissions";
    /** Account ids the user selected to share (array claim). */
    public static final String ACCOUNTS = "accounts";
    /** OAuth2 client id of the TPP the token was issued to. */
    public static final String CLIENT_ID = "client_id";

    public static final String ISSUER = "https://consent-auth.balabank.local";
}

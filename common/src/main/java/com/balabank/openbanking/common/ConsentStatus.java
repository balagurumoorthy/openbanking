package com.balabank.openbanking.common;

/** OBIE consent status model (shared by account-access and payment consents). */
public enum ConsentStatus {
    AWAITING_AUTHORISATION,
    AUTHORISED,
    REJECTED,
    REVOKED,
    EXPIRED,
    CONSUMED
}

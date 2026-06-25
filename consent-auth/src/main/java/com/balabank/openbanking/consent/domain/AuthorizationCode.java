package com.balabank.openbanking.consent.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Short-lived OAuth2 authorization code bound to an approved consent. */
@Entity
@Table(name = "auth_code")
public class AuthorizationCode extends PanacheEntityBase {
    @Id
    public String code;
    public String clientId;
    public String consentId;
    public String customerId;
    public String redirectUri;
    public Instant expiresAt;
    public boolean used;
}

package com.balabank.openbanking.consent.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A registered TPP OAuth2 client (e.g. MohanaTPP). */
@Entity
@Table(name = "registered_client")
public class RegisteredClient extends PanacheEntityBase {
    @Id
    @Column(name = "client_id")
    public String clientId;
    public String clientSecret;
    public String redirectUri;
    public String allowedScopes; // space-separated
}

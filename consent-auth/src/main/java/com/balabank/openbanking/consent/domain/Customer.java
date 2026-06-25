package com.balabank.openbanking.consent.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer extends PanacheEntityBase {
    @Id
    @Column(name = "customer_id")
    public String customerId;
    public String username;
    public String password; // demo only: plaintext seed; real impl would hash
    public String displayName;

    public static Customer findByUsername(String username) {
        return find("username", username).firstResult();
    }
}

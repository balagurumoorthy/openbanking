package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "party")
public class PartyEntity extends PanacheEntity {
    public String customerId;
    public String accountId;
    public String partyId;
    public String partyNumber;
    public String fullLegalName;
    public String partyType;
    public String emailAddress;
    public String phoneNumber;
}

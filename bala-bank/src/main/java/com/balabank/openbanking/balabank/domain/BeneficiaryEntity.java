package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "beneficiary")
public class BeneficiaryEntity extends PanacheEntity {
    public String accountId;
    public String beneficiaryId;
    public String accountIdentification;
    public String accountName;
    public String accountSortCode;
    public String reference;
}

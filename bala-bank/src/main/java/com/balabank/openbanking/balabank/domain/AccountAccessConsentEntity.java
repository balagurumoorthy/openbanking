package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "account_access_consent")
public class AccountAccessConsentEntity extends PanacheEntityBase {
    @Id
    @Column(name = "consent_id")
    public String consentId;
    public String customerId;
    public String status;
    /** Comma-separated OBIE permission codes. */
    public String permissions;
    public OffsetDateTime expirationDateTime;
    public OffsetDateTime transactionFromDateTime;
    public OffsetDateTime transactionToDateTime;
    public OffsetDateTime creationDateTime;
    public OffsetDateTime statusUpdateDateTime;

    public static AccountAccessConsentEntity findByConsentId(String consentId) {
        return findById(consentId);
    }
}

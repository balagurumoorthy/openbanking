package com.balabank.openbanking.balabank.domain;

import com.balabank.openbanking.common.ConsentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** CBPII funds-confirmation-consent (OBIE OBFundsConfirmationConsent1). */
@Entity
@Table(name = "funds_confirmation_consent")
public class FundsConfirmationConsentEntity extends PanacheEntityBase {
    @Id
    @Column(name = "consent_id")
    public String consentId;
    public String debtorAccountId;
    public OffsetDateTime expirationDateTime;
    @Enumerated(EnumType.STRING)
    public ConsentStatus status;
}

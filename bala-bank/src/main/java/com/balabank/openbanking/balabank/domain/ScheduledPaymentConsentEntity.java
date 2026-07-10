package com.balabank.openbanking.balabank.domain;

import com.balabank.openbanking.common.ConsentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "scheduled_payment_consent")
public class ScheduledPaymentConsentEntity extends PanacheEntityBase {
    @Id
    @Column(name = "consent_id")
    public String consentId;
    public String debtorAccountId;
    public String creditorIdentification;
    public String creditorName;
    public BigDecimal amount;
    public String currency;
    public String reference;
    public OffsetDateTime requestedExecutionDateTime;
    @Enumerated(EnumType.STRING)
    public ConsentStatus status;
}

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

@Entity
@Table(name = "standing_order_consent")
public class StandingOrderConsentEntity extends PanacheEntityBase {
    @Id
    @Column(name = "consent_id")
    public String consentId;
    public String debtorAccountId;
    public String creditorIdentification;
    public String creditorName;
    public BigDecimal firstPaymentAmount;
    public String currency;
    public String reference;
    public String frequency;
    public Integer numberOfPayments;
    @Enumerated(EnumType.STRING)
    public ConsentStatus status;
}

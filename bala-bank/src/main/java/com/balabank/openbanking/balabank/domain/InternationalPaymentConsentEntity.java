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
@Table(name = "international_payment_consent")
public class InternationalPaymentConsentEntity extends PanacheEntityBase {
    @Id
    @Column(name = "consent_id")
    public String consentId;
    public String debtorAccountId;
    public String creditorAccountIdentification;
    public String creditorAccountName;
    public String creditorAgentBic;
    public BigDecimal instructedAmount;
    public String instructedCurrency;
    public String reference;
    @Enumerated(EnumType.STRING)
    public ConsentStatus status;
}

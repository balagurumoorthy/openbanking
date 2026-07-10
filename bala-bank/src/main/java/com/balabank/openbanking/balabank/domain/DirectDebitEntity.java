package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "direct_debit")
public class DirectDebitEntity extends PanacheEntity {
    public String accountId;
    public String directDebitId;
    public String frequency;
    public String status;
    public BigDecimal previousPaymentAmount;
    public String previousPaymentCurrency;
    public OffsetDateTime previousPaymentDate;
    public String name;
}

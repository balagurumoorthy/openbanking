package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "standing_order")
public class StandingOrderEntity extends PanacheEntity {
    public String accountId;
    public String standingOrderId;
    public String frequency;
    public String status;
    public BigDecimal nextPaymentAmount;
    public String nextPaymentCurrency;
    public OffsetDateTime nextPaymentDate;
    public BigDecimal finalPaymentAmount;
    public String finalPaymentCurrency;
    public String creditorIdentification;
    public String creditorName;
}

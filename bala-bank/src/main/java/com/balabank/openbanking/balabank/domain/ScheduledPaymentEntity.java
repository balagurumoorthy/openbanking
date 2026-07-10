package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "scheduled_payment")
public class ScheduledPaymentEntity extends PanacheEntity {
    public String accountId;
    public String scheduledPaymentId;
    public OffsetDateTime scheduledPaymentDate;
    public String scheduledType;
    public BigDecimal amount;
    public String currency;
    public String creditorIdentification;
    public String creditorName;
    public String reference;
}

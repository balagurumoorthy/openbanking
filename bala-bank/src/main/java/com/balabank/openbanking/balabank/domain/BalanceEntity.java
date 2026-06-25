package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "balance")
public class BalanceEntity extends PanacheEntity {
    public String accountId;
    public BigDecimal amount;
    public String currency;
    public String creditDebitIndicator;
    public String type;
    public OffsetDateTime dateTime;
}

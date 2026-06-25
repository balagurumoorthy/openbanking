package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transaction")
public class TransactionEntity extends PanacheEntity {
    public String accountId;
    public String creditDebitIndicator;
    public String status;
    public BigDecimal amount;
    public String currency;
    public OffsetDateTime bookingDateTime;
    public String transactionInformation;
}

package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "offer")
public class OfferEntity extends PanacheEntity {
    public String accountId;
    public String offerId;
    public String offerType;
    public String description;
    public BigDecimal amount;
    public String currency;
    public OffsetDateTime startDateTime;
    public OffsetDateTime endDateTime;
}

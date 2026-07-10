package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "statement")
public class StatementEntity extends PanacheEntity {
    public String accountId;
    public String statementId;
    public String statementType;
    public OffsetDateTime startDateTime;
    public OffsetDateTime endDateTime;
    public OffsetDateTime creationDateTime;
}

package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class ProductEntity extends PanacheEntity {
    public String accountId;
    public String productId;
    public String productName;
    public String productType;
    public String marketingState;
}

package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "domestic_standing_order")
public class DomesticStandingOrderEntity extends PanacheEntityBase {
    @Id
    @Column(name = "standing_order_id")
    public String standingOrderId;
    public String consentId;
    /** OBIE standing order status, e.g. Active / InactiveOrDormant. */
    public String status;
}

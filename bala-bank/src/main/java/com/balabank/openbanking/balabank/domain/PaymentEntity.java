package com.balabank.openbanking.balabank.domain;

import com.balabank.openbanking.common.PaymentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment")
public class PaymentEntity extends PanacheEntityBase {
    @Id
    @Column(name = "payment_id")
    public String paymentId;
    public String consentId;
    @Enumerated(EnumType.STRING)
    public PaymentStatus status;
}

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
@Table(name = "international_payment")
public class InternationalPaymentEntity extends PanacheEntityBase {
    @Id
    @Column(name = "international_payment_id")
    public String internationalPaymentId;
    public String consentId;
    @Enumerated(EnumType.STRING)
    public PaymentStatus status;
}

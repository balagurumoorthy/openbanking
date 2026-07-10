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
@Table(name = "domestic_scheduled_payment")
public class DomesticScheduledPaymentEntity extends PanacheEntityBase {
    @Id
    @Column(name = "scheduled_payment_id")
    public String scheduledPaymentId;
    public String consentId;
    @Enumerated(EnumType.STRING)
    public PaymentStatus status;
}

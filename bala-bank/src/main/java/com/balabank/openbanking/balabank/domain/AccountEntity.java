package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class AccountEntity extends PanacheEntityBase {
    @Id
    @Column(name = "account_id")
    public String accountId;
    public String customerId;
    public String status;
    public String currency;
    public String accountType;
    public String accountSubType;
    public String nickname;
    public String identification;
    public String name;

    public static AccountEntity findByIdForCustomer(String accountId, String customerId) {
        return find("accountId = ?1 and customerId = ?2", accountId, customerId).firstResult();
    }
}

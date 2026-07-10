package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Event Notification API (6.5) callback-urls resource. */
@Entity
@Table(name = "callback_url")
public class CallbackUrlEntity extends PanacheEntityBase {
    @Id
    @Column(name = "callback_url_id")
    public String callbackUrlId;
    public String clientId;
    public String url;
    public String version;
}

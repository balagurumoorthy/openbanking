package com.balabank.openbanking.balabank.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Event Notification API (6.5) event-subscriptions resource. */
@Entity
@Table(name = "event_subscription")
public class EventSubscriptionEntity extends PanacheEntityBase {
    @Id
    @Column(name = "subscription_id")
    public String subscriptionId;
    public String clientId;
    public String callbackUrl;
    /** Comma-separated OBIE event type URNs, e.g. urn:uk:org:openbanking:events:resource-update. */
    public String eventTypes;
    public String version;
}

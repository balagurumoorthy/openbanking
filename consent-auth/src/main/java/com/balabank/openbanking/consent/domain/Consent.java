package com.balabank.openbanking.consent.domain;

import com.balabank.openbanking.common.ConsentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

/** Account-access consent: requested vs granted permissions and selected accounts. */
@Entity
@Table(name = "consent")
public class Consent extends PanacheEntityBase {
    @Id
    @Column(name = "consent_id")
    public String consentId;
    public String clientId;
    public String customerId;
    @Column(length = 2000)
    public String requestedPermissions; // space-separated OBIE codes
    @Column(length = 2000)
    public String grantedPermissions;   // space-separated, set at approval
    @Column(length = 2000)
    public String grantedAccounts;      // space-separated account ids
    @Enumerated(EnumType.STRING)
    public ConsentStatus status;
    public Instant expiresAt;
}

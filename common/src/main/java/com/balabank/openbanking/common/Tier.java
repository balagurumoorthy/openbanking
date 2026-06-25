package com.balabank.openbanking.common;

/**
 * API plan tiers for a TPP. Each tier grants a different request allowance per minute,
 * enforced by the rate limiter (and mirrored by APISIX consumer-group limit-count config).
 */
public enum Tier {
    SILVER(5),
    GOLD(20),
    DIAMOND(100);

    private final int requestsPerMinute;

    Tier(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public int requestsPerMinute() {
        return requestsPerMinute;
    }

    /** Next tier up, or null if already the highest. */
    public Tier next() {
        return switch (this) {
            case SILVER -> GOLD;
            case GOLD -> DIAMOND;
            case DIAMOND -> null;
        };
    }
}

package com.balabank.openbanking.common;

/** OBIE domestic payment status model. */
public enum PaymentStatus {
    PENDING,
    ACCEPTED_SETTLEMENT_IN_PROCESS,   // AcceptedSettlementInProcess
    ACCEPTED_SETTLEMENT_COMPLETED,    // AcceptedSettlementCompleted
    REJECTED;

    /** OBIE wire value, e.g. {@code AcceptedSettlementInProcess}. */
    public String obieValue() {
        StringBuilder sb = new StringBuilder();
        for (String part : name().split("_")) {
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}

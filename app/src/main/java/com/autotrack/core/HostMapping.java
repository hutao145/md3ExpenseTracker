package com.autotrack.core;

public interface HostMapping {
    String mapCategory(TransactionCandidate candidate);

    String mapAccount(TransactionCandidate candidate);

    static HostMapping identity() {
        return new HostMapping() {
            @Override
            public String mapCategory(TransactionCandidate candidate) {
                return candidate == null ? "" : candidate.getCategoryHint();
            }

            @Override
            public String mapAccount(TransactionCandidate candidate) {
                return candidate == null ? "" : candidate.getPaymentMethod();
            }
        };
    }
}

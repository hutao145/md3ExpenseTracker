package com.autotrack.core;

public final class TransactionDraft {
    private final TransactionCandidate candidate;
    private final String mappedCategory;
    private final String mappedAccount;

    public TransactionDraft(TransactionCandidate candidate, String mappedCategory, String mappedAccount) {
        this.candidate = candidate;
        this.mappedCategory = mappedCategory == null ? "" : mappedCategory;
        this.mappedAccount = mappedAccount == null ? "" : mappedAccount;
    }

    public TransactionCandidate getCandidate() {
        return candidate;
    }

    public String getMappedCategory() {
        return mappedCategory;
    }

    public String getMappedAccount() {
        return mappedAccount;
    }
}

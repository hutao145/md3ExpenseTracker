package com.autotrack.core;

public final class RecognitionResult {
    private static final RecognitionResult EMPTY = new RecognitionResult(null, false, "");

    private final TransactionCandidate candidate;
    private final boolean matched;
    private final String reason;

    private RecognitionResult(TransactionCandidate candidate, boolean matched, String reason) {
        this.candidate = candidate;
        this.matched = matched;
        this.reason = reason == null ? "" : reason;
    }

    public static RecognitionResult empty() {
        return EMPTY;
    }

    public static RecognitionResult matched(TransactionCandidate candidate, String reason) {
        return new RecognitionResult(candidate, candidate != null, reason);
    }

    public TransactionCandidate getCandidate() {
        return candidate;
    }

    public boolean isMatched() {
        return matched;
    }

    public String getReason() {
        return reason;
    }
}

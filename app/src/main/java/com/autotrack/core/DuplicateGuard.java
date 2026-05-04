package com.autotrack.core;

public final class DuplicateGuard {
    private final long intervalMillis;
    private String lastSignature = "";
    private long lastAcceptedAt = 0;

    public DuplicateGuard(long intervalMillis) {
        this.intervalMillis = Math.max(0, intervalMillis);
    }

    public synchronized boolean shouldAccept(TransactionCandidate candidate) {
        if (candidate == null) return false;
        long now = System.currentTimeMillis();
        String signature = candidate.getSignature();
        if (signature.equals(lastSignature) && now - lastAcceptedAt < intervalMillis) {
            return false;
        }
        lastSignature = signature;
        lastAcceptedAt = now;
        return true;
    }
}

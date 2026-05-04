package com.autotrack.core;

public interface RecordSink {
    void save(TransactionCandidate candidate);
}

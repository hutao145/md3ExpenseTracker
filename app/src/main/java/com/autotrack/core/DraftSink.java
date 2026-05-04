package com.autotrack.core;

public interface DraftSink {
    void save(TransactionDraft draft);
}

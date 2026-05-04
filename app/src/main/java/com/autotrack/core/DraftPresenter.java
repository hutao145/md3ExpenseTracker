package com.autotrack.core;

public interface DraftPresenter {
    void present(TransactionDraft draft, Callback callback);

    interface Callback {
        void onConfirmed(TransactionDraft draft);

        void onCancelled();
    }
}

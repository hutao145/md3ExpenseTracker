package com.autotrack.core;

public interface ConfirmPresenter {
    void present(TransactionCandidate candidate, Callback callback);

    interface Callback {
        void onConfirmed(TransactionCandidate candidate);

        void onCancelled();
    }
}

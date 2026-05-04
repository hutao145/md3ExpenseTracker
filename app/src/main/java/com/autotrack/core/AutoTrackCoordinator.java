package com.autotrack.core;

public final class AutoTrackCoordinator {
    private final RecognitionEngine recognitionEngine;
    private final ConfirmPresenter confirmPresenter;
    private final RecordSink recordSink;
    private final DuplicateGuard duplicateGuard;

    public AutoTrackCoordinator(RecognitionEngine recognitionEngine, ConfirmPresenter confirmPresenter, RecordSink recordSink) {
        this(recognitionEngine, confirmPresenter, recordSink, new DuplicateGuard(2500));
    }

    public AutoTrackCoordinator(RecognitionEngine recognitionEngine, ConfirmPresenter confirmPresenter, RecordSink recordSink, DuplicateGuard duplicateGuard) {
        this.recognitionEngine = recognitionEngine;
        this.confirmPresenter = confirmPresenter;
        this.recordSink = recordSink;
        this.duplicateGuard = duplicateGuard;
    }

    public boolean handle(PageSnapshot snapshot) {
        RecognitionResult result = recognitionEngine.recognize(snapshot);
        if (!result.isMatched()) return false;

        TransactionCandidate candidate = result.getCandidate();
        if (!duplicateGuard.shouldAccept(candidate)) return false;

        confirmPresenter.present(candidate, new ConfirmPresenter.Callback() {
            @Override
            public void onConfirmed(TransactionCandidate confirmedCandidate) {
                recordSink.save(confirmedCandidate);
            }

            @Override
            public void onCancelled() {
                // 宿主 App 可在 ConfirmPresenter 内自行处理取消事件。
            }
        });
        return true;
    }
}

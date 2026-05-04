package com.autotrack.core;

public final class MappedAutoTrackCoordinator {
    private final RecognitionEngine recognitionEngine;
    private final HostMapping hostMapping;
    private final DraftPresenter draftPresenter;
    private final DraftSink draftSink;
    private final DuplicateGuard duplicateGuard;

    public MappedAutoTrackCoordinator(RecognitionEngine recognitionEngine, HostMapping hostMapping, DraftPresenter draftPresenter, DraftSink draftSink) {
        this(recognitionEngine, hostMapping, draftPresenter, draftSink, new DuplicateGuard(2500));
    }

    public MappedAutoTrackCoordinator(RecognitionEngine recognitionEngine, HostMapping hostMapping, DraftPresenter draftPresenter, DraftSink draftSink, DuplicateGuard duplicateGuard) {
        this.recognitionEngine = recognitionEngine;
        this.hostMapping = hostMapping == null ? HostMapping.identity() : hostMapping;
        this.draftPresenter = draftPresenter;
        this.draftSink = draftSink;
        this.duplicateGuard = duplicateGuard;
    }

    public boolean handle(PageSnapshot snapshot) {
        RecognitionResult result = recognitionEngine.recognize(snapshot);
        if (!result.isMatched()) return false;

        TransactionCandidate candidate = result.getCandidate();
        if (!duplicateGuard.shouldAccept(candidate)) return false;

        TransactionDraft draft = new TransactionDraft(
                candidate,
                hostMapping.mapCategory(candidate),
                hostMapping.mapAccount(candidate)
        );

        draftPresenter.present(draft, new DraftPresenter.Callback() {
            @Override
            public void onConfirmed(TransactionDraft confirmedDraft) {
                draftSink.save(confirmedDraft);
            }

            @Override
            public void onCancelled() {
                // 宿主 App 可在 DraftPresenter 内自行处理取消事件。
            }
        });
        return true;
    }
}

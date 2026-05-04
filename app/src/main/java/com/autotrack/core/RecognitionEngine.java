package com.autotrack.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RecognitionEngine {
    private final List<PageRecognizer> recognizers;

    public RecognitionEngine(List<PageRecognizer> recognizers) {
        this.recognizers = Collections.unmodifiableList(new ArrayList<>(recognizers == null ? Collections.emptyList() : recognizers));
    }

    public RecognitionResult recognize(PageSnapshot snapshot) {
        if (snapshot == null) return RecognitionResult.empty();
        for (PageRecognizer recognizer : recognizers) {
            if (!recognizer.supports(snapshot)) continue;
            RecognitionResult result = recognizer.recognize(snapshot);
            if (result != null && result.isMatched()) return result;
        }
        return RecognitionResult.empty();
    }
}

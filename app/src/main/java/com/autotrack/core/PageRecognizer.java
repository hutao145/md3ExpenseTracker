package com.autotrack.core;

public interface PageRecognizer {
    boolean supports(PageSnapshot snapshot);

    RecognitionResult recognize(PageSnapshot snapshot);
}

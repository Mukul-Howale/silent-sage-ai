package org.example;

import java.util.ArrayList;
import java.util.List;

public class TranscriptStorage {
    private final List<String> transcripts;
    private static final int MAX_STORAGE = 1000; // Prevent memory issues

    public TranscriptStorage() {
        this.transcripts = new ArrayList<>();
        Logger.info("TranscriptStorage initialized");
    }

    public void addTranscript(String transcript) {
        if (transcripts.size() >= MAX_STORAGE) {
            transcripts.remove(0); // Remove oldest transcript
        }
        transcripts.add(transcript);
        Logger.debug("Added transcript to storage");
    }

    public String getMergedTranscript() {
        return String.join(" ", transcripts);
    }

    public void clear() {
        transcripts.clear();
        Logger.info("Transcript storage cleared");
    }
} 
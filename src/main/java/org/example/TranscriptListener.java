package org.example;

import java.util.function.Consumer;

public class TranscriptListener {
    private final String deepgramApiKey;
    private Consumer<String> transcriptCallback;

    public TranscriptListener(String deepgramApiKey) {
        this.deepgramApiKey = deepgramApiKey;
    }

    public void setTranscriptCallback(Consumer<String> callback) {
        this.transcriptCallback = callback;
    }

    public void startListening() {
        // Mock implementation for now
        // You can integrate Deepgram's WebSocket here
        System.out.println("🎙 Listening started (mocked).");

        // Simulate receiving a transcript
        new Thread(() -> {
            try {
                Thread.sleep(3000); // simulate delay
                if (transcriptCallback != null) {
                    transcriptCallback.accept("Suppose we have a tree, how do you do in-order traversal?");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public String getMergedTranscript() {
        // In real implementation, this would merge partial transcriptions
        return "Suppose we have a tree, how do you do in-order traversal?";
    }
}

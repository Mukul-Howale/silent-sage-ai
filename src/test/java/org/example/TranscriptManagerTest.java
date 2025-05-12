package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicReference;

public class TranscriptManagerTest {
    private TranscriptManager transcriptManager;
    private static final String TEST_API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transcriptManager = new TranscriptManager(TEST_API_KEY);
    }

    @Test
    void testStartStopListening() {
        assertFalse(transcriptManager.isListening());
        
        transcriptManager.startListening();
        assertTrue(transcriptManager.isListening());
        
        transcriptManager.stopListening();
        assertFalse(transcriptManager.isListening());
    }

    @Test
    void testTranscriptCallback() {
        AtomicReference<String> receivedTranscript = new AtomicReference<>();
        transcriptManager.setTranscriptCallback(receivedTranscript::set);
        
        // Simulate receiving a transcript
        String testTranscript = "This is a test transcript";
        transcriptManager.getStorage().addTranscript(testTranscript);
        
        assertEquals(testTranscript, transcriptManager.getMergedTranscript());
    }
} 
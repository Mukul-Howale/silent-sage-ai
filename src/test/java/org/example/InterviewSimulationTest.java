package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

public class InterviewSimulationTest {
    private StealthChatWindow chatWindow;
    private static final String TEST_GEMINI_KEY = "test-gemini-key";
    private static final String TEST_DEEPGRAM_KEY = "test-deepgram-key";

    @BeforeEach
    void setUp() {
        chatWindow = new StealthChatWindow(TEST_GEMINI_KEY, TEST_DEEPGRAM_KEY);
    }

    @Test
    void testTechnicalInterviewScenario() {
        // Simulate interviewer asking a technical question
        String technicalQuestion = "Can you explain the concept of polymorphism in Java?";
        
        // Start transcription
        chatWindow.startTranscription();
        
        // Simulate receiving the question
        chatWindow.getTranscriptManager().getStorage().addTranscript(technicalQuestion);
        
        // Stop transcription to trigger AI response
        chatWindow.stopTranscription();
        
        // Wait for AI response
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify response contains relevant technical content
        String response = chatWindow.getLastResponse();
        assertNotNull(response);
        assertTrue(response.toLowerCase().contains("polymorphism") || 
                  response.toLowerCase().contains("java") ||
                  response.toLowerCase().contains("object-oriented"));
    }

    @Test
    void testBehavioralInterviewScenario() {
        // Simulate interviewer asking a behavioral question
        String behavioralQuestion = "Tell me about a time when you had to work under pressure.";
        
        // Start transcription
        chatWindow.startTranscription();
        
        // Simulate receiving the question
        chatWindow.getTranscriptManager().getStorage().addTranscript(behavioralQuestion);
        
        // Stop transcription to trigger AI response
        chatWindow.stopTranscription();
        
        // Wait for AI response
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify response contains relevant behavioral content
        String response = chatWindow.getLastResponse();
        assertNotNull(response);
        assertTrue(response.toLowerCase().contains("experience") || 
                  response.toLowerCase().contains("situation") ||
                  response.toLowerCase().contains("challenge"));
    }
} 
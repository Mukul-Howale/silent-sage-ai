package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class InterviewSimulationTest {
    private StealthChatWindow chatWindow;
    private static final String TEST_GEMINI_KEY = "test-gemini-key";
    private static final String TEST_DEEPGRAM_KEY = "test-deepgram-key";
    private static final int TIMEOUT_MS = 5000;
    
    @Mock
    private GPTService mockGptService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize UI components on the Event Dispatch Thread
        try {
            SwingUtilities.invokeAndWait(() -> {
                chatWindow = new StealthChatWindow(TEST_GEMINI_KEY, TEST_DEEPGRAM_KEY);
                // Replace the real GPT service with our mock
                chatWindow.setGptService(mockGptService);
                chatWindow.startChatWindow();
            });
        } catch (Exception e) {
            fail("Failed to initialize chat window: " + e.getMessage());
        }
    }

    @Test
    void testTechnicalInterviewScenario() throws Exception {
        // Simulate interviewer asking a technical question
        String technicalQuestion = "Can you explain the concept of polymorphism in Java?";
        String mockResponse = "Polymorphism is a key concept in object-oriented programming that allows objects of different classes to be treated as objects of a common superclass.";
        CountDownLatch responseLatch = new CountDownLatch(1);
        
        // Set up mock behavior
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((java.util.function.Consumer<String>) args[1]).accept(mockResponse);
            responseLatch.countDown();
            return null;
        }).when(mockGptService).requestAnswer(anyString(), any());
        
        SwingUtilities.invokeAndWait(() -> {
            // Start transcription
            chatWindow.startTranscription();
            
            // Simulate receiving the question
            chatWindow.getTranscriptManager().getStorage().addTranscript(technicalQuestion);
            
            // Stop transcription to trigger AI response
            chatWindow.stopTranscription();
        });
        
        // Wait for response with timeout
        boolean responseReceived = responseLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(responseReceived, "Response should be received within timeout");
        
        // Verify response content
        String response = chatWindow.getLastResponse();
        assertNotNull(response, "Response should not be null");
        assertTrue(response.toLowerCase().contains("polymorphism") || 
                  response.toLowerCase().contains("java") ||
                  response.toLowerCase().contains("object-oriented"),
                  "Response should contain relevant technical terms");
    }

    @Test
    void testBehavioralInterviewScenario() throws Exception {
        // Simulate interviewer asking a behavioral question
        String behavioralQuestion = "Tell me about a time when you had to work under pressure.";
        String mockResponse = "In my previous role, I faced a challenging situation when we had to deliver a critical project under tight deadlines.";
        CountDownLatch responseLatch = new CountDownLatch(1);
        
        // Set up mock behavior
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((java.util.function.Consumer<String>) args[1]).accept(mockResponse);
            responseLatch.countDown();
            return null;
        }).when(mockGptService).requestAnswer(anyString(), any());
        
        SwingUtilities.invokeAndWait(() -> {
            // Start transcription
            chatWindow.startTranscription();
            
            // Simulate receiving the question
            chatWindow.getTranscriptManager().getStorage().addTranscript(behavioralQuestion);
            
            // Stop transcription to trigger AI response
            chatWindow.stopTranscription();
        });
        
        // Wait for response with timeout
        boolean responseReceived = responseLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(responseReceived, "Response should be received within timeout");
        
        // Verify response content
        String response = chatWindow.getLastResponse();
        assertNotNull(response, "Response should not be null");
        assertTrue(response.toLowerCase().contains("experience") || 
                  response.toLowerCase().contains("situation") ||
                  response.toLowerCase().contains("challenge"),
                  "Response should contain relevant behavioral terms");
    }
} 
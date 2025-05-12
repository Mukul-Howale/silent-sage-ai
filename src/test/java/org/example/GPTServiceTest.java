package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

public class GPTServiceTest {
    private GPTService gptService;
    private static final String TEST_API_KEY = "test-api-key";
    private static final int TIMEOUT_MS = 5000; // Increased timeout to 5 seconds

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gptService = new GPTService(TEST_API_KEY);
    }

    @Test
    void testRequestAnswer() {
        AtomicReference<String> response = new AtomicReference<>();
        String testPrompt = "Tell me about your experience with Java";
        
        gptService.requestAnswer(testPrompt, response::set);
        
        // Wait for async response with timeout
        long startTime = System.currentTimeMillis();
        while (response.get() == null && System.currentTimeMillis() - startTime < TIMEOUT_MS) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted while waiting for response");
            }
        }
        
        assertNotNull(response.get(), "Response should not be null");
        assertTrue(response.get().length() > 0, "Response should not be empty");
    }
} 
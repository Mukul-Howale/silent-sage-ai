package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

public class GPTServiceTest {
    private GPTService gptService;
    private static final String TEST_API_KEY = "test-api-key";

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
        
        // Wait for async response
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertNotNull(response.get());
        assertTrue(response.get().length() > 0);
    }
} 
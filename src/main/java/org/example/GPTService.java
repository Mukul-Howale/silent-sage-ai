package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.function.Consumer;

public class GPTService {
    private final String geminiApiKey;
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public GPTService(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
        Logger.info("GPTService initialized with Gemini AI");
    }

    public void requestAnswer(String prompt, Consumer<String> callback) {
        try {
            Logger.debug("Processing Gemini request for prompt: {}", prompt);
            String finalPrompt = "You are in a job interview. Respond appropriately to the following:\n" + prompt;

            // Create request body
            String requestBody = objectMapper.writeValueAsString(new GeminiRequest(finalPrompt));
            RequestBody body = RequestBody.create(requestBody, MediaType.parse("application/json"));

            // Build request
            Request request = new Request.Builder()
                .url(GEMINI_API_URL + "?key=" + geminiApiKey)
                .post(body)
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Logger.error("Gemini API request failed", e);
                    callback.accept("Failed to get response.");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Logger.warn("Gemini API returned error response: {}", response.code());
                        callback.accept("Error: " + response.code());
                        return;
                    }
                    String responseBody = response.body().string();
                    String answer = objectMapper.readTree(responseBody)
                            .path("candidates").get(0)
                            .path("content")
                            .path("parts").get(0)
                            .path("text")
                            .asText();
                    Logger.debug("Received Gemini response successfully");
                    callback.accept(answer.trim());
                }
            });
        } catch (Exception e) {
            Logger.error("Exception occurred while processing Gemini request", e);
            callback.accept("Exception occurred.");
        }
    }

    static class GeminiRequest {
        public Content[] contents;

        public GeminiRequest(String userPrompt) {
            this.contents = new Content[]{new Content(userPrompt)};
        }

        static class Content {
            public Part[] parts;

            public Content(String text) {
                this.parts = new Part[]{new Part(text)};
            }
        }

        static class Part {
            public String text;

            public Part(String text) {
                this.text = text;
            }
        }
    }
}

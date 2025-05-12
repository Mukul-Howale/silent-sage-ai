package org.example;

import com.google.cloud.ai.generativelanguage.v1beta.GenerativeServiceClient;
import com.google.cloud.ai.generativelanguage.v1beta.GenerateContentRequest;
import com.google.cloud.ai.generativelanguage.v1beta.GenerateContentResponse;
import com.google.cloud.ai.generativelanguage.v1beta.Content;
import com.google.cloud.ai.generativelanguage.v1beta.Part;
import com.google.cloud.ai.generativelanguage.v1beta.ModelName;

import java.io.IOException;
import java.util.function.Consumer;

public class GPTService {
    private final String geminiApiKey;
    private final GenerativeServiceClient client;

    public GPTService(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
        try {
            this.client = GenerativeServiceClient.create();
            Logger.info("GPTService initialized with Gemini AI");
        } catch (IOException e) {
            Logger.error("Failed to initialize Gemini AI client", e);
            throw new RuntimeException("Failed to initialize Gemini AI client", e);
        }
    }

    public void requestAnswer(String prompt, Consumer<String> callback) {
        try {
            Logger.debug("Processing Gemini request for prompt: {}", prompt);
            String finalPrompt = "You are in a job interview. Respond appropriately to the following:\n" + prompt;

            Content content = Content.newBuilder()
                .addParts(Part.newBuilder().setText(finalPrompt).build())
                .build();

            GenerateContentRequest request = GenerateContentRequest.newBuilder()
                .setModel(ModelName.of("gemini-pro").toString())
                .setContents(content)
                .build();

            GenerateContentResponse response = client.generateContent(request);
            String answer = response.getCandidates(0).getContent().getParts(0).getText();
            
            Logger.debug("Received Gemini response successfully");
            callback.accept(answer.trim());
        } catch (Exception e) {
            Logger.error("Exception occurred while processing Gemini request", e);
            callback.accept("Exception occurred.");
        }
    }
}

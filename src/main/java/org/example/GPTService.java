package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.function.Consumer;

public class GPTService {
    private final String openaiApiKey;
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GPTService(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public void requestAnswer(String prompt, Consumer<String> callback) {
        try {
            String finalPrompt = "You are in a job interview. Respond appropriately to the following:\n" + prompt;

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(new ChatRequest(finalPrompt)),
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + openaiApiKey)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.accept("Failed to get response.");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.accept("Error: " + response.code());
                        return;
                    }
                    String responseBody = response.body().string();
                    String answer = objectMapper.readTree(responseBody)
                            .path("choices").get(0)
                            .path("message")
                            .path("content")
                            .asText();
                    callback.accept(answer.trim());
                }
            });
        } catch (Exception e) {
            callback.accept("Exception occurred.");
        }
    }

    static class ChatRequest {
        public String model = "gpt-3.5-turbo";
        public Message[] messages;

        public ChatRequest(String userPrompt) {
            this.messages = new Message[]{new Message("user", userPrompt)};
        }

        static class Message {
            public String role;
            public String content;

            public Message(String role, String content) {
                this.role = role;
                this.content = content;
            }
        }
    }
}

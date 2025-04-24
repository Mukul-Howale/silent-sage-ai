package org.example;

import io.github.cdimascio.dotenv.Dotenv;

public class StealthAppLauncher {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String openaiApiKey = dotenv.get("OPENAI_API_KEY");
        String deepgramApiKey = dotenv.get("DEEPGRAM_API_KEY");

        new StealthChatWindow(openaiApiKey, deepgramApiKey);
    }
}
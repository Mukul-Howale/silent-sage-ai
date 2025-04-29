package org.example;

import io.github.cdimascio.dotenv.Dotenv;

public class StealthAppLauncher {
    public static void main(String[] args) {
        Logger.info("Starting Silent Sage AI application");
        
        try {
            Dotenv dotenv = Dotenv.load();
            String openaiApiKey = dotenv.get("OPENAI_API_KEY");
            String deepgramApiKey = dotenv.get("DEEPGRAM_API_KEY");

            Logger.debug("API keys loaded successfully");
            
            StealthChatWindow chatWindow = new StealthChatWindow(openaiApiKey, deepgramApiKey);
            chatWindow.startChatWindow();
            
            Logger.info("Chat window started successfully");
        } catch (Exception e) {
            Logger.error("Failed to start application", e);
            System.exit(1);
        }
    }
}
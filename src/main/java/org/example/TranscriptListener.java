package org.example;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import javax.sound.sampled.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class TranscriptListener {
    private static final String DEEPGRAM_WS_URL = "wss://api.deepgram.com/v1/listen";
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    private final String deepgramApiKey;
    private Consumer<String> transcriptCallback;
    private WebSocketClient webSocketClient;
    private TargetDataLine audioLine;
    private boolean isListening = false;
    private Thread audioThread;

    public TranscriptListener(String deepgramApiKey) {
        this.deepgramApiKey = deepgramApiKey;
        Logger.info("TranscriptListener initialized");
    }

    public void setTranscriptCallback(Consumer<String> callback) {
        this.transcriptCallback = callback;
        Logger.debug("Transcript callback set");
    }

    public void startListening() {
        if (isListening) {
            Logger.warn("Attempted to start listening while already listening");
            return;
        }
        
        Logger.info("Starting transcript listener : {TranscriptListener}");
        setupWebSocket();
        setupAudioCapture();
        isListening = true;
    }

    public void stopListening() {
        if (!isListening) {
            Logger.warn("Attempted to stop listening while not listening");
            return;
        }
        
        Logger.info("Stopping transcript listener : {TranscriptListener}");
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
        }
        if (audioThread != null) {
            audioThread.interrupt();
        }
        isListening = false;
    }

    public boolean isListening() {
        return isListening;
    }

    private void setupWebSocket() {
        webSocketClient = new WebSocketClient(URI.create(DEEPGRAM_WS_URL + "?encoding=linear16&sample_rate=" + SAMPLE_RATE + "&diarize=true")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Logger.info("Connected to Deepgram WebSocket");
            }

            @Override
            public void onMessage(String message) {
                try {
                    JSONObject response = new JSONObject(message);
                    if (response.has("channel") && response.getJSONObject("channel").has("alternatives")) {
                        JSONObject channel = response.getJSONObject("channel");
                        JSONObject alternative = channel.getJSONArray("alternatives").getJSONObject(0);
                        
                        if (alternative.has("transcript") && !alternative.getString("transcript").isEmpty()) {
                            if (alternative.has("words")) {
                                JSONObject firstWord = alternative.getJSONArray("words").getJSONObject(0);
                                if (firstWord.has("speaker") && firstWord.getInt("speaker") == 0) {
                                    String transcript = alternative.getString("transcript");
                                    Logger.debug("Received transcript: {}", transcript);
                                    if (transcriptCallback != null) {
                                        transcriptCallback.accept(transcript);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.error("Error processing WebSocket message", e);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Logger.info("WebSocket connection closed: {} (code: {})", reason, code);
            }

            @Override
            public void onError(Exception ex) {
                Logger.error("WebSocket error occurred", ex);
            }
        };

        webSocketClient.addHeader("Authorization", "Token " + deepgramApiKey);
        webSocketClient.connect();
    }

    private void setupAudioCapture() {
        try {
            Logger.debug("Setting up audio capture");
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Line not supported");
            }

            audioLine = (TargetDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
            audioLine.start();

            audioThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (!Thread.currentThread().isInterrupted() && webSocketClient.isOpen()) {
                    int count = audioLine.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        webSocketClient.send(ByteBuffer.wrap(buffer, 0, count));
                    }
                }
            });
            audioThread.start();
            Logger.info("Audio capture setup completed successfully");
        } catch (Exception e) {
            Logger.error("Error setting up audio capture", e);
        }
    }

    public String getMergedTranscript() {
        return "";
    }
}

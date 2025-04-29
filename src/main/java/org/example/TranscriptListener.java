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
    }

    public void setTranscriptCallback(Consumer<String> callback) {
        this.transcriptCallback = callback;
    }

    public void startListening() {
        if (isListening) return;
        
        setupWebSocket();
        setupAudioCapture();
        isListening = true;
    }

    public void stopListening() {
        if (!isListening) return;
        
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
                System.out.println("🎙 Connected to Deepgram");
            }

            @Override
            public void onMessage(String message) {
                JSONObject response = new JSONObject(message);
                if (response.has("channel") && response.getJSONObject("channel").has("alternatives")) {
                    JSONObject channel = response.getJSONObject("channel");
                    JSONObject alternative = channel.getJSONArray("alternatives").getJSONObject(0);
                    
                    if (alternative.has("transcript") && !alternative.getString("transcript").isEmpty()) {
                        // Check if the speaker is the interviewer (speaker 0)
                        if (alternative.has("words")) {
                            JSONObject firstWord = alternative.getJSONArray("words").getJSONObject(0);
                            if (firstWord.has("speaker") && firstWord.getInt("speaker") == 0) {
                                String transcript = alternative.getString("transcript");
                                if (transcriptCallback != null) {
                                    transcriptCallback.accept(transcript);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("Connection closed: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("WebSocket error: " + ex.getMessage());
            }
        };

        webSocketClient.addHeader("Authorization", "Token " + deepgramApiKey);
        webSocketClient.connect();
    }

    private void setupAudioCapture() {
        try {
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
        } catch (Exception e) {
            System.err.println("Error setting up audio capture: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getMergedTranscript() {
        return "";
    }
}

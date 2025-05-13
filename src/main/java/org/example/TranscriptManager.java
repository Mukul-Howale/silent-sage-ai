package org.example;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import javax.sound.sampled.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.io.IOException;

public class TranscriptManager {
    private static final String DEEPGRAM_WS_URL = "wss://api.deepgram.com/v1/listen";
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    private final String deepgramApiKey;
    private final TranscriptStorage storage;
    private Consumer<String> transcriptCallback;
    private WebSocketClient webSocketClient;
    private TargetDataLine audioLine;
    private boolean isListening = false;
    private Thread audioThread;

    public TranscriptManager(String deepgramApiKey) {
        this.deepgramApiKey = deepgramApiKey;
        this.storage = new TranscriptStorage();
        Logger.info("TranscriptManager initialized");
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
        
        Logger.info("Starting transcript manager");
        setupWebSocket();
        setupAudioCapture();
        isListening = true;
    }

    public void stopListening() {
        if (!isListening) {
            Logger.warn("Attempted to stop listening while not listening");
            return;
        }
        
        Logger.info("Stopping transcript manager");
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

    public String getMergedTranscript() {
        return storage.getMergedTranscript();
    }

    public TranscriptStorage getStorage() {
        return storage;
    }

    private void setupWebSocket() {
        webSocketClient = new WebSocketClient(URI.create(DEEPGRAM_WS_URL + "?encoding=linear16&sample_rate=" + SAMPLE_RATE + "&diarize=true")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Logger.info("Connected to Deepgram WebSocket with headers: {}", handshakedata.getHttpStatusMessage());
            }

            @Override
            public void onMessage(String message) {
                try {
                    Logger.debug("Raw WebSocket message received: {}", message);
                    JSONObject response = new JSONObject(message);
                    if (response.has("channel") && response.getJSONObject("channel").has("alternatives")) {
                        JSONObject channel = response.getJSONObject("channel");
                        JSONObject alternative = channel.getJSONArray("alternatives").getJSONObject(0);
                        
                        if (alternative.has("transcript") && !alternative.getString("transcript").isEmpty()) {
                            if (alternative.has("words")) {
                                JSONObject firstWord = alternative.getJSONArray("words").getJSONObject(0);
                                if (firstWord.has("speaker")) {
                                    Logger.debug("Speaker detected: {}", firstWord.getInt("speaker"));
                                    if (firstWord.getInt("speaker") == 0) {
                                        String transcript = alternative.getString("transcript");
                                        Logger.debug("Received transcript from speaker 0: {}", transcript);
                                        storage.addTranscript(transcript);
                                        if (transcriptCallback != null) {
                                            transcriptCallback.accept(transcript);
                                        }
                                    }
                                } else {
                                    Logger.debug("No speaker information in response");
                                }
                            }
                        } else {
                            Logger.debug("Empty transcript received");
                        }
                    } else {
                        Logger.debug("Response missing channel or alternatives");
                    }
                } catch (Exception e) {
                    Logger.error("Error processing WebSocket message: {}", e.getMessage(), e);
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
            Logger.debug("Setting up audio capture with format: {}Hz, {}bit, {} channels", SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS);
            AudioFormat targetFormat = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
            
            Logger.debug("Getting available audio lines");
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            Logger.info("Found {} audio mixers", mixers.length);
            
            Mixer selectedMixer = null;
            AudioFormat nativeFormat = null;
            
            // First try to find system audio capture devices
            String[] systemAudioDevices = {"Stereo Mix", "What U Hear", "What You Hear", "Wave Out Mix", "Mixage stéréo"};
            for (String deviceName : systemAudioDevices) {
                for (Mixer.Info mixerInfo : mixers) {
                    if (mixerInfo.getName().contains(deviceName)) {
                        selectedMixer = AudioSystem.getMixer(mixerInfo);
                        Logger.info("Selected system audio device: {}", mixerInfo.getName());
                        nativeFormat = getAudioFormat(selectedMixer, nativeFormat);
                        if (nativeFormat != null) {
                            break;
                        }
                    }
                }
                if (selectedMixer != null && nativeFormat != null) {
                    break;
                }
            }
            
            // If no system audio device found, try Camo microphone
            if (selectedMixer == null || nativeFormat == null) {
                for (Mixer.Info mixerInfo : mixers) {
                    if (mixerInfo.getName().contains("Camo")) {
                        selectedMixer = AudioSystem.getMixer(mixerInfo);
                        Logger.info("Selected Camo microphone: {}", mixerInfo.getName());
                        nativeFormat = getAudioFormat(selectedMixer, nativeFormat);
                        if (nativeFormat != null) {
                            break;
                        }
                    }
                }
            }
            
            // If still no device found, try Realtek microphone
            if (selectedMixer == null || nativeFormat == null) {
                for (Mixer.Info mixerInfo : mixers) {
                    if (mixerInfo.getName().contains("Realtek") && mixerInfo.getName().contains("Microphone")) {
                        selectedMixer = AudioSystem.getMixer(mixerInfo);
                        Logger.info("Selected Realtek microphone: {}", mixerInfo.getName());
                        nativeFormat = getAudioFormat(selectedMixer, nativeFormat);
                        if (nativeFormat != null) {
                            break;
                        }
                    }
                }
            }
            
            // If still no device found, try any microphone
            if (selectedMixer == null || nativeFormat == null) {
                for (Mixer.Info mixerInfo : mixers) {
                    if (mixerInfo.getName().contains("Microphone")) {
                        selectedMixer = AudioSystem.getMixer(mixerInfo);
                        Logger.info("Selected generic microphone: {}", mixerInfo.getName());
                        nativeFormat = getAudioFormat(selectedMixer, nativeFormat);
                        if (nativeFormat != null) {
                            break;
                        }
                    }
                }
            }

            if (selectedMixer == null || nativeFormat == null) {
                Logger.error("No suitable mixer or format found for audio capture");
                throw new LineUnavailableException("No suitable mixer or format found");
            }

            // Create a line with the native format
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, nativeFormat);
            audioLine = (TargetDataLine) selectedMixer.getLine(info);
            Logger.info("Audio line obtained: {} with format: {}", audioLine.getLineInfo(), audioLine.getFormat());
            
            // Set a larger buffer size
            int bufferSize = 16384;
            audioLine.open(nativeFormat, bufferSize);
            audioLine.start();
            Logger.info("Audio line started with format: {} and buffer size: {}", audioLine.getFormat(), bufferSize);

            // Create an audio converter
            AudioInputStream audioInputStream = new AudioInputStream(audioLine);
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            Logger.info("Audio conversion stream created from {} to {}", nativeFormat, targetFormat);

            audioThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                int totalBytesRead = 0;
                int consecutiveEmptyReads = 0;
                long lastDataTime = System.currentTimeMillis();
                boolean hasReceivedData = false;
                
                while (!Thread.currentThread().isInterrupted() && webSocketClient.isOpen()) {
                    try {
                        int count = convertedStream.read(buffer, 0, buffer.length);
                        if (count > 0) {
                            totalBytesRead += count;
                            consecutiveEmptyReads = 0;
                            lastDataTime = System.currentTimeMillis();
                            
                            // Check if the buffer contains non-zero data
                            boolean hasNonZeroData = false;
                            for (int i = 0; i < count; i++) {
                                if (buffer[i] != 0) {
                                    hasNonZeroData = true;
                                    break;
                                }
                            }
                            
                            if (hasNonZeroData) {
                                if (!hasReceivedData) {
                                    Logger.info("First audio data received");
                                    hasReceivedData = true;
                                }
                                Logger.debug("Read {} bytes of audio data, total: {}", count, totalBytesRead);
                                webSocketClient.send(ByteBuffer.wrap(buffer, 0, count));
                            } else {
                                Logger.debug("Skipping silent audio data");
                            }
                        } else {
                            consecutiveEmptyReads++;
                            if (consecutiveEmptyReads % 10 == 0) {
                                Logger.debug("No audio data read for {} consecutive attempts", consecutiveEmptyReads);
                            }
                            
                            // Log if no data for more than 5 seconds
                            if (System.currentTimeMillis() - lastDataTime > 5000) {
                                Logger.warn("No audio data received for more than 5 seconds");
                                lastDataTime = System.currentTimeMillis();
                            }
                        }
                    } catch (IOException e) {
                        Logger.error("Error reading from audio stream: {}", e.getMessage(), e);
                        break;
                    }
                }
                
                try {
                    convertedStream.close();
                    audioInputStream.close();
                } catch (IOException e) {
                    Logger.error("Error closing audio streams: {}", e.getMessage(), e);
                }
            });
            audioThread.start();
            Logger.info("Audio capture setup completed successfully");
        } catch (Exception e) {
            Logger.error("Error setting up audio capture: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to setup audio capture", e);
        }
    }

    private AudioFormat getAudioFormat(Mixer selectedMixer, AudioFormat nativeFormat) {
        Line.Info[] lineInfos = selectedMixer.getSourceLineInfo();
        for (Line.Info lineInfo : lineInfos) {
            if (lineInfo instanceof DataLine.Info) {
                AudioFormat[] formats = ((DataLine.Info) lineInfo).getFormats();
                if (formats.length > 0) {
                    nativeFormat = formats[0];
                    Logger.debug("Native format: {}", nativeFormat);
                    break;
                }
            }
        }
        return nativeFormat;
    }
} 
package org.example;

import javax.swing.*;
import java.awt.*;

public class StealthChatWindow extends JFrame {
    private TranscriptListener transcriptListener = null;
    private GPTService gptService = null;
    private final JTextArea chatArea;

    public StealthChatWindow(String openaiApiKey, String deepgramApiKey) {
        super("SilentSage AI");
        setUndecorated(true);
        setSize(300, 200);
        setAlwaysOnTop(true);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(Color.BLACK);
        chatArea.setForeground(Color.GREEN);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JLabel listeningIndicator = new JLabel("Listening 🔴", SwingConstants.CENTER);
        listeningIndicator.setForeground(Color.RED);
        add(listeningIndicator, BorderLayout.NORTH);

        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> gptService.requestAnswer(transcriptListener.getMergedTranscript(), this::updateChatArea));
        add(refreshButton, BorderLayout.SOUTH);

        transcriptListener = new TranscriptListener(deepgramApiKey);
        gptService = new GPTService(openaiApiKey);

        transcriptListener.setTranscriptCallback(transcript -> {
            gptService.requestAnswer(transcript, this::updateChatArea);
        });

        transcriptListener.startListening();
    }

    private void updateChatArea(String answer) {
        chatArea.append("\n\nAssistant: " + answer);
    }
}

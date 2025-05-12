package org.example;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class StealthChatWindow {
    private TranscriptListener transcriptListener = null;
    private GPTService gptService = null;
    private final JTextArea chatArea;
    private final JFrame frame;
    private JButton toggleButton;
    private JLabel statusLabel;

    private final String geminiApiKey;
    private final String deepgramApiKey;

    public StealthChatWindow(String geminiApiKey, String deepgramApiKey) {
        this.geminiApiKey = geminiApiKey;
        this.deepgramApiKey = deepgramApiKey;
        chatArea = new JTextArea();
        frame = new JFrame();
        Logger.info("StealthChatWindow initialized");
    }

    public void startChatWindow() {
        Logger.info("Starting chat window");
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setSize(500, 500);
        frame.setLocation(100, 100);
        frame.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel();
        panel.setBackground(new Color(0, 0, 0, 200));
        panel.setLayout(new BorderLayout(5, 5));
        frame.setContentPane(panel);

        // Status label
        statusLabel = new JLabel("Not Listening ⚪");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(statusLabel, BorderLayout.NORTH);

        // Chat area
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(0, 0, 0, 200));
        chatArea.setForeground(Color.GREEN);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBackground(new Color(0, 0, 0, 200));

        // Toggle button
        toggleButton = new JButton("🎤 Start Listening");
        toggleButton.addActionListener(e -> toggleListening());
        buttonPanel.add(toggleButton);

        // Refresh button
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> {
            Logger.debug("Refresh button clicked");
            gptService.requestAnswer(transcriptListener.getMergedTranscript(), this::updateChatArea);
        });
        buttonPanel.add(refreshButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        makeDraggable(frame, panel);
        frame.setVisible(true);

        Timer timer = new Timer(300, e -> makeWindowStealthy(frame));
        timer.setRepeats(false);
        timer.start();

        transcriptListener = new TranscriptListener(deepgramApiKey);
        gptService = new GPTService(geminiApiKey);

        transcriptListener.setTranscriptCallback(transcript -> {
            Logger.debug("Received transcript callback: {}", transcript);
            gptService.requestAnswer(transcript, this::updateChatArea);
        });
    }

    private void toggleListening() {
        if (transcriptListener.isListening()) {
            Logger.info("Stopping transcript listener : {StealthChatWindow}");
            transcriptListener.stopListening();
            toggleButton.setText("🎤 Start Listening");
            statusLabel.setText("Not Listening ⚪");
            statusLabel.setForeground(Color.WHITE);
        } else {
            Logger.info("Starting transcript listener : {StealthChatWindow}");
            transcriptListener.startListening();
            toggleButton.setText("⏹ Stop Listening");
            statusLabel.setText("Listening 🔴");
            statusLabel.setForeground(Color.RED);
        }
    }

    private void makeDraggable(JFrame frame, Component dragComponent) {
        final Point[] initialClick = {null};

        dragComponent.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick[0] = e.getPoint();
            }
        });
        dragComponent.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (initialClick[0] != null) {
                    int xMoved = e.getX() - initialClick[0].x;
                    int yMoved = e.getY() - initialClick[0].y;

                    Point frameLocation = frame.getLocation();
                    frame.setLocation(frameLocation.x + xMoved, frameLocation.y + yMoved);
                }
            }
        });
    }

    private void updateChatArea(String answer) {
        Logger.debug("Updating chat area with answer: {}", answer);
        chatArea.append("\n\nAssistant: " + answer);
    }

    private void makeWindowStealthy(JFrame frame) {
        Logger.debug("Making window stealthy");
        WinDef.HWND hwnd = getHWnd(frame);

        int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
        exStyle |= WinUser.WS_EX_LAYERED;
        User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle);

        User32.INSTANCE.SetLayeredWindowAttributes(hwnd, 0, (byte) 230, WinUser.LWA_ALPHA);

        boolean result = ExtendedUser32.INSTANCE.SetWindowDisplayAffinity(hwnd, ExtendedUser32.WDA_EXCLUDEFROMCAPTURE);
        Logger.debug("SetWindowDisplayAffinity result: {}", result);
    }

    private WinDef.HWND getHWnd(JFrame frame) {
        if (!frame.isDisplayable()) {
            Logger.error("Frame not displayable yet");
            return null;
        }
        WinDef.HWND hwnd = new WinDef.HWND();
        hwnd.setPointer(Native.getComponentPointer(frame));
        return hwnd;
    }
}

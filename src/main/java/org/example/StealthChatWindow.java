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

    private final String openaiApiKey;
    private final String deepgramApiKey;

    public StealthChatWindow(String openaiApiKey, String deepgramApiKey) {
        this.openaiApiKey = openaiApiKey;
        this.deepgramApiKey = deepgramApiKey;
        chatArea = new JTextArea();
        frame = new JFrame();
    }

    public void startChatWindow() {
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setSize(300, 160); // Smaller size for stealth
        frame.setLocation(100, 100);
        frame.setBackground(new Color(0, 0, 0, 0)); // Transparent window

        // Use JPanel as main content with custom background
        JPanel panel = new JPanel();
        panel.setBackground(new Color(0, 0, 0, 200)); // Increased opacity for better visibility
        panel.setLayout(new BorderLayout(5, 5));
        frame.setContentPane(panel);

        // Listening label
        JLabel statusLabel = new JLabel("Listening 🔴");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(statusLabel, BorderLayout.NORTH);

        // Chat area
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(0, 0, 0, 200)); // Match panel background
        chatArea.setForeground(Color.GREEN);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> gptService.requestAnswer(transcriptListener.getMergedTranscript(), this::updateChatArea));
        panel.add(refreshButton, BorderLayout.SOUTH);

        // Add draggable behavior to the entire panel
        makeDraggable(frame, panel);

        // Show the window
        frame.setVisible(true);

        // Stealth mode (slight delay)
        Timer timer = new Timer(300, e -> makeWindowStealthy(frame));
        timer.setRepeats(false);
        timer.start();

        // Start services
        transcriptListener = new TranscriptListener(deepgramApiKey);
        gptService = new GPTService(openaiApiKey);

        transcriptListener.setTranscriptCallback(transcript -> {
            gptService.requestAnswer(transcript, this::updateChatArea);
        });

        transcriptListener.startListening();
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
        chatArea.append("\n\nAssistant: " + answer);
    }

    private void makeWindowStealthy(JFrame frame) {
        WinDef.HWND hwnd = getHWnd(frame);

        int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
        exStyle |= WinUser.WS_EX_LAYERED;
        User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle);

        // Set opacity to 90% for better visibility while maintaining stealth
        User32.INSTANCE.SetLayeredWindowAttributes(hwnd, 0, (byte) 230, WinUser.LWA_ALPHA);

        // Hide from full-screen screen sharing
        boolean result = ExtendedUser32.INSTANCE.SetWindowDisplayAffinity(hwnd, ExtendedUser32.WDA_EXCLUDEFROMCAPTURE);
        System.out.println("SetWindowDisplayAffinity success? " + result);
    }

    private WinDef.HWND getHWnd(JFrame frame) {
        if (!frame.isDisplayable()) {
            System.err.println("Frame not displayable yet.");
            return null;
        }
        WinDef.HWND hwnd = new WinDef.HWND();
        hwnd.setPointer(Native.getComponentPointer(frame));
        return hwnd;
    }
}

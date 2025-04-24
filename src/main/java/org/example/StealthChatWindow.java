package org.example;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import javax.swing.*;
import java.awt.*;

public class StealthChatWindow {
    private TranscriptListener transcriptListener = null;
    private GPTService gptService = null;
    private final JTextArea chatArea;

    public StealthChatWindow(String openaiApiKey, String deepgramApiKey) {
        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setSize(300, 200);
        frame.setLocationRelativeTo(null);
        frame.setLocation(100, 100);
        frame.setBackground(new Color(0, 0, 0, 0));
        frame.setLayout(new FlowLayout());
        frame.setVisible(true);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(Color.BLACK);
        chatArea.setForeground(Color.GREEN);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JLabel listeningIndicator = new JLabel("Listening 🔴", SwingConstants.CENTER);
        listeningIndicator.setForeground(Color.RED);
        frame.add(listeningIndicator, BorderLayout.NORTH);

        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> gptService.requestAnswer(transcriptListener.getMergedTranscript(), this::updateChatArea));
        frame.add(refreshButton, BorderLayout.SOUTH);

        // ⚠️ Delay stealthing to make sure native peer is created
        Timer timer = new Timer(300, e -> makeWindowStealthy(frame));
        timer.setRepeats(false);
        timer.start();

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

    private void makeWindowStealthy(JFrame frame) {
        WinDef.HWND hwnd = getHWnd(frame);

        int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
        exStyle |= WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
        User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle);

        // Optional: adjust opacity (0-255)
        User32.INSTANCE.SetLayeredWindowAttributes(hwnd, 0, (byte) 255, WinUser.LWA_ALPHA);

        // ❗ Hide from full-screen screen sharing
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

package com.yuemo.demo.channel.swing.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class ChatPanel {

    private final JPanel panel;
    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private final Consumer<String> onSendMessage;

    public ChatPanel(Consumer<String> onSendMessage) {
        this.onSendMessage = onSendMessage;
        this.panel = createPanel();
        this.chatArea = createChatArea();
        this.inputField = createInputField();
        this.sendButton = createSendButton();
        layoutComponents();
        showWelcomeMessage();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void appendMessage(String sender, String content, MessageSource source) {
        SwingUtilities.invokeLater(() -> {
            chatArea.setForeground(source.getColor());
            chatArea.append(String.format("[%s] %s\n\n", sender, content));
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void setInputEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            sendButton.setEnabled(enabled);
            inputField.setEnabled(enabled);
            if (enabled) {
                inputField.requestFocus();
            }
        });
    }

    private JPanel createPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        return p;
    }

    private JTextArea createChatArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(new Color(245, 245, 245));
        return area;
    }

    private JTextField createInputField() {
        JTextField field = new JTextField();
        field.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        field.addActionListener((ActionEvent e) -> handleSend());
        return field;
    }

    private JButton createSendButton() {
        JButton button = new JButton("发送");
        button.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.addActionListener((ActionEvent e) -> handleSend());
        return button;
    }

    private void layoutComponents() {
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);
    }

    private void showWelcomeMessage() {
        appendMessage("系统", "欢迎使用多智能体聊天系统！\n\n" +
                "我可以帮您：\n" +
                "  • 查询天气 - 例如：北京的天气怎么样？\n" +
                "  • 进行计算 - 例如：256 * 12 等于多少？\n" +
                "  • 查询时间 - 例如：现在几点了？\n" +
                "  • 管理子智能体 - 例如：创建一个天气专家智能体\n\n" +
                "请输入您的问题...", MessageSource.SYSTEM);
    }

    private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        appendMessage("你", text, MessageSource.USER);
        inputField.setText("");

        if (onSendMessage != null) {
            onSendMessage.accept(text);
        }
    }

    public enum MessageSource {
        SYSTEM(Color.BLUE),
        USER(Color.BLACK),
        ASSISTANT(new Color(139, 69, 19)),
        FEISHU(new Color(0, 128, 0)),
        DINGTALK(new Color(0, 0, 200));

        private final Color color;

        MessageSource(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }
}
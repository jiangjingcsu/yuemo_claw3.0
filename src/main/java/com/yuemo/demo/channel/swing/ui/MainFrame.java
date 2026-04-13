package com.yuemo.demo.channel.swing.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame {

    private static final String TITLE = "多智能体聊天系统 - Spring AI Alibaba";
    private static final int DEFAULT_WIDTH = 900;
    private static final int DEFAULT_HEIGHT = 700;

    private JFrame frame;
    private final ChatPanel chatPanel;
    private final ConfigPanel configPanel;
    private final ConnectionStatusPanel connectionStatusPanel;
    private final PromptPanel promptPanel;

    public MainFrame(ChatPanel chatPanel, ConfigPanel configPanel,
                     ConnectionStatusPanel connectionStatusPanel, PromptPanel promptPanel) {
        this.chatPanel = chatPanel;
        this.configPanel = configPanel;
        this.connectionStatusPanel = connectionStatusPanel;
        this.promptPanel = promptPanel;
    }

    public void show(String channelType) {
        frame = new JFrame(TITLE + " - " + channelType);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        frame.add(createTopPanel(channelType), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        tabbedPane.addTab("聊天", chatPanel.getPanel());
        tabbedPane.addTab("系统提示词", promptPanel.getPanel());
        tabbedPane.addTab("配置管理", configPanel.getPanel());
        tabbedPane.addTab("连接状态", connectionStatusPanel.getPanel());
        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public ConfigPanel getConfigPanel() {
        return configPanel;
    }

    public ConnectionStatusPanel getConnectionStatusPanel() {
        return connectionStatusPanel;
    }

    private JPanel createTopPanel(String channelType) {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JLabel titleLabel = new JLabel("多智能体聊天系统 - " + channelType, SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        return topPanel;
    }
}
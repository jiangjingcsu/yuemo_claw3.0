package com.yuemo.demo.channel.swing;

import com.yuemo.demo.common.config.ConfigManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 主窗口框架
 *
 * <p>组装所有子面板（聊天、配置、连接状态、提示词管理）到主窗口中。
 * 作为 Swing 界面的顶层容器，负责窗口生命周期管理。</p>
 */
public class MainFrame {

    private static final String TITLE = "多智能体聊天系统 - Spring AI Alibaba";
    private static final int DEFAULT_WIDTH = 900;
    private static final int DEFAULT_HEIGHT = 700;

    private JFrame frame;
    private final ChatPanel chatPanel;
    private final ConfigPanel configPanel;
    private final ConnectionStatusPanel connectionStatusPanel;
    private final PromptPanel promptPanel;

    /**
     * 构造方法
     *
     * @param chatPanel              聊天面板
     * @param configPanel            配置管理面板
     * @param connectionStatusPanel  连接状态面板
     * @param promptPanel            提示词管理面板
     */
    public MainFrame(ChatPanel chatPanel, ConfigPanel configPanel,
                     ConnectionStatusPanel connectionStatusPanel, PromptPanel promptPanel) {
        this.chatPanel = chatPanel;
        this.configPanel = configPanel;
        this.connectionStatusPanel = connectionStatusPanel;
        this.promptPanel = promptPanel;
    }

    /**
     * 初始化并显示主窗口
     *
     * @param channelType 通道类型，显示在标题栏
     */
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

    /**
     * 关闭主窗口
     */
    public void dispose() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    /**
     * 获取聊天面板
     *
     * @return ChatPanel 实例
     */
    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    /**
     * 获取配置面板
     *
     * @return ConfigPanel 实例
     */
    public ConfigPanel getConfigPanel() {
        return configPanel;
    }

    /**
     * 获取连接状态面板
     *
     * @return ConnectionStatusPanel 实例
     */
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

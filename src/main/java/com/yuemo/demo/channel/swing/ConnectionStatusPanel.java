package com.yuemo.demo.channel.swing;

import com.yuemo.demo.channel.Channel;
import com.yuemo.demo.channel.ChannelType;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接状态面板
 *
 * <p>显示所有通道的连接状态，支持实时刷新。
 * 通过 {@link ChannelStatusProvider} 接口查询各通道状态，
 * 不直接依赖具体通道实现。</p>
 */
@Slf4j
public class ConnectionStatusPanel {

    private final JPanel panel;
    private final Map<String, JLabel> statusLabels = new ConcurrentHashMap<>();
    private List<ChannelStatusProvider> statusProviders;

    /**
     * 构造方法
     */
    public ConnectionStatusPanel() {
        this.panel = createPanel();
    }

    /**
     * 获取面板组件
     *
     * @return JPanel 实例
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * 设置通道状态提供者列表
     *
     * @param providers 状态提供者列表
     */
    public void setStatusProviders(List<ChannelStatusProvider> providers) {
        this.statusProviders = providers;
        rebuildStatusPanel();
    }

    /**
     * 刷新所有通道状态显示
     */
    public void refreshStatus() {
        if (statusProviders == null) {
            return;
        }

        for (ChannelStatusProvider provider : statusProviders) {
            JLabel label = statusLabels.get(provider.getChannelName());
            if (label != null) {
                updateStatusLabel(label, provider);
            }
        }
    }

    private JPanel createPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return p;
    }

    private void rebuildStatusPanel() {
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;

        JLabel titleLabel = new JLabel("通道连接状态", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        infoPanel.add(titleLabel, gbc);
        row++;

        gbc.gridwidth = 1;

        for (ChannelStatusProvider provider : statusProviders) {
            JLabel nameLabel = new JLabel(provider.getChannelName() + ":");
            nameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            infoPanel.add(nameLabel, gbc);

            JLabel statusLabel = new JLabel("检测中...");
            statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
            statusLabel.setForeground(Color.GRAY);
            gbc.gridx = 1;
            gbc.gridy = row;
            gbc.weightx = 1;
            infoPanel.add(statusLabel, gbc);

            statusLabels.put(provider.getChannelName(), statusLabel);
            row++;
        }

        JButton refreshButton = new JButton("刷新状态");
        refreshButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        refreshButton.setBackground(new Color(70, 130, 180));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.addActionListener(e -> refreshStatus());
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        infoPanel.add(refreshButton, gbc);
        row++;

        String noteHtml = "<html><b>飞书长连接模式说明：</b><br>" +
                "• 无需公网 IP 或域名，无需内网穿透<br>" +
                "• 本地开发环境即可接收事件<br>" +
                "• 内置加密和鉴权，无需额外处理<br><br>" +
                "<b>飞书开放平台配置步骤：</b><br>" +
                "1. 访问飞书开放平台: https://open.feishu.cn<br>" +
                "2. 进入应用管理 → 选择你的应用<br>" +
                "3. 事件与回调 → 事件配置<br>" +
                "4. 编辑订阅方式 → 选择「使用长连接接收事件」<br>" +
                "5. 添加事件 → 搜索并添加「接收消息」(im.message.receive_v1)<br>" +
                "6. 保存并发布应用<br>" +
                "7. 点击上方「刷新状态」按钮确认连接成功</html>";

        JLabel noteLabel = new JLabel(noteHtml);
        noteLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        noteLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        infoPanel.add(noteLabel, gbc);

        panel.add(new JScrollPane(infoPanel), BorderLayout.CENTER);
    }

    private void updateStatusLabel(JLabel label, ChannelStatusProvider provider) {
        SwingUtilities.invokeLater(() -> {
            try {
                boolean connected = provider.isConnected();
                if (connected) {
                    label.setText("已连接");
                    label.setForeground(new Color(0, 153, 0));
                } else {
                    label.setText("未连接 - 请检查配置");
                    label.setForeground(Color.RED);
                }
            } catch (Exception e) {
                label.setText("检测失败 - " + e.getMessage());
                label.setForeground(Color.ORANGE);
            }
        });
    }
}

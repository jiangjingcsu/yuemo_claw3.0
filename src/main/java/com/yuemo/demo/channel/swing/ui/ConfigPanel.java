package com.yuemo.demo.channel.swing.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuemo.demo.common.config.ConfigManager;
import com.yuemo.demo.channel.dingtalk.config.DingTalkConfig;
import com.yuemo.demo.channel.feishu.config.FeishuConfig;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class ConfigPanel {

    private final JPanel panel;
    private final ConfigManager configManager;
    private final ObjectMapper objectMapper;
    private final Runnable onConfigSaved;

    private JCheckBox feishuEnabledCheckBox;
    private JTextField feishuAppIdField;
    private JTextField feishuAppSecretField;
    private JTextField feishuVerificationTokenField;
    private JTextField feishuEncryptKeyField;

    private JCheckBox dingtalkEnabledCheckBox;
    private JTextField dingtalkAppKeyField;
    private JTextField dingtalkAppSecretField;
    private JTextField dingtalkRobotWebhookField;

    public ConfigPanel(ConfigManager configManager, Runnable onConfigSaved) {
        this.configManager = configManager;
        this.objectMapper = new ObjectMapper();
        this.onConfigSaved = onConfigSaved;
        this.panel = createPanel();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void refreshConfig() {
        loadFeishuConfig();
        loadDingTalkConfig();
    }

    private JPanel createPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        tabbedPane.addTab("飞书配置", createFeishuConfigPanel());
        tabbedPane.addTab("钉钉配置", createDingTalkConfigPanel());
        p.add(tabbedPane, BorderLayout.CENTER);

        p.add(createButtonPanel(), BorderLayout.SOUTH);

        return p;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton refreshButton = new JButton("刷新配置");
        refreshButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        refreshButton.addActionListener(e -> handleRefresh());
        buttonPanel.add(refreshButton);

        JButton saveButton = new JButton("保存配置");
        saveButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        saveButton.setBackground(new Color(70, 130, 180));
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> handleSave());
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    private JPanel createFeishuConfigPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = createDefaultConstraints();
        int row = 0;

        feishuEnabledCheckBox = new JCheckBox("启用飞书");
        feishuEnabledCheckBox.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        addFullWidthRow(p, gbc, row++, feishuEnabledCheckBox);

        feishuAppIdField = addLabeledField(p, gbc, row++, "App ID:", new JTextField(30));
        feishuAppSecretField = addLabeledField(p, gbc, row++, "App Secret:", new JPasswordField(30));
        feishuVerificationTokenField = addLabeledField(p, gbc, row++, "Verification Token:", new JPasswordField(30));
        feishuEncryptKeyField = addLabeledField(p, gbc, row++, "Encrypt Key:", new JPasswordField(30));

        loadFeishuConfig();
        return p;
    }

    private JPanel createDingTalkConfigPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = createDefaultConstraints();
        int row = 0;

        dingtalkEnabledCheckBox = new JCheckBox("启用钉钉");
        dingtalkEnabledCheckBox.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        addFullWidthRow(p, gbc, row++, dingtalkEnabledCheckBox);

        dingtalkAppKeyField = addLabeledField(p, gbc, row++, "App Key:", new JTextField(30));
        dingtalkAppSecretField = addLabeledField(p, gbc, row++, "App Secret:", new JPasswordField(30));
        dingtalkRobotWebhookField = addLabeledField(p, gbc, row++, "机器人 Webhook:", new JTextField(30));

        loadDingTalkConfig();
        return p;
    }

    private void loadFeishuConfig() {
        FeishuConfig config = objectMapper.convertValue(configManager.getFeishuConfigMap(), FeishuConfig.class);
        if (feishuEnabledCheckBox != null) feishuEnabledCheckBox.setSelected(config.isEnabled());
        if (feishuAppIdField != null) feishuAppIdField.setText(nullToEmpty(config.getAppId()));
        if (feishuAppSecretField != null) feishuAppSecretField.setText(nullToEmpty(config.getAppSecret()));
        if (feishuVerificationTokenField != null) feishuVerificationTokenField.setText(nullToEmpty(config.getVerificationToken()));
        if (feishuEncryptKeyField != null) feishuEncryptKeyField.setText(nullToEmpty(config.getEncryptKey()));
    }

    private void loadDingTalkConfig() {
        DingTalkConfig config = objectMapper.convertValue(configManager.getDingTalkConfigMap(), DingTalkConfig.class);
        if (dingtalkEnabledCheckBox != null) dingtalkEnabledCheckBox.setSelected(config.isEnabled());
        if (dingtalkAppKeyField != null) dingtalkAppKeyField.setText(nullToEmpty(config.getAppKey()));
        if (dingtalkAppSecretField != null) dingtalkAppSecretField.setText(nullToEmpty(config.getAppSecret()));
        if (dingtalkRobotWebhookField != null) dingtalkRobotWebhookField.setText(nullToEmpty(config.getRobotWebhook()));
    }

    private void handleSave() {
        try {
            FeishuConfig feishuConfig = new FeishuConfig();
            feishuConfig.setEnabled(feishuEnabledCheckBox.isSelected());
            feishuConfig.setAppId(feishuAppIdField.getText().trim());
            feishuConfig.setAppSecret(feishuAppSecretField.getText().trim());
            feishuConfig.setVerificationToken(feishuVerificationTokenField.getText().trim());
            feishuConfig.setEncryptKey(feishuEncryptKeyField.getText().trim());
            configManager.updateFeishuConfig(objectMapper.convertValue(feishuConfig, java.util.Map.class));

            DingTalkConfig dingtalkConfig = new DingTalkConfig();
            dingtalkConfig.setEnabled(dingtalkEnabledCheckBox.isSelected());
            dingtalkConfig.setAppKey(dingtalkAppKeyField.getText().trim());
            dingtalkConfig.setAppSecret(dingtalkAppSecretField.getText().trim());
            dingtalkConfig.setRobotWebhook(dingtalkRobotWebhookField.getText().trim());
            configManager.updateDingTalkConfig(objectMapper.convertValue(dingtalkConfig, java.util.Map.class));

            log.info("配置保存成功");
            JOptionPane.showMessageDialog(panel, "配置保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);

            if (onConfigSaved != null) {
                onConfigSaved.run();
            }
        } catch (Exception e) {
            log.error("保存配置失败", e);
            JOptionPane.showMessageDialog(panel, "保存配置失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRefresh() {
        try {
            configManager.reloadConfig();
            refreshConfig();
            log.info("配置刷新成功");
            JOptionPane.showMessageDialog(panel, "配置已从 config.json 刷新成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            log.error("刷新配置失败", e);
            JOptionPane.showMessageDialog(panel, "刷新配置失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private GridBagConstraints createDefaultConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private void addFullWidthRow(JPanel panel, GridBagConstraints gbc, int row, JComponent component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        panel.add(component, gbc);
        gbc.gridwidth = 1;
    }

    private JTextField addLabeledField(JPanel panel, GridBagConstraints gbc, int row, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(label, gbc);

        field.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        panel.add(field, gbc);

        return field;
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
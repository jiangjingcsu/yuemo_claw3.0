package com.yuemo.demo.channel.swing.ui;

import com.yuemo.demo.common.util.MarkdownReader;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.List;

@Slf4j
public class PromptPanel {

    private final JPanel panel;
    private JList<String> fileList;
    private JTextArea contentArea;

    public PromptPanel() {
        this.panel = createPanel();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void refreshFileList() {
        List<String> files = MarkdownReader.getMarkdownFiles();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String file : files) {
            model.addElement(file);
        }
        fileList.setModel(model);

        if (!files.isEmpty()) {
            fileList.setSelectedIndex(0);
        }
    }

    private JPanel createPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        p.add(leftPanel, BorderLayout.WEST);
        p.add(rightPanel, BorderLayout.CENTER);

        refreshFileList();
        return p;
    }

    private JPanel createLeftPanel() {
        JPanel left = new JPanel(new BorderLayout(10, 10));
        left.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        JLabel fileListLabel = new JLabel("Markdown 文件:");
        fileListLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        left.add(fileListLabel, BorderLayout.NORTH);

        fileList = new JList<>();
        fileList.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    loadFileContent(selectedFile);
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(fileList);
        listScrollPane.setPreferredSize(new Dimension(200, 0));
        left.add(listScrollPane, BorderLayout.CENTER);

        left.add(createButtonPanel(), BorderLayout.SOUTH);

        return left;
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        JLabel contentLabel = new JLabel("文件内容:");
        contentLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        right.add(contentLabel, BorderLayout.NORTH);

        contentArea = new JTextArea();
        contentArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JScrollPane contentScrollPane = new JScrollPane(contentArea);
        contentScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        right.add(contentScrollPane, BorderLayout.CENTER);

        return right;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));

        JButton refreshButton = new JButton("刷新列表");
        refreshButton.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        refreshButton.addActionListener(e -> refreshFileList());
        buttonPanel.add(refreshButton);

        JButton saveButton = new JButton("保存文件");
        saveButton.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        saveButton.addActionListener(e -> saveFileContent());
        buttonPanel.add(saveButton);

        JButton reloadButton = new JButton("重新加载提示词");
        reloadButton.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        reloadButton.addActionListener(e ->
                JOptionPane.showMessageDialog(panel, "系统提示词重新加载功能需通过 Gateway 实现", "提示", JOptionPane.INFORMATION_MESSAGE));
        buttonPanel.add(reloadButton);

        return buttonPanel;
    }

    private void loadFileContent(String fileName) {
        String content = MarkdownReader.readMarkdownFile(fileName);
        contentArea.setText(content);
    }

    private void saveFileContent() {
        String selectedFile = fileList.getSelectedValue();
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(panel, "请先选择一个文件", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String content = contentArea.getText();
        MarkdownReader.writeMarkdownFile(selectedFile, content);
        JOptionPane.showMessageDialog(panel, "文件保存成功", "成功", JOptionPane.INFORMATION_MESSAGE);
    }
}
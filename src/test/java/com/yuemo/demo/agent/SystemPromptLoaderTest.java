package com.yuemo.demo.agent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SystemPromptLoaderTest {

    @TempDir
    Path tempDir;

    private SystemPromptLoader loader;
    private Path workspaceDir;

    @BeforeEach
    void setUp() {
        workspaceDir = tempDir.resolve("workspace");
        try {
            Files.createDirectories(workspaceDir);
        } catch (IOException e) {
            fail("无法创建临时 workspace 目录");
        }
        loader = new SystemPromptLoader();
        loader.setWorkspaceDir(workspaceDir.toString());
        loader.setExcludeDirs(Set.of("skills", ".git"));
        loader.setExcludeFiles(Set.of("README.md", ".gitignore"));
        loader.setEnabled(true);
    }

    @AfterEach
    void tearDown() {
        if (loader != null) {
            loader.clearCache();
        }
    }

    @Test
    void testGetMarkdownFiles_EmptyDirectory() {
        List<Path> files = loader.getMarkdownFiles();
        assertNotNull(files);
        assertTrue(files.isEmpty(), "空目录应返回空列表");
    }

    @Test
    void testGetMarkdownFiles_SingleFile() throws IOException {
        Path testFile = workspaceDir.resolve("test.md");
        Files.writeString(testFile, "# Test");

        loader.clearCache();
        List<Path> files = loader.getMarkdownFiles();

        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("test.md", files.get(0).getFileName().toString());
    }

    @Test
    void testGetMarkdownFiles_MultipleFiles() throws IOException {
        Files.writeString(workspaceDir.resolve("aaa.md"), "# A");
        Files.writeString(workspaceDir.resolve("zzz.md"), "# Z");
        Files.writeString(workspaceDir.resolve("mmm.md"), "# M");

        loader.clearCache();
        List<Path> files = loader.getMarkdownFiles();

        assertNotNull(files);
        assertEquals(3, files.size());
        assertEquals("aaa.md", files.get(0).getFileName().toString());
        assertEquals("mmm.md", files.get(1).getFileName().toString());
        assertEquals("zzz.md", files.get(2).getFileName().toString());
    }

    @Test
    void testGetMarkdownFiles_NonMdFiles_Excluded() throws IOException {
        Files.writeString(workspaceDir.resolve("test.md"), "# Test");
        Files.writeString(workspaceDir.resolve("readme.txt"), "Text file");
        Files.writeString(workspaceDir.resolve("data.json"), "{}");

        loader.clearCache();
        List<Path> files = loader.getMarkdownFiles();

        assertNotNull(files);
        assertEquals(1, files.size());
        assertTrue(files.get(0).getFileName().toString().endsWith(".md"));
    }

    @Test
    void testGetMarkdownFiles_RecursiveScan() throws IOException {
        Path subDir1 = workspaceDir.resolve("subdir1");
        Path subDir2 = workspaceDir.resolve("subdir2");
        Files.createDirectories(subDir1);
        Files.createDirectories(subDir2);

        Files.writeString(workspaceDir.resolve("root.md"), "# Root");
        Files.writeString(subDir1.resolve("nested1.md"), "# Nested 1");
        Files.writeString(subDir2.resolve("nested2.md"), "# Nested 2");

        loader.clearCache();
        List<Path> files = loader.getMarkdownFiles();

        assertNotNull(files);
        assertEquals(3, files.size());
    }

    @Test
    void testGetMarkdownFiles_ExcludedDirs() throws IOException {
        Path skillsDir = workspaceDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("skill.md"), "# Skill");

        Path normalDir = workspaceDir.resolve("normal");
        Files.createDirectories(normalDir);
        Files.writeString(normalDir.resolve("normal.md"), "# Normal");

        loader.clearCache();
        List<Path> files = loader.getMarkdownFiles();

        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("normal.md", files.get(0).getFileName().toString());
    }

    @Test
    void testGetMarkdownFiles_ExcludedFiles() throws IOException {
        Files.writeString(workspaceDir.resolve("README.md"), "# README");
        Files.writeString(workspaceDir.resolve("test.md"), "# Test");
        Files.writeString(workspaceDir.resolve(".gitignore"), "# Ignore");

        loader.clearCache();
        List<Path> files = loader.getMarkdownFiles();

        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("test.md", files.get(0).getFileName().toString());
    }

    @Test
    void testGetSystemPrompt_Success() throws IOException {
        Files.writeString(workspaceDir.resolve("file1.md"), "# File 1\nContent 1");
        Files.writeString(workspaceDir.resolve("file2.md"), "# File 2\nContent 2");

        loader.clearCache();
        String prompt = loader.getSystemPrompt();

        assertNotNull(prompt);
        assertTrue(prompt.contains("File 1"));
        assertTrue(prompt.contains("Content 1"));
        assertTrue(prompt.contains("File 2"));
        assertTrue(prompt.contains("Content 2"));
    }

    @Test
    void testGetSystemPrompt_NoFiles_ReturnsDefault() {
        loader.clearCache();
        String prompt = loader.getSystemPrompt();

        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("智能助手"));
    }

    @Test
    void testGetSystemPrompt_Caching() throws IOException {
        Files.writeString(workspaceDir.resolve("test.md"), "# Original");

        loader.clearCache();
        String prompt1 = loader.getSystemPrompt();
        assertTrue(prompt1.contains("Original"));

        Files.writeString(workspaceDir.resolve("test.md"), "# Modified");
        String prompt2 = loader.getSystemPrompt();

        assertTrue(prompt2.contains("Original"), "缓存未过期时应返回原内容");
    }

    @Test
    void testRefresh_ClearsCache() throws IOException {
        Files.writeString(workspaceDir.resolve("test.md"), "# Original");

        loader.clearCache();
        assertEquals(1, loader.getMarkdownFiles().size());

        Files.writeString(workspaceDir.resolve("new.md"), "# New");
        loader.refresh();

        List<String> fileNames = loader.getLoadedFileNames();
        assertTrue(fileNames.contains("new.md"));
    }

    @Test
    void testGetFeishuOpenId_NotConfigured() {
        System.clearProperty("feishu.user.openid");
        String openId = loader.getFeishuOpenId();
        assertNull(openId);
    }

    @Test
    void testGetFeishuOpenId_FromSystemProperty() {
        System.setProperty("feishu.user.openid", "test_openid_123");
        try {
            String openId = loader.getFeishuOpenId();
            assertEquals("test_openid_123", openId);
        } finally {
            System.clearProperty("feishu.user.openid");
        }
    }

    @Test
    void testSetExcludeDirs() throws IOException {
        Path customDir = workspaceDir.resolve("custom");
        Files.createDirectories(customDir);
        Files.writeString(customDir.resolve("custom.md"), "# Custom");

        loader.clearCache();
        assertEquals(1, loader.getMarkdownFiles().size());

        loader.setExcludeDirs(Set.of("custom"));
        assertEquals(0, loader.getMarkdownFiles().size());
    }

    @Test
    void testSetExcludeFiles() throws IOException {
        Files.writeString(workspaceDir.resolve("skip.md"), "# Skip");

        loader.clearCache();
        assertEquals(1, loader.getMarkdownFiles().size());

        loader.setExcludeFiles(Set.of("skip.md"));
        assertEquals(0, loader.getMarkdownFiles().size());
    }

    @Test
    void testEnabled_False_ReturnsEmptyList() {
        loader.setEnabled(false);
        try {
            List<Path> files = loader.getMarkdownFiles();
            assertNotNull(files);
            assertTrue(files.isEmpty());
        } finally {
            loader.setEnabled(true);
        }
    }

    @Test
    void testGetLoadedFileNames() throws IOException {
        Files.writeString(workspaceDir.resolve("aaa.md"), "# A");
        Files.writeString(workspaceDir.resolve("bbb.md"), "# B");

        loader.clearCache();
        List<String> names = loader.getLoadedFileNames();

        assertNotNull(names);
        assertEquals(2, names.size());
        assertTrue(names.contains("aaa.md"));
        assertTrue(names.contains("bbb.md"));
    }

    @Test
    void testFileVisitFailure_Handled() throws IOException {
        Path testFile = workspaceDir.resolve("accessible.md");
        Files.writeString(testFile, "# Accessible");

        loader.clearCache();
        List<Path> files = loader.getMarkdownFiles();
        assertNotNull(files);
        assertFalse(files.isEmpty());
    }
}
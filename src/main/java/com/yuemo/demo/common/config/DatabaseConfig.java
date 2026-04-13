package com.yuemo.demo.common.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.File;

@Slf4j
@Configuration
public class DatabaseConfig {

    private static final String DEFAULT_DB_PATH = "workspace/data/memory.db";

    private final ConfigManager configManager;
    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    public DatabaseConfig(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @PostConstruct
    public void init() {
        String dbPath = getConfiguredDbPath();
        File dbFile = new File(dbPath);
        File dbDir = dbFile.getParentFile();
        if (dbDir != null && !dbDir.exists()) {
            dbDir.mkdirs();
            log.info("创建数据库目录: {}", dbDir.getAbsolutePath());
        }

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite:" + dbPath);
        this.dataSource = ds;
        this.jdbcTemplate = new JdbcTemplate(ds);

        initializeDatabase();
        log.info("SQLite 数据库初始化完成: {}", dbFile.getAbsolutePath());
    }

    private String getConfiguredDbPath() {
        try {
            AppConfig.DatabaseConfig dbConfig = configManager.getConfig().getDatabase();
            if (dbConfig != null && dbConfig.getPath() != null && !dbConfig.getPath().isEmpty()) {
                log.info("使用配置的数据库路径: {}", dbConfig.getPath());
                return dbConfig.getPath();
            }
        } catch (Exception e) {
            log.warn("读取数据库配置失败，使用默认路径: {}", e.getMessage());
        }
        log.info("使用默认数据库路径: {}", DEFAULT_DB_PATH);
        return DEFAULT_DB_PATH;
    }

    private void initializeDatabase() {
        try {
            String schema = """
                CREATE TABLE IF NOT EXISTS chat_session (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    session_title TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    is_active INTEGER NOT NULL DEFAULT 1,
                    metadata TEXT
                );

                CREATE TABLE IF NOT EXISTS chat_message (
                    id TEXT PRIMARY KEY,
                    session_id TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('user', 'assistant', 'system')),
                    content TEXT NOT NULL,
                    tokens INTEGER,
                    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
                );

                CREATE TABLE IF NOT EXISTS user_context (
                    user_id TEXT NOT NULL,
                    current_session_id TEXT,
                    last_active_time TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    metadata TEXT,
                    PRIMARY KEY (user_id),
                    FOREIGN KEY (current_session_id) REFERENCES chat_session(id) ON DELETE SET NULL
                );

                CREATE INDEX IF NOT EXISTS idx_message_session ON chat_message(session_id);
                CREATE INDEX IF NOT EXISTS idx_message_created ON chat_message(created_at);
                CREATE INDEX IF NOT EXISTS idx_session_user ON chat_session(user_id);
                CREATE INDEX IF NOT EXISTS idx_session_updated ON chat_session(updated_at DESC);
                """;

            String[] statements = schema.split(";");
            for (String sql : statements) {
                sql = sql.trim();
                if (!sql.isEmpty()) {
                    jdbcTemplate.execute(sql);
                }
            }
            log.info("数据库表初始化完成");
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭数据库连接");
    }
}

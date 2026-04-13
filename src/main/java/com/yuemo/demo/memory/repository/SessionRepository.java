package com.yuemo.demo.memory.repository;

import com.yuemo.demo.common.config.DatabaseConfig;
import com.yuemo.demo.memory.entity.ChatSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
public class SessionRepository {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DatabaseConfig databaseConfig;

    public SessionRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public ChatSession create(String userId, String title) {
        String id = UUID.randomUUID().toString();
        String now = LocalDateTime.now().format(FORMATTER);

        String sql = "INSERT INTO chat_session (id, user_id, session_title, created_at, updated_at, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, 1)";

        databaseConfig.getJdbcTemplate().update(sql, id, userId, title, now, now);

        ChatSession session = new ChatSession();
        session.setId(id);
        session.setUserId(userId);
        session.setSessionTitle(title);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setIsActive(1);

        log.info("创建会话: id={}, user={}", id, userId);
        return session;
    }

    public Optional<ChatSession> findById(String id) {
        String sql = "SELECT * FROM chat_session WHERE id = ?";
        List<ChatSession> results = databaseConfig.getJdbcTemplate().query(sql, (rs, rowNum) -> {
            ChatSession session = new ChatSession();
            session.setId(rs.getString("id"));
            session.setUserId(rs.getString("user_id"));
            session.setSessionTitle(rs.getString("session_title"));
            session.setCreatedAt(parseDateTime(rs.getString("created_at")));
            session.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
            session.setIsActive(rs.getInt("is_active"));
            session.setMetadata(rs.getString("metadata"));
            return session;
        }, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<ChatSession> findByUserId(String userId, int limit) {
        String sql = "SELECT * FROM chat_session WHERE user_id = ? " +
                     "ORDER BY updated_at DESC LIMIT ?";
        return databaseConfig.getJdbcTemplate().query(sql, (rs, rowNum) -> {
            ChatSession session = new ChatSession();
            session.setId(rs.getString("id"));
            session.setUserId(rs.getString("user_id"));
            session.setSessionTitle(rs.getString("session_title"));
            session.setCreatedAt(parseDateTime(rs.getString("created_at")));
            session.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
            session.setIsActive(rs.getInt("is_active"));
            session.setMetadata(rs.getString("metadata"));
            return session;
        }, userId, limit);
    }

    public void updateTitle(String sessionId, String title) {
        String now = LocalDateTime.now().format(FORMATTER);
        String sql = "UPDATE chat_session SET session_title = ?, updated_at = ? WHERE id = ?";
        databaseConfig.getJdbcTemplate().update(sql, title, now, sessionId);
    }

    public void updateTime(String sessionId) {
        String now = LocalDateTime.now().format(FORMATTER);
        String sql = "UPDATE chat_session SET updated_at = ? WHERE id = ?";
        databaseConfig.getJdbcTemplate().update(sql, now, sessionId);
    }

    public void delete(String sessionId) {
        String sql = "DELETE FROM chat_session WHERE id = ?";
        databaseConfig.getJdbcTemplate().update(sql, sessionId);
    }

    private LocalDateTime parseDateTime(String str) {
        if (str == null || str.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(str, FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}

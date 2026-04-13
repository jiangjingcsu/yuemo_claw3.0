package com.yuemo.demo.memory.repository;

import com.yuemo.demo.common.config.DatabaseConfig;
import com.yuemo.demo.memory.entity.MessageRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
public class MessageRepository {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DatabaseConfig databaseConfig;

    public MessageRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public MessageRecord create(String sessionId, String role, String content) {
        return create(sessionId, role, content, null);
    }

    public MessageRecord create(String sessionId, String role, String content, Integer tokens) {
        String id = UUID.randomUUID().toString();
        String now = LocalDateTime.now().format(FORMATTER);

        String sql = "INSERT INTO chat_message (id, session_id, role, content, tokens, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        databaseConfig.getJdbcTemplate().update(sql, id, sessionId, role, content, tokens, now);

        MessageRecord message = new MessageRecord();
        message.setId(id);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setTokens(tokens);
        message.setCreatedAt(LocalDateTime.now());

        log.debug("创建消息: id={}, session={}, role={}", id, sessionId, role);
        return message;
    }

    public List<MessageRecord> findBySessionId(String sessionId, int offset, int limit) {
        String sql = "SELECT * FROM chat_message WHERE session_id = ? ORDER BY created_at ASC LIMIT ? OFFSET ?";
        return databaseConfig.getJdbcTemplate().query(sql, (rs, rowNum) -> {
            MessageRecord message = new MessageRecord();
            message.setId(rs.getString("id"));
            message.setSessionId(rs.getString("session_id"));
            message.setRole(rs.getString("role"));
            message.setContent(rs.getString("content"));
            message.setTokens(rs.getObject("tokens") != null ? rs.getInt("tokens") : null);
            message.setCreatedAt(parseDateTime(rs.getString("created_at")));
            return message;
        }, sessionId, limit, offset);
    }

    public List<MessageRecord> findRecentBySessionId(String sessionId, int count) {
        String sql = "SELECT * FROM chat_message WHERE session_id = ? ORDER BY created_at DESC LIMIT ?";
        List<MessageRecord> messages = databaseConfig.getJdbcTemplate().query(sql, (rs, rowNum) -> {
            MessageRecord message = new MessageRecord();
            message.setId(rs.getString("id"));
            message.setSessionId(rs.getString("session_id"));
            message.setRole(rs.getString("role"));
            message.setContent(rs.getString("content"));
            message.setTokens(rs.getObject("tokens") != null ? rs.getInt("tokens") : null);
            message.setCreatedAt(parseDateTime(rs.getString("created_at")));
            return message;
        }, sessionId, count);

        return messages.reversed();
    }

    public List<MessageRecord> searchByUserIdAndKeyword(String userId, String keyword, int limit) {
        String sql = """
            SELECT m.* FROM chat_message m
            INNER JOIN chat_session s ON m.session_id = s.id
            WHERE s.user_id = ? AND m.content LIKE ? AND m.role = 'user'
            ORDER BY m.created_at DESC LIMIT ?
            """;
        return databaseConfig.getJdbcTemplate().query(sql, (rs, rowNum) -> {
            MessageRecord message = new MessageRecord();
            message.setId(rs.getString("id"));
            message.setSessionId(rs.getString("session_id"));
            message.setRole(rs.getString("role"));
            message.setContent(rs.getString("content"));
            message.setTokens(rs.getObject("tokens") != null ? rs.getInt("tokens") : null);
            message.setCreatedAt(parseDateTime(rs.getString("created_at")));
            return message;
        }, userId, "%" + keyword + "%", limit);
    }

    public int countBySessionId(String sessionId) {
        String sql = "SELECT COUNT(*) FROM chat_message WHERE session_id = ?";
        Integer count = databaseConfig.getJdbcTemplate().queryForObject(sql, Integer.class, sessionId);
        return count != null ? count : 0;
    }

    public void deleteBySessionId(String sessionId) {
        String sql = "DELETE FROM chat_message WHERE session_id = ?";
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

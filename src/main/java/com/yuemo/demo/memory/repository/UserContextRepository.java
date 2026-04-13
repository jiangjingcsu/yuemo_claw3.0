package com.yuemo.demo.memory.repository;

import com.yuemo.demo.common.config.DatabaseConfig;
import com.yuemo.demo.memory.entity.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Repository
public class UserContextRepository {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DatabaseConfig databaseConfig;

    public UserContextRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void saveOrUpdate(UserContext context) {
        String now = LocalDateTime.now().format(FORMATTER);
        String sql = """
            INSERT INTO user_context (user_id, current_session_id, last_active_time, metadata)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(user_id) DO UPDATE SET
                current_session_id = excluded.current_session_id,
                last_active_time = excluded.last_active_time,
                metadata = excluded.metadata
            """;
        databaseConfig.getJdbcTemplate().update(sql,
            context.getUserId(),
            context.getCurrentSessionId(),
            now,
            context.getMetadata());
        log.debug("保存用户上下文: user={}, session={}",
            context.getUserId(), context.getCurrentSessionId());
    }

    public Optional<UserContext> findByUserId(String userId) {
        String sql = "SELECT * FROM user_context WHERE user_id = ?";
        var results = databaseConfig.getJdbcTemplate().query(sql, (rs, rowNum) -> {
            UserContext context = new UserContext();
            context.setUserId(rs.getString("user_id"));
            context.setCurrentSessionId(rs.getString("current_session_id"));
            context.setLastActiveTime(parseDateTime(rs.getString("last_active_time")));
            context.setMetadata(rs.getString("metadata"));
            return context;
        }, userId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void updateCurrentSession(String userId, String sessionId) {
        String now = LocalDateTime.now().format(FORMATTER);
        String sql = "UPDATE user_context SET current_session_id = ?, last_active_time = ? WHERE user_id = ?";
        databaseConfig.getJdbcTemplate().update(sql, sessionId, now, userId);
        log.debug("更新用户当前会话: user={}, session={}", userId, sessionId);
    }

    public void delete(String userId) {
        String sql = "DELETE FROM user_context WHERE user_id = ?";
        databaseConfig.getJdbcTemplate().update(sql, userId);
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

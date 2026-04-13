package com.yuemo.demo.memory.service;

import com.yuemo.demo.memory.entity.MessageRecord;
import com.yuemo.demo.memory.entity.ChatSession;
import com.yuemo.demo.memory.entity.UserContext;
import com.yuemo.demo.memory.repository.MessageRepository;
import com.yuemo.demo.memory.repository.SessionRepository;
import com.yuemo.demo.memory.repository.UserContextRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MemoryService {

    private static final int DEFAULT_CONTEXT_SIZE = 20;
    private static final int DEFAULT_TOKEN_LIMIT = 4000;
    private static final int CHARS_PER_TOKEN = 4;

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final UserContextRepository userContextRepository;

    public MemoryService(SessionRepository sessionRepository,
                        MessageRepository messageRepository,
                        UserContextRepository userContextRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userContextRepository = userContextRepository;
    }

    public String createSession(String userId, String title) {
        ChatSession session = sessionRepository.create(userId, title);
        updateUserCurrentSession(userId, session.getId());
        return session.getId();
    }

    public Optional<ChatSession> getSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    public List<ChatSession> getUserSessions(String userId, int limit) {
        return sessionRepository.findByUserId(userId, limit);
    }

    public void switchSession(String userId, String sessionId) {
        updateUserCurrentSession(userId, sessionId);
        log.info("用户切换会话: user={}, session={}", userId, sessionId);
    }

    public String addUserMessage(String sessionId, String content) {
        MessageRecord message = messageRepository.create(sessionId, "user", content);
        sessionRepository.updateTime(sessionId);
        return message.getId();
    }

    public String addAssistantMessage(String sessionId, String content) {
        MessageRecord message = messageRepository.create(sessionId, "assistant", content);
        sessionRepository.updateTime(sessionId);
        return message.getId();
    }

    public List<MessageRecord> getSessionMessages(String sessionId, int offset, int limit) {
        return messageRepository.findBySessionId(sessionId, offset, limit);
    }

    public List<MessageRecord> getRecentMessages(String sessionId, int count) {
        return messageRepository.findRecentBySessionId(sessionId, count);
    }

    public String getRecentContext(String userId, int messageCount) {
        return getRecentContext(userId, messageCount, DEFAULT_TOKEN_LIMIT);
    }

    public String getRecentContextBySessionId(String sessionId, int messageCount) {
        return getRecentContextBySessionId(sessionId, messageCount, DEFAULT_TOKEN_LIMIT);
    }

    public String getRecentContextBySessionId(String sessionId, int messageCount, int maxTokens) {
        List<MessageRecord> messages = messageRepository.findRecentBySessionId(sessionId, messageCount);

        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("【历史对话上下文】\n");

        int totalTokens = 0;
        for (MessageRecord msg : messages) {
            int msgTokens = estimateTokens(msg.getContent());
            if (totalTokens + msgTokens > maxTokens) {
                log.debug("上下文 token 达到限制，提前截断: totalTokens={}, maxTokens={}", totalTokens, maxTokens);
                break;
            }
            String roleName = "user".equals(msg.getRole()) ? "用户" : "助手";
            context.append(roleName).append(": ").append(msg.getContent()).append("\n");
            totalTokens += msgTokens;
        }
        context.append("【上下文结束】\n\n");

        log.debug("生成上下文: messages={}, tokens={}", messages.size(), totalTokens);
        return context.toString();
    }

    public String getRecentContext(String userId, int messageCount, int maxTokens) {
        Optional<UserContext> contextOpt = userContextRepository.findByUserId(userId);
        if (contextOpt.isEmpty() || contextOpt.get().getCurrentSessionId() == null) {
            return "";
        }

        String sessionId = contextOpt.get().getCurrentSessionId();
        List<MessageRecord> messages = messageRepository.findRecentBySessionId(sessionId, messageCount);

        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("【历史对话上下文】\n");

        int totalTokens = 0;
        for (MessageRecord msg : messages) {
            int msgTokens = estimateTokens(msg.getContent());
            if (totalTokens + msgTokens > maxTokens) {
                log.debug("上下文 token 达到限制，提前截断: totalTokens={}, maxTokens={}", totalTokens, maxTokens);
                break;
            }
            String roleName = "user".equals(msg.getRole()) ? "用户" : "助手";
            context.append(roleName).append(": ").append(msg.getContent()).append("\n");
            totalTokens += msgTokens;
        }
        context.append("【上下文结束】\n\n");

        log.debug("生成上下文: messages={}, tokens={}", messages.size(), totalTokens);
        return context.toString();
    }

    private int estimateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(content.length() / (double) CHARS_PER_TOKEN);
    }

    public int countSessionMessages(String sessionId) {
        return messageRepository.countBySessionId(sessionId);
    }

    public void updateSessionTitle(String sessionId, String title) {
        sessionRepository.updateTitle(sessionId, title);
    }

    public void deleteSession(String sessionId) {
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(sessionId);
        log.info("删除会话: sessionId={}", sessionId);
    }

    public String getOrCreateCurrentSession(String userId) {
        Optional<UserContext> contextOpt = userContextRepository.findByUserId(userId);
        if (contextOpt.isPresent() && contextOpt.get().getCurrentSessionId() != null) {
            String sessionId = contextOpt.get().getCurrentSessionId();
            if (sessionRepository.findById(sessionId).isPresent()) {
                return sessionId;
            }
        }
        return createSession(userId, null);
    }

    private void updateUserCurrentSession(String userId, String sessionId) {
        UserContext context = new UserContext();
        context.setUserId(userId);
        context.setCurrentSessionId(sessionId);
        userContextRepository.saveOrUpdate(context);
    }
}

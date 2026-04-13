package com.yuemo.demo.memory;

public class SessionContextHolder {

    private static final ThreadLocal<SessionContext> contextHolder = new ThreadLocal<>();

    public static class SessionContext {
        private final String sessionId;
        private final String userId;

        public SessionContext(String sessionId, String userId) {
            this.sessionId = sessionId;
            this.userId = userId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getUserId() {
            return userId;
        }
    }

    public static void setContext(String sessionId, String userId) {
        contextHolder.set(new SessionContext(sessionId, userId));
    }

    public static SessionContext getContext() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }

    public static String getCurrentSessionId() {
        SessionContext ctx = contextHolder.get();
        return ctx != null ? ctx.getSessionId() : null;
    }

    public static String getCurrentUserId() {
        SessionContext ctx = contextHolder.get();
        return ctx != null ? ctx.getUserId() : null;
    }
}

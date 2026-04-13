package com.yuemo.demo.channel.feishu.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书用户信息仓储
 *
 * <p>管理飞书用户的 openId 与用户名的映射关系，提供本地缓存以减少 API 调用。
 * 使用 ConcurrentHashMap 保证线程安全。</p>
 *
 * <p>当前实现为本地内存缓存，用户名为自动生成的占位名称。
 * 后续可扩展为调用飞书用户信息 API 获取真实用户名。</p>
 */
@Slf4j
@Component
public class FeishuUserRepository {

    /**
     * openId → 用户名 映射缓存
     */
    private final Map<String, String> userOpenIdMap = new ConcurrentHashMap<>();

    /**
     * userId → openId 映射缓存（预留，当前与 openId 相同）
     */
    private final Map<String, String> userIdToOpenIdMap = new ConcurrentHashMap<>();

    /**
     * 根据 openId 获取用户名
     *
     * <p>优先从缓存获取，缓存未命中时生成占位用户名并写入缓存。</p>
     *
     * @param openId 用户的 openId，不可为null
     * @return 用户名，缓存命中返回真实名称，否则返回占位名称
     */
    public String getUserName(String openId) {
        if (userOpenIdMap.containsKey(openId)) {
            return userOpenIdMap.get(openId);
        }

        String userName = "用户_" + openId.substring(0, Math.min(8, openId.length()));
        userOpenIdMap.put(openId, userName);
        log.debug("缓存用户信息 - OpenId: {}, UserName: {}", openId, userName);
        return userName;
    }

    /**
     * 根据 userId 获取对应的 openId
     *
     * <p>当前实现中 userId 与 openId 相同，直接返回。
     * 预留接口供后续扩展。</p>
     *
     * @param userId 用户ID
     * @return 对应的 openId，未找到时返回 userId 本身
     */
    public String getOpenIdByUserId(String userId) {
        String openId = userIdToOpenIdMap.get(userId);
        if (openId == null) {
            openId = userId;
        }
        return openId;
    }

    /**
     * 缓存用户信息
     *
     * @param openId   用户的 openId，不可为null
     * @param userName 用户名，不可为null
     */
    public void cacheUser(String openId, String userName) {
        if (openId != null && userName != null) {
            userOpenIdMap.put(openId, userName);
            userIdToOpenIdMap.put(openId, openId);
            log.debug("更新用户缓存 - OpenId: {}, UserName: {}", openId, userName);
        }
    }

    /**
     * 清除所有用户缓存
     */
    public void clearCache() {
        userOpenIdMap.clear();
        userIdToOpenIdMap.clear();
        log.info("用户缓存已清除");
    }
}

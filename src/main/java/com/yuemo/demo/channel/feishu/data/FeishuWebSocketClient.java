package com.yuemo.demo.channel.feishu.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.Client;
import com.lark.oapi.core.enums.AppType;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.CreateFileReq;
import com.lark.oapi.service.im.v1.model.CreateFileResp;
import com.lark.oapi.service.im.v1.model.CreateImageReq;
import com.lark.oapi.service.im.v1.model.CreateImageReqBody;
import com.lark.oapi.service.im.v1.model.CreateImageResp;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.yuemo.demo.channel.feishu.exception.FeishuException;
import com.yuemo.demo.channel.feishu.config.FeishuConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 飞书 WebSocket 长连接客户端
 *
 * <p>封装飞书 SDK 的两个组件：</p>
 * <ul>
 *   <li>{@code com.lark.oapi.ws.Client} - WebSocket 长连接，用于接收飞书事件推送</li>
 *   <li>{@code com.lark.oapi.Client} - REST API 客户端，用于发送消息、上传文件等 API 调用</li>
 * </ul>
 *
 * <p>消息处理采用异步模式：handle() 方法仅将任务提交到线程池后立即返回，
 * 避免 SDK 内部超时导致飞书重推事件。</p>
 *
 * <p>文件发送流程：先调用 uploadFile() 上传文件获取 file_key，
 * 再调用 sendFileMessage() 发送文件消息。</p>
 *
 * @see FeishuConfig 飞书配置
 * @see FeishuException 飞书业务异常
 */
@Slf4j
@Component
public class FeishuWebSocketClient {

    private com.lark.oapi.ws.Client wsClient;
    private Client apiClient;
    private volatile boolean connected = false;
    private Thread clientThread;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService messageExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(), r -> {
                Thread t = new Thread(r, "feishu-msg-handler");
                t.setDaemon(true);
                return t;
            });

    public FeishuWebSocketClient() {
    }

    /**
     * 启动飞书长连接
     *
     * <p>同时初始化 WebSocket 客户端（接收事件）和 REST API 客户端（发送消息）。</p>
     *
     * @param config         飞书配置，包含 appId、appSecret 等，不可为null
     * @param messageHandler 消息接收回调，不可为null
     * @throws FeishuException 初始化失败时抛出
     */
    public void start(FeishuConfig config, Consumer<P2MessageReceiveV1> messageHandler) {
        if (connected && wsClient != null) {
            log.warn("飞书长连接已连接，跳过启动");
            return;
        }

        try {
            initApiClient(config);
            initWsClient(config, messageHandler);

            clientThread = new Thread(() -> {
                try {
                    log.info("启动飞书长连接客户端...");
                    connected = true;
                    wsClient.start();
                } catch (Exception e) {
                    log.error("飞书长连接异常退出", e);
                    connected = false;
                }
            }, "feishu-ws-client");
            clientThread.setDaemon(true);
            clientThread.start();

            log.info("飞书长连接客户端已启动，正在建立连接...");

        } catch (Exception e) {
            log.error("初始化飞书长连接失败", e);
            connected = false;
            throw new FeishuException("初始化飞书长连接失败", e);
        }
    }

    /**
     * 初始化 REST API 客户端
     */
    private void initApiClient(FeishuConfig config) {
        if (apiClient != null) {
            return;
        }
        apiClient = Client.newBuilder(config.getAppId(), config.getAppSecret())
                .appType(AppType.SELF_BUILT)
                .logReqAtDebug(true)
                .build();
        log.info("飞书 API 客户端初始化成功");
    }

    /**
     * 初始化 WebSocket 长连接客户端
     */
    private void initWsClient(FeishuConfig config, Consumer<P2MessageReceiveV1> messageHandler) {
        EventDispatcher eventDispatcher = EventDispatcher.newBuilder(config.getVerificationToken(), config.getEncryptKey())
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) throws Exception {
                        log.info("收到飞书长连接消息事件: {}", Jsons.DEFAULT.toJson(event));
                        messageExecutor.submit(() -> {
                            try {
                                messageHandler.accept(event);
                            } catch (Exception e) {
                                log.error("处理长连接消息异常", e);
                            }
                        });
                    }
                })
                .build();

        wsClient = new com.lark.oapi.ws.Client.Builder(config.getAppId(), config.getAppSecret())
                .eventHandler(eventDispatcher)
                .build();
    }

    /**
     * 检查 WebSocket 是否已连接
     *
     * @return true 表示长连接正常
     */
    public boolean isConnected() {
        return connected && wsClient != null && clientThread != null && clientThread.isAlive();
    }

    /**
     * 停止飞书长连接
     */
    public void stop() {
        if (wsClient != null) {
            try {
                log.info("关闭飞书长连接...");
                connected = false;
                if (clientThread != null) {
                    clientThread.interrupt();
                    clientThread = null;
                }
                wsClient = null;
                log.info("飞书长连接已关闭");
            } catch (Exception e) {
                log.error("关闭飞书长连接异常", e);
            }
        }

        if (apiClient != null) {
            apiClient = null;
            log.info("飞书 API 客户端已关闭");
        }

        messageExecutor.shutdownNow();
        log.info("消息处理线程池已关闭");
    }

    /**
     * 发送文本消息
     *
     * <p>通过 REST API 向指定用户发送文本消息。</p>
     *
     * @param openId  接收者的 openId，不可为null或空
     * @param content 消息文本内容，不可为null或空
     * @return 发送成功的消息ID
     * @throws FeishuException API 客户端未初始化、发送失败或网络异常时抛出
     */
    public String sendTextMessage(String openId, String content) {
        if (apiClient == null) {
            throw new FeishuException("飞书 API 客户端未初始化");
        }

        try {
            Map<String, String> textContent = new HashMap<>(1);
            textContent.put("text", content);
            String messageContent = objectMapper.writeValueAsString(textContent);

            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType("open_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(openId)
                            .msgType("text")
                            .content(messageContent)
                            .uuid(java.util.UUID.randomUUID().toString())
                            .build())
                    .build();

            CreateMessageResp resp = apiClient.im().v1().message().create(req, RequestOptions.newBuilder().build());

            if (resp.success()) {
                String messageId = resp.getData().getMessageId();
                log.info("飞书消息发送成功 - MessageId: {}", messageId);
                return messageId;
            } else {
                log.error("飞书消息发送失败 - Code: {}, Msg: {}", resp.getCode(), resp.getMsg());
                throw new FeishuException(String.valueOf(resp.getCode()),
                        "飞书消息发送失败: " + resp.getMsg());
            }
        } catch (FeishuException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("构造消息JSON失败", e);
            throw new FeishuException("构造消息JSON失败", e);
        } catch (Exception e) {
            log.error("发送飞书消息异常", e);
            throw new FeishuException("发送飞书消息异常", e);
        }
    }

    /**
     * 上传文件到飞书服务器
     *
     * <p>飞书要求先上传文件获取 file_key，才能发送文件消息。</p>
     *
     * @param filePath 本地文件路径
     * @return 飞书返回的 file_key
     */
    public String uploadFile(String filePath) {
        if (apiClient == null) {
            throw new FeishuException("飞书 API 客户端未初始化");
        }

        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                throw new FeishuException("文件不存在: " + filePath);
            }

            String fileName = file.getName();
            String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            String fileType = resolveFileType(fileExtension);

            CreateFileReq req = CreateFileReq.newBuilder()
                    .createFileReqBody(com.lark.oapi.service.im.v1.model.CreateFileReqBody.newBuilder()
                            .fileType(fileType)
                            .fileName(fileName)
                            .file(file)
                            .build())
                    .build();

            CreateFileResp resp = apiClient.im().v1().file().create(req);

            if (resp.success()) {
                String fileKey = resp.getData().getFileKey();
                log.info("文件上传成功 - FileKey: {}, 原始文件: {}", fileKey, filePath);
                return fileKey;
            } else {
                log.error("文件上传失败 - Code: {}, Msg: {}", resp.getCode(), resp.getMsg());
                throw new FeishuException(String.valueOf(resp.getCode()),
                        "文件上传失败: " + resp.getMsg());
            }
        } catch (FeishuException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传文件异常 - Path: {}", filePath, e);
            throw new FeishuException("上传文件异常", e);
        }
    }

    /**
     * 发送文件消息
     *
     * <p>通过 REST API 向指定用户发送文件。先上传文件获取 file_key，再发送文件消息。</p>
     *
     * @param openId  接收者的 openId
     * @param filePath 本地文件路径
     * @return 发送成功的消息ID
     */
    public String sendFileMessage(String openId, String filePath) {
        if (apiClient == null) {
            throw new FeishuException("飞书 API 客户端未初始化");
        }

        String fileKey = uploadFile(filePath);
        String fileName = new java.io.File(filePath).getName();

        try {
            Map<String, String> fileContent = new HashMap<>(1);
            fileContent.put("file_key", fileKey);
            String contentJson = objectMapper.writeValueAsString(fileContent);

            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType("open_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(openId)
                            .msgType("file")
                            .content(contentJson)
                            .uuid(java.util.UUID.randomUUID().toString())
                            .build())
                    .build();

            CreateMessageResp resp = apiClient.im().v1().message().create(req, RequestOptions.newBuilder().build());

            if (resp.success()) {
                String messageId = resp.getData().getMessageId();
                log.info("飞书文件消息发送成功 - MessageId: {}, File: {}", messageId, fileName);
                return messageId;
            } else {
                log.error("飞书文件消息发送失败 - Code: {}, Msg: {}", resp.getCode(), resp.getMsg());
                throw new FeishuException(String.valueOf(resp.getCode()),
                        "飞书文件消息发送失败: " + resp.getMsg());
            }
        } catch (FeishuException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送文件消息异常", e);
            throw new FeishuException("发送文件消息异常", e);
        }
    }

    private String resolveFileType(String extension) {
        return switch (extension) {
            case "pdf" -> "pdf";
            case "doc", "docx" -> "doc";
            case "xls", "xlsx" -> "xls";
            case "ppt", "pptx" -> "ppt";
            case "mp4" -> "mp4";
            case "opus" -> "opus";
            default -> "stream";
        };
    }
}

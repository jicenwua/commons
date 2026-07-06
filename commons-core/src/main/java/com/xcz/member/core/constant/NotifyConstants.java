package com.xcz.member.core.constant;

/**
 * 实时通知相关常量
 */
public final class NotifyConstants {

    private NotifyConstants() {
    }

    /** 工单新消息 Redis 广播 Topic */
    public static final String TICKET_MESSAGE_TOPIC = "ticket:message:topic";

    /** WebSocket 路径（app-notify 服务内路径，经 Gateway 前缀 /mobi） */
    public static final String WS_PATH = "/notify/ws";
}

package com.xcz.commons.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单消息实时通知事件（Redis Pub/Sub 载荷）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageNotifyEvent implements Serializable {

    public static final String TYPE_NEW_MESSAGE = "ticket_message";
    public static final String TYPE_UNREAD_CHANGED = "unread_changed";
    public static final String TYPE_STATUS_CHANGED = "ticket_status_changed";

    private String type;
    private Long ticketId;
    private Long messageId;
    private Integer senderType;
    private Long senderId;
    private String senderLabel;
    private String senderAvatar;
    private String content;
    private LocalDateTime createTime;

    /** 工单状态（状态变更事件） */
    private Integer status;
    /** 工单状态文案（状态变更事件） */
    private String statusLabel;

    /** 工单所属 C 端用户 */
    private Long ticketUserId;
    /** 当前认领客服 */
    private Long assignedStaffId;

    /** 推送给指定 C 端用户 */
    private Long targetCustomerUserId;
    /** 推送给指定后台客服 */
    private Long targetStaffId;
    /** 推送给所有在线客服（未认领工单） */
    private boolean notifyAllStaff;
}

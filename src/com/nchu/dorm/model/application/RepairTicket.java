package com.nchu.dorm.model.application;

import com.nchu.dorm.util.TextUtil;

/**
 * 维修申请单。学生提交，宿管科（或宿舍管理人员）处理。
 * 初版仅建立数据结构，处理闭环在后续迭代实现。
 */
public class RepairTicket {

    /** 状态：待处理 */
    public static final String STATUS_PENDING = "PENDING";

    /** 状态：处理中 */
    public static final String STATUS_PROCESSING = "PROCESSING";

    /** 状态：已处理完成 */
    public static final String STATUS_DONE = "DONE";

    /** 工单编号，如 RP0001 */
    private String id;

    /** 报修房间（楼栋-房间） */
    private String roomKey;

    /** 报修人学号 */
    private String reporterId;

    /** 维修内容描述 */
    private String description;

    /** 状态 */
    private String status;

    /** 提交时间 */
    private String createTime;

    /** 处理人工号 */
    private String handlerId;

    /** 处理时间 */
    private String handleTime;

    public RepairTicket() {
    }

    public RepairTicket(String id, String roomKey, String reporterId, String description,
                        String status, String createTime) {
        this.id = id;
        this.roomKey = roomKey;
        this.reporterId = reporterId;
        this.description = description;
        this.status = status;
        this.createTime = createTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomKey() {
        return roomKey;
    }

    public void setRoomKey(String roomKey) {
        this.roomKey = roomKey;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }

    public String getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(String handleTime) {
        this.handleTime = handleTime;
    }

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(roomKey) + "|"
                + TextUtil.escape(reporterId) + "|"
                + TextUtil.escape(description) + "|"
                + TextUtil.escape(status) + "|"
                + TextUtil.escape(createTime) + "|"
                + TextUtil.escape(handlerId) + "|"
                + TextUtil.escape(handleTime);
    }

    public static RepairTicket fromLine(String line) {
        String[] f = TextUtil.split(line);
        RepairTicket t = new RepairTicket();
        t.id = f[0];
        t.roomKey = f[1];
        t.reporterId = f[2];
        t.description = f[3];
        t.status = f[4];
        t.createTime = f[5];
        t.handlerId = f[6];
        t.handleTime = f[7];
        return t;
    }
}

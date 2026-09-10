package com.nchu.dorm.model.application;

import com.nchu.dorm.util.TextUtil;

/**
 * 维修申请单。学生提交后进入<b>分管宿舍管理人员</b>的接收队列，由宿管核实并统一上报宿管科，
 * 再由宿管科受理 / 办结。状态流转：
 * {@link #STATUS_SUBMITTED}（待宿管上报）→ {@link #STATUS_PENDING}（待处理）→
 * {@link #STATUS_PROCESSING}（处理中）→ {@link #STATUS_DONE}（已完成）。
 */
public class RepairTicket {

    /** 状态：待宿管上报（学生提交后的初始状态，尚未到达宿管科） */
    public static final String STATUS_SUBMITTED = "SUBMITTED";

    /** 状态：待处理（宿管已上报，宿管科待受理） */
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

    /** 上报宿管科的宿舍管理人员工号（未上报为空） */
    private String escalatorId;

    /** 上报宿管科的时间 */
    private String escalateTime;

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

    public String getEscalatorId() {
        return escalatorId;
    }

    public void setEscalatorId(String escalatorId) {
        this.escalatorId = escalatorId;
    }

    public String getEscalateTime() {
        return escalateTime;
    }

    public void setEscalateTime(String escalateTime) {
        this.escalateTime = escalateTime;
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

    /** 是否尚未上报宿管科（仍在分管宿管的接收队列中）。 */
    public boolean isSubmitted() {
        return STATUS_SUBMITTED.equals(status);
    }

    /** 状态中文名 */
    public String getStatusName() {
        if (STATUS_SUBMITTED.equals(status)) {
            return "待宿管上报";
        }
        if (STATUS_PROCESSING.equals(status)) {
            return "处理中";
        }
        if (STATUS_DONE.equals(status)) {
            return "已完成";
        }
        return "待处理";
    }

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(roomKey) + "|"
                + TextUtil.escape(reporterId) + "|"
                + TextUtil.escape(description) + "|"
                + TextUtil.escape(status) + "|"
                + TextUtil.escape(createTime) + "|"
                + TextUtil.escape(handlerId) + "|"
                + TextUtil.escape(handleTime) + "|"
                + TextUtil.escape(escalatorId) + "|"
                + TextUtil.escape(escalateTime);
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
        if (f.length > 7) {
            t.handleTime = f[7];
        }
        // 上报两列为本轮新增：旧数据行没有这两段，缺省即「已上报」（旧单曾直达宿管科）
        if (f.length > 8) {
            t.escalatorId = f[8];
        }
        if (f.length > 9) {
            t.escalateTime = f[9];
        }
        return t;
    }
}

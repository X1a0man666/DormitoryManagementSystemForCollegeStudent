package com.nchu.dorm.model.application;

import com.nchu.dorm.util.TextUtil;

/**
 * 宿舍申请（申请 / 转移 / 退出 三类共用）。
 * 学生发起，辅导员（本学院）审批；涉及换楼栋/退宿的后续迭代再由宿管科复核。
 */
public class DormApplication {

    /** 申请类型：入住申请 */
    public static final String TYPE_APPLY = "APPLY";

    /** 申请类型：转移宿舍 */
    public static final String TYPE_TRANSFER = "TRANSFER";

    /** 申请类型：退宿 */
    public static final String TYPE_EXIT = "EXIT";

    /** 状态：待审批 */
    public static final String STATUS_PENDING = "PENDING";

    /** 状态：已通过 */
    public static final String STATUS_APPROVED = "APPROVED";

    /** 状态：已驳回 */
    public static final String STATUS_REJECTED = "REJECTED";

    /** 申请编号，如 AP0001 */
    private String id;

    /** 申请学生学号 */
    private String studentId;

    /** 申请类型，见 TYPE_* 常量 */
    private String type;

    /** 申请目标楼栋 */
    private String targetBuilding;

    /** 申请目标房间（可空，由辅导员审批时分配） */
    private String targetRoom;

    /** 申请原因 */
    private String reason;

    /** 状态，见 STATUS_* 常量 */
    private String status;

    /** 提交时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;

    /** 审批人工号 */
    private String reviewerId;

    /** 审批时间 */
    private String reviewTime;

    /** 审批意见 */
    private String reviewComment;

    public DormApplication() {
    }

    public DormApplication(String id, String studentId, String type, String targetBuilding,
                           String reason, String status, String createTime) {
        this.id = id;
        this.studentId = studentId;
        this.type = type;
        this.targetBuilding = targetBuilding;
        this.reason = reason;
        this.status = status;
        this.createTime = createTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetBuilding() {
        return targetBuilding;
    }

    public void setTargetBuilding(String targetBuilding) {
        this.targetBuilding = targetBuilding;
    }

    public String getTargetRoom() {
        return targetRoom;
    }

    public void setTargetRoom(String targetRoom) {
        this.targetRoom = targetRoom;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(String reviewTime) {
        this.reviewTime = reviewTime;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    /** 类型中文名 */
    public String getTypeName() {
        switch (type) {
            case TYPE_APPLY:
                return "入住申请";
            case TYPE_TRANSFER:
                return "转移宿舍";
            case TYPE_EXIT:
                return "退宿申请";
            default:
                return type;
        }
    }

    /** 状态中文名 */
    public String getStatusName() {
        switch (status) {
            case STATUS_PENDING:
                return "待审批";
            case STATUS_APPROVED:
                return "已通过";
            case STATUS_REJECTED:
                return "已驳回";
            default:
                return status;
        }
    }

    /**
     * 文本序列化。
     * id|studentId|type|targetBuilding|targetRoom|reason|status|createTime|reviewerId|reviewTime|reviewComment
     */
    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(studentId) + "|"
                + TextUtil.escape(type) + "|"
                + TextUtil.escape(targetBuilding) + "|"
                + TextUtil.escape(targetRoom) + "|"
                + TextUtil.escape(reason) + "|"
                + TextUtil.escape(status) + "|"
                + TextUtil.escape(createTime) + "|"
                + TextUtil.escape(reviewerId) + "|"
                + TextUtil.escape(reviewTime) + "|"
                + TextUtil.escape(reviewComment);
    }

    public static DormApplication fromLine(String line) {
        String[] f = TextUtil.split(line);
        DormApplication a = new DormApplication();
        a.id = f[0];
        a.studentId = f[1];
        a.type = f[2];
        a.targetBuilding = f[3];
        a.targetRoom = f[4];
        a.reason = f[5];
        a.status = f[6];
        a.createTime = f[7];
        a.reviewerId = f[8];
        a.reviewTime = f[9];
        a.reviewComment = f[10];
        return a;
    }
}

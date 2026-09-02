package com.nchu.dorm.model.application;

import com.nchu.dorm.util.TextUtil;

/**
 * 宿舍申请（入住 / 转移 / 退宿 / 转专业换宿 四类共用）。
 * 学生发起，辅导员审批：
 * <ul>
 *   <li>入住(APPLY)/转移(TRANSFER)/退宿(EXIT)：本学院辅导员单级审批，通过时执行床位/宿舍数据变更。</li>
 *   <li>转专业换宿(MAJOR_TRANSFER)：两级审批——先由本专业辅导员同意迁出，再由目标专业辅导员同意接收，
 *       两级均同意才成功，任一级拒绝即失败。跨学院转专业才需要更换宿舍楼（目标学院同性别楼）。</li>
 * </ul>
 * 状态：PENDING（待审批）/ AWAITING_TARGET（转专业·已同意迁出，待目标专业审批）/
 * APPROVED（已通过）/ REJECTED（已驳回）/ CANCELLED（已撤销）。
 */
public class DormApplication {

    /** 申请类型：入住申请 */
    public static final String TYPE_APPLY = "APPLY";

    /** 申请类型：转移宿舍 */
    public static final String TYPE_TRANSFER = "TRANSFER";

    /** 申请类型：退宿 */
    public static final String TYPE_EXIT = "EXIT";

    /** 申请类型：转专业换宿（两级审批） */
    public static final String TYPE_MAJOR_TRANSFER = "MAJOR_TRANSFER";

    /** 状态：待审批（单级类型=待本院辅导员；转专业=待本专业辅导员同意迁出） */
    public static final String STATUS_PENDING = "PENDING";

    /** 状态：已同意迁出，待目标专业辅导员审批（仅转专业换宿使用） */
    public static final String STATUS_AWAITING_TARGET = "AWAITING_TARGET";

    /** 状态：已通过 */
    public static final String STATUS_APPROVED = "APPROVED";

    /** 状态：已驳回 */
    public static final String STATUS_REJECTED = "REJECTED";

    /** 状态：已撤销 */
    public static final String STATUS_CANCELLED = "CANCELLED";

    /** 申请编号，如 AP0001 */
    private String id;

    /** 申请学生学号 */
    private String studentId;

    /** 申请类型，见 TYPE_* 常量 */
    private String type;

    /** 申请目标楼栋（退宿为空；同学院转专业不换房时也为空） */
    private String targetBuilding;

    /** 申请目标房间（可空，由辅导员审批时分配） */
    private String targetRoom;

    /** 申请原因 */
    private String reason;

    /** 状态，见 STATUS_* 常量 */
    private String status;

    /** 提交时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;

    /** 最后处理人（单级=本院辅导员；转专业=目标专业辅导员）工号 */
    private String reviewerId;

    /** 最后处理时间 */
    private String reviewTime;

    /** 最后处理意见 */
    private String reviewComment;

    /** 提交时现居楼栋快照（转移/退宿/转专业用） */
    private String originBuilding;

    /** 提交时现居房间快照（转移/退宿/转专业用） */
    private String originRoom;

    /** 转专业目标班级编码（学号前 6 位，如 251613；非转专业为空） */
    private String targetClass;

    /** 提交时原班级编码（转专业成功后学生档案会被改写，需快照供追溯与历史归口） */
    private String originClass;

    /** 转专业第一级：同意迁出的本专业辅导员 id */
    private String step1ReviewerId;

    /** 转专业第一级：同意迁出时间 */
    private String step1ReviewTime;

    /** 转专业第一级：同意迁出意见 */
    private String step1ReviewComment;

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

    public String getOriginBuilding() {
        return originBuilding;
    }

    public void setOriginBuilding(String originBuilding) {
        this.originBuilding = originBuilding;
    }

    public String getOriginRoom() {
        return originRoom;
    }

    public void setOriginRoom(String originRoom) {
        this.originRoom = originRoom;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public String getOriginClass() {
        return originClass;
    }

    public void setOriginClass(String originClass) {
        this.originClass = originClass;
    }

    public String getStep1ReviewerId() {
        return step1ReviewerId;
    }

    public void setStep1ReviewerId(String step1ReviewerId) {
        this.step1ReviewerId = step1ReviewerId;
    }

    public String getStep1ReviewTime() {
        return step1ReviewTime;
    }

    public void setStep1ReviewTime(String step1ReviewTime) {
        this.step1ReviewTime = step1ReviewTime;
    }

    public String getStep1ReviewComment() {
        return step1ReviewComment;
    }

    public void setStep1ReviewComment(String step1ReviewComment) {
        this.step1ReviewComment = step1ReviewComment;
    }

    /** 类型中文名 */
    public String getTypeName() {
        if (TYPE_MAJOR_TRANSFER.equals(type)) {
            return "转专业换宿";
        }
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
            case STATUS_AWAITING_TARGET:
                return "待目标专业审批";
            case STATUS_APPROVED:
                return "已通过";
            case STATUS_REJECTED:
                return "已驳回";
            case STATUS_CANCELLED:
                return "已撤销";
            default:
                return status;
        }
    }

    /** 是否仍处于"在办/可撤销"状态（含转专业等待目标审批的阶段） */
    public boolean isPendingLike() {
        return STATUS_PENDING.equals(status) || STATUS_AWAITING_TARGET.equals(status);
    }

    /** 是否终态 */
    public boolean isTerminal() {
        return STATUS_APPROVED.equals(status) || STATUS_REJECTED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    /**
     * 文本序列化（18 列）。
     * id|studentId|type|targetBuilding|targetRoom|reason|status|createTime|reviewerId|reviewTime|reviewComment
     *   |originBuilding|originRoom|targetClass|originClass|step1ReviewerId|step1ReviewTime|step1ReviewComment
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
                + TextUtil.escape(reviewComment) + "|"
                + TextUtil.escape(originBuilding) + "|"
                + TextUtil.escape(originRoom) + "|"
                + TextUtil.escape(targetClass) + "|"
                + TextUtil.escape(originClass) + "|"
                + TextUtil.escape(step1ReviewerId) + "|"
                + TextUtil.escape(step1ReviewTime) + "|"
                + TextUtil.escape(step1ReviewComment);
    }

    public static DormApplication fromLine(String line) {
        String[] f = TextUtil.split(line);
        DormApplication a = new DormApplication();
        a.id = col(f, 0);
        a.studentId = col(f, 1);
        a.type = col(f, 2);
        a.targetBuilding = col(f, 3);
        a.targetRoom = col(f, 4);
        a.reason = col(f, 5);
        a.status = col(f, 6);
        a.createTime = col(f, 7);
        a.reviewerId = col(f, 8);
        a.reviewTime = col(f, 9);
        a.reviewComment = col(f, 10);
        // 迭代四新增列：旧 11 列数据向后兼容（越界取 null）
        a.originBuilding = opt(col(f, 11));
        a.originRoom = opt(col(f, 12));
        a.targetClass = opt(col(f, 13));
        a.originClass = opt(col(f, 14));
        a.step1ReviewerId = opt(col(f, 15));
        a.step1ReviewTime = opt(col(f, 16));
        a.step1ReviewComment = opt(col(f, 17));
        return a;
    }

    private static String col(String[] f, int idx) {
        return idx < f.length ? f[idx] : null;
    }

    /** 空串规整为 null，便于新列空值统一用 null 判断 */
    private static String opt(String v) {
        return v == null || v.isEmpty() ? null : v;
    }
}

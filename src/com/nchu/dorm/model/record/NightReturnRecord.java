package com.nchu.dorm.model.record;

import com.nchu.dorm.util.TextUtil;

/**
 * 夜归记录（宿舍管理人员日常管理）。
 * 登记学生晚归的时间、原因；登记后由学生在学生端确认「已到寝」或提出异议
 * （状态与学生说明见 {@link #getConfirmStatus()} / {@link #getStudentRemark()}）。
 * <p>学生提出异议后由<b>分管宿管复核</b>收尾：维持原登记或更正后通过，
 * 复核结论与意见见 {@link #getReviewStatus()} 相关字段；复核后记录终结，学生不可再议。</p>
 */
public class NightReturnRecord {

    /** 确认状态：待学生确认（登记后的初始状态） */
    public static final String CONFIRM_PENDING = "PENDING";

    /** 确认状态：学生已确认到寝 */
    public static final String CONFIRM_CONFIRMED = "CONFIRMED";

    /** 确认状态：学生有异议（如登记的时间/原因与实际不符） */
    public static final String CONFIRM_DISPUTED = "DISPUTED";

    /** 复核结论：异议不成立，维持原登记（终态） */
    public static final String REVIEW_KEPT = "REVIEWED_KEPT";

    /** 复核结论：异议成立，宿管已更正登记内容（终态） */
    public static final String REVIEW_ADJUSTED = "REVIEWED_ADJUSTED";

    /** 记录编号，如 NR0001 */
    private String id;

    /** 学生学号 */
    private String studentId;

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 夜归时间 HH:mm */
    private String returnTime;

    /** 夜归原因 */
    private String reason;

    /** 学生确认状态，见 CONFIRM_* 常量 */
    private String confirmStatus = CONFIRM_PENDING;

    /** 学生确认/异议时间 yyyy-MM-dd HH:mm:ss */
    private String confirmTime;

    /** 学生异议说明（确认到寝时为空） */
    private String studentRemark;

    /** 复核人工号（分管宿管；未复核为空） */
    private String reviewerId;

    /** 复核时间 yyyy-MM-dd HH:mm:ss */
    private String reviewTime;

    /** 复核意见（宿管对异议的说明） */
    private String reviewComment;

    public NightReturnRecord() {
    }

    public NightReturnRecord(String id, String studentId, String date, String returnTime, String reason) {
        this.id = id;
        this.studentId = studentId;
        this.date = date;
        this.returnTime = returnTime;
        this.reason = reason;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(String returnTime) {
        this.returnTime = returnTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getConfirmStatus() {
        return confirmStatus;
    }

    public void setConfirmStatus(String confirmStatus) {
        this.confirmStatus = confirmStatus == null || confirmStatus.isEmpty()
                ? CONFIRM_PENDING : confirmStatus;
    }

    public String getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(String confirmTime) {
        this.confirmTime = confirmTime;
    }

    public String getStudentRemark() {
        return studentRemark;
    }

    public void setStudentRemark(String studentRemark) {
        this.studentRemark = studentRemark;
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

    /** 是否仍待学生确认。 */
    public boolean isPendingConfirm() {
        return confirmStatus == null || CONFIRM_PENDING.equals(confirmStatus);
    }

    /** 学生是否已提出异议且尚待宿管复核。 */
    public boolean needsReview() {
        return CONFIRM_DISPUTED.equals(confirmStatus);
    }

    /** 是否已由宿管复核（终态：维持原登记 / 已更正）。 */
    public boolean isReviewed() {
        return REVIEW_KEPT.equals(confirmStatus) || REVIEW_ADJUSTED.equals(confirmStatus);
    }

    /** 确认状态中文名。 */
    public String getConfirmStatusName() {
        if (CONFIRM_CONFIRMED.equals(confirmStatus)) {
            return "已确认到寝";
        }
        if (CONFIRM_DISPUTED.equals(confirmStatus)) {
            return "有异议";
        }
        if (REVIEW_KEPT.equals(confirmStatus)) {
            return "已复核·维持原记录";
        }
        if (REVIEW_ADJUSTED.equals(confirmStatus)) {
            return "已复核·已更正";
        }
        return "待确认";
    }

    /** 学生确认状态 + 说明 + 复核结论的展示文案，供学生端与宿管端表格使用。 */
    public String getConfirmSummary() {
        String name = getConfirmStatusName();
        StringBuilder sb = new StringBuilder(
                studentRemark == null || studentRemark.isEmpty() ? name : name + "：" + studentRemark);
        if (isReviewed() && reviewComment != null && !reviewComment.isEmpty()) {
            sb.append("（").append(reviewComment).append("）");
        }
        return sb.toString();
    }

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(studentId) + "|"
                + TextUtil.escape(date) + "|"
                + TextUtil.escape(returnTime) + "|"
                + TextUtil.escape(reason) + "|"
                + TextUtil.escape(confirmStatus) + "|"
                + TextUtil.escape(confirmTime) + "|"
                + TextUtil.escape(studentRemark) + "|"
                + TextUtil.escape(reviewerId) + "|"
                + TextUtil.escape(reviewTime) + "|"
                + TextUtil.escape(reviewComment);
    }

    public static NightReturnRecord fromLine(String line) {
        String[] f = TextUtil.split(line);
        NightReturnRecord r = new NightReturnRecord();
        r.id = f[0];
        r.studentId = f[1];
        r.date = f[2];
        r.returnTime = f[3];
        r.reason = f[4];
        // 确认相关三列为本轮新增：旧数据行没有这三段，缺省即「待确认」
        if (f.length > 5) {
            r.setConfirmStatus(f[5]);
        }
        if (f.length > 6) {
            r.confirmTime = f[6];
        }
        if (f.length > 7) {
            r.studentRemark = f[7];
        }
        // 复核三列为本轮新增：旧数据行没有这三段，缺省即「未复核」
        if (f.length > 8) {
            r.reviewerId = f[8];
        }
        if (f.length > 9) {
            r.reviewTime = f[9];
        }
        if (f.length > 10) {
            r.reviewComment = f[10];
        }
        return r;
    }
}

package com.nchu.dorm.model.record;

import com.nchu.dorm.util.TextUtil;

/**
 * 卫生检查记录（宿舍管理人员日常管理）。
 * 记录某房间在某次卫生检查中的得分与评语；检查结果由该寝室学生在学生端查看（历史可查），
 * 并可反馈「无异议」或「有问题」（见 FEEDBACK_* 与学生反馈字段）。
 * <p>学生反馈「有问题」后由<b>分管宿管复核</b>收尾：维持原记录或更正得分/评语后通过，
 * 复核结论与意见见 REVIEW_* 与复核字段；复核后记录终结，学生不可再反馈。</p>
 */
public class HygieneRecord {

    /** 学生反馈：无异议 */
    public static final String FEEDBACK_ACKNOWLEDGED = "ACKNOWLEDGED";

    /** 学生反馈：有问题 */
    public static final String FEEDBACK_DISPUTED = "DISPUTED";

    /** 复核结论：反馈不成立，维持原记录（终态） */
    public static final String REVIEW_KEPT = "REVIEWED_KEPT";

    /** 复核结论：反馈成立，宿管已更正得分/评语（终态） */
    public static final String REVIEW_ADJUSTED = "REVIEWED_ADJUSTED";

    /** 记录编号，如 HG0001 */
    private String id;

    /** 房间（楼栋-房间） */
    private String roomKey;

    /** 检查日期 yyyy-MM-dd */
    private String date;

    /** 卫生得分（百分制） */
    private double score;

    /** 检查人工号 */
    private String inspectorId;

    /** 评语 */
    private String comment;

    /** 反馈学生学号（空表示尚无学生反馈） */
    private String studentId;

    /** 学生反馈状态，见 FEEDBACK_* 常量；空表示未反馈 */
    private String studentStatus;

    /** 学生反馈说明（无异议时为空） */
    private String studentRemark;

    /** 反馈时间 yyyy-MM-dd HH:mm:ss */
    private String feedbackTime;

    /** 复核人工号（分管宿管；未复核为空） */
    private String reviewerId;

    /** 复核时间 yyyy-MM-dd HH:mm:ss */
    private String reviewTime;

    /** 复核意见（宿管对反馈的说明） */
    private String reviewComment;

    public HygieneRecord() {
    }

    public HygieneRecord(String id, String roomKey, String date, double score,
                         String inspectorId, String comment) {
        this.id = id;
        this.roomKey = roomKey;
        this.date = date;
        this.score = score;
        this.inspectorId = inspectorId;
        this.comment = comment;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getInspectorId() {
        return inspectorId;
    }

    public void setInspectorId(String inspectorId) {
        this.inspectorId = inspectorId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentStatus() {
        return studentStatus;
    }

    public void setStudentStatus(String studentStatus) {
        this.studentStatus = studentStatus;
    }

    public String getStudentRemark() {
        return studentRemark;
    }

    public void setStudentRemark(String studentRemark) {
        this.studentRemark = studentRemark;
    }

    public String getFeedbackTime() {
        return feedbackTime;
    }

    public void setFeedbackTime(String feedbackTime) {
        this.feedbackTime = feedbackTime;
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

    /** 学生是否已作出反馈（「无异议」或「有问题」）；复核态不再算作学生反馈。 */
    public boolean hasStudentFeedback() {
        return FEEDBACK_ACKNOWLEDGED.equals(studentStatus) || FEEDBACK_DISPUTED.equals(studentStatus);
    }

    /** 学生是否已反馈「有问题」且尚待宿管复核。 */
    public boolean needsReview() {
        return FEEDBACK_DISPUTED.equals(studentStatus);
    }

    /** 是否已由宿管复核（终态：维持原记录 / 已更正）。 */
    public boolean isReviewed() {
        return REVIEW_KEPT.equals(studentStatus) || REVIEW_ADJUSTED.equals(studentStatus);
    }

    /** 学生反馈中文名（未反馈返回「待反馈」）。 */
    public String getStudentStatusName() {
        if (FEEDBACK_ACKNOWLEDGED.equals(studentStatus)) {
            return "无异议";
        }
        if (FEEDBACK_DISPUTED.equals(studentStatus)) {
            return "有问题";
        }
        if (REVIEW_KEPT.equals(studentStatus)) {
            return "已复核·维持原记录";
        }
        if (REVIEW_ADJUSTED.equals(studentStatus)) {
            return "已复核·已更正";
        }
        return "待反馈";
    }

    /** 学生反馈 + 说明 + 复核结论的展示文案，供学生端与宿管端表格使用。 */
    public String getStudentSummary() {
        String name = getStudentStatusName();
        StringBuilder sb = new StringBuilder(
                studentRemark == null || studentRemark.isEmpty() ? name : name + "：" + studentRemark);
        if (isReviewed() && reviewComment != null && !reviewComment.isEmpty()) {
            sb.append("（").append(reviewComment).append("）");
        }
        return sb.toString();
    }

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(roomKey) + "|"
                + TextUtil.escape(date) + "|"
                + score + "|"
                + TextUtil.escape(inspectorId) + "|"
                + TextUtil.escape(comment) + "|"
                + TextUtil.escape(studentId) + "|"
                + TextUtil.escape(studentStatus) + "|"
                + TextUtil.escape(studentRemark) + "|"
                + TextUtil.escape(feedbackTime) + "|"
                + TextUtil.escape(reviewerId) + "|"
                + TextUtil.escape(reviewTime) + "|"
                + TextUtil.escape(reviewComment);
    }

    public static HygieneRecord fromLine(String line) {
        String[] f = TextUtil.split(line);
        HygieneRecord r = new HygieneRecord();
        r.id = f[0];
        r.roomKey = f[1];
        r.date = f[2];
        r.score = Double.parseDouble(f[3]);
        r.inspectorId = f[4];
        r.comment = f[5];
        // 学生反馈四列为本轮新增：旧数据行缺省即「未反馈」
        if (f.length > 6) {
            r.studentId = f[6];
        }
        if (f.length > 7) {
            r.studentStatus = f[7];
        }
        if (f.length > 8) {
            r.studentRemark = f[8];
        }
        if (f.length > 9) {
            r.feedbackTime = f[9];
        }
        // 复核三列为本轮新增：旧数据行没有这三段，缺省即「未复核」
        if (f.length > 10) {
            r.reviewerId = f[10];
        }
        if (f.length > 11) {
            r.reviewTime = f[11];
        }
        if (f.length > 12) {
            r.reviewComment = f[12];
        }
        return r;
    }
}

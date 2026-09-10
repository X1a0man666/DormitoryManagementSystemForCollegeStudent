package com.nchu.dorm.model.record;

import com.nchu.dorm.util.TextUtil;

/**
 * 贵重物品出入登记（宿舍管理人员日常管理）。
 * 登记学生携带贵重物品进出楼栋的情况；登记后由学生在学生端确认「未丢失」，
 * 若遗失则学生报损 → 分管宿管上报宿管科 → 宿管科发布全校公告
 * （确认状态见 CONFIRM_*，上报链路见 REPORT_*）。
 */
public class ValuablesRecord {

    /** 方向：带出 */
    public static final String DIRECTION_OUT = "OUT";

    /** 方向：带入 */
    public static final String DIRECTION_IN = "IN";

    /** 确认状态：待学生确认 */
    public static final String CONFIRM_PENDING = "PENDING";

    /** 确认状态：学生已确认未丢失 */
    public static final String CONFIRM_INTACT = "INTACT";

    /** 确认状态：学生报告已遗失 */
    public static final String CONFIRM_LOST = "LOST";

    /** 上报状态：未涉及遗失上报 */
    public static final String REPORT_NONE = "NONE";

    /** 上报状态：学生已报遗失，待分管宿管核实上报 */
    public static final String REPORT_STUDENT_REPORTED = "STUDENT_REPORTED";

    /** 上报状态：分管宿管已上报宿管科，待宿管科发布公告 */
    public static final String REPORT_ESCALATED = "ESCALATED";

    /** 上报状态：宿管科已发布公告 */
    public static final String REPORT_PUBLISHED = "PUBLISHED";

    /** 记录编号，如 VA0001 */
    private String id;

    /** 学生学号 */
    private String studentId;

    /** 物品名称 */
    private String itemName;

    /** 出入方向，见 DIRECTION_* 常量 */
    private String direction;

    /** 日期时间 yyyy-MM-dd HH:mm */
    private String recordTime;

    /** 登记人工号 */
    private String handlerId;

    /** 学生确认状态，见 CONFIRM_* 常量 */
    private String confirmStatus = CONFIRM_PENDING;

    /** 学生确认/报损时间 yyyy-MM-dd HH:mm:ss */
    private String confirmTime;

    /** 学生说明：确认时的备注或遗失情况说明 */
    private String studentRemark;

    /** 遗失上报状态，见 REPORT_* 常量 */
    private String reportStatus = REPORT_NONE;

    /** 上报宿管科的宿管工号 */
    private String reporterId;

    /** 宿管上报时间 yyyy-MM-dd HH:mm:ss */
    private String reportTime;

    public ValuablesRecord() {
    }

    public ValuablesRecord(String id, String studentId, String itemName, String direction,
                           String recordTime, String handlerId) {
        this.id = id;
        this.studentId = studentId;
        this.itemName = itemName;
        this.direction = direction;
        this.recordTime = recordTime;
        this.handlerId = handlerId;
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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(String recordTime) {
        this.recordTime = recordTime;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
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

    public String getReportStatus() {
        return reportStatus;
    }

    public void setReportStatus(String reportStatus) {
        this.reportStatus = reportStatus == null || reportStatus.isEmpty()
                ? REPORT_NONE : reportStatus;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public String getReportTime() {
        return reportTime;
    }

    public void setReportTime(String reportTime) {
        this.reportTime = reportTime;
    }

    /** 方向中文名 */
    public String getDirectionName() {
        return DIRECTION_IN.equals(direction) ? "带入" : "带出";
    }

    /** 学生确认状态中文名。 */
    public String getConfirmStatusName() {
        if (CONFIRM_INTACT.equals(confirmStatus)) {
            return "未丢失";
        }
        if (CONFIRM_LOST.equals(confirmStatus)) {
            return "已遗失";
        }
        return "待确认";
    }

    /** 遗失上报状态中文名。 */
    public String getReportStatusName() {
        if (REPORT_STUDENT_REPORTED.equals(reportStatus)) {
            return "待宿管上报";
        }
        if (REPORT_ESCALATED.equals(reportStatus)) {
            return "已上报宿管科";
        }
        if (REPORT_PUBLISHED.equals(reportStatus)) {
            return "已发布公告";
        }
        return "—";
    }

    /** 确认与上报状态的合并展示文案，供列表使用。 */
    public String getStatusSummary() {
        String name = getConfirmStatusName();
        if (REPORT_NONE.equals(reportStatus)) {
            return name;
        }
        return name + " / " + getReportStatusName();
    }

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(studentId) + "|"
                + TextUtil.escape(itemName) + "|"
                + TextUtil.escape(direction) + "|"
                + TextUtil.escape(recordTime) + "|"
                + TextUtil.escape(handlerId) + "|"
                + TextUtil.escape(confirmStatus) + "|"
                + TextUtil.escape(confirmTime) + "|"
                + TextUtil.escape(studentRemark) + "|"
                + TextUtil.escape(reportStatus) + "|"
                + TextUtil.escape(reporterId) + "|"
                + TextUtil.escape(reportTime);
    }

    public static ValuablesRecord fromLine(String line) {
        String[] f = TextUtil.split(line);
        ValuablesRecord r = new ValuablesRecord();
        r.id = f[0];
        r.studentId = f[1];
        r.itemName = f[2];
        r.direction = f[3];
        r.recordTime = f[4];
        r.handlerId = f[5];
        // 确认与上报六列为本轮新增：旧数据行缺省即「待确认 / 未上报」
        if (f.length > 6) {
            r.setConfirmStatus(f[6]);
        }
        if (f.length > 7) {
            r.confirmTime = f[7];
        }
        if (f.length > 8) {
            r.studentRemark = f[8];
        }
        if (f.length > 9) {
            r.setReportStatus(f[9]);
        }
        if (f.length > 10) {
            r.reporterId = f[10];
        }
        if (f.length > 11) {
            r.reportTime = f[11];
        }
        return r;
    }
}

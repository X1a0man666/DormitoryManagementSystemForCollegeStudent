package com.nchu.dorm.model.record;

import com.nchu.dorm.util.TextUtil;

/**
 * 夜归记录（宿舍管理人员日常管理）。
 * 登记学生晚归的时间、原因。
 */
public class NightReturnRecord {

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

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(studentId) + "|"
                + TextUtil.escape(date) + "|"
                + TextUtil.escape(returnTime) + "|"
                + TextUtil.escape(reason);
    }

    public static NightReturnRecord fromLine(String line) {
        String[] f = TextUtil.split(line);
        NightReturnRecord r = new NightReturnRecord();
        r.id = f[0];
        r.studentId = f[1];
        r.date = f[2];
        r.returnTime = f[3];
        r.reason = f[4];
        return r;
    }
}

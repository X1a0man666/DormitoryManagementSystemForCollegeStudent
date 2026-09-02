package com.nchu.dorm.model.record;

import com.nchu.dorm.util.TextUtil;

/**
 * 贵重物品出入登记（宿舍管理人员日常管理）。
 * 登记学生携带贵重物品进出楼栋的情况。
 */
public class ValuablesRecord {

    /** 方向：带出 */
    public static final String DIRECTION_OUT = "OUT";

    /** 方向：带入 */
    public static final String DIRECTION_IN = "IN";

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

    /** 方向中文名 */
    public String getDirectionName() {
        return DIRECTION_IN.equals(direction) ? "带入" : "带出";
    }

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(studentId) + "|"
                + TextUtil.escape(itemName) + "|"
                + TextUtil.escape(direction) + "|"
                + TextUtil.escape(recordTime) + "|"
                + TextUtil.escape(handlerId);
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
        return r;
    }
}

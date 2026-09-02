package com.nchu.dorm.model.record;

import com.nchu.dorm.util.TextUtil;

/**
 * 卫生检查记录（宿舍管理人员日常管理）。
 * 记录某房间在某次卫生检查中的得分与评语。
 */
public class HygieneRecord {

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

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(roomKey) + "|"
                + TextUtil.escape(date) + "|"
                + score + "|"
                + TextUtil.escape(inspectorId) + "|"
                + TextUtil.escape(comment);
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
        return r;
    }
}

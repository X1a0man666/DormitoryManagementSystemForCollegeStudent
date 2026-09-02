package com.nchu.dorm.model;

/**
 * 床位（宿舍信息"楼栋-房间-床位"组合链的最末级）。
 * 组合（has-a）关系：{@link Room} 由若干 {@link Bed} 组成。
 */
public class Bed {

    /** 床位号，从 1 开始 */
    private int bedNo;

    /** 入住学生学号，空床为 null */
    private String occupantId;

    public Bed() {
    }

    public Bed(int bedNo) {
        this.bedNo = bedNo;
    }

    public int getBedNo() {
        return bedNo;
    }

    public void setBedNo(int bedNo) {
        this.bedNo = bedNo;
    }

    public String getOccupantId() {
        return occupantId;
    }

    public void setOccupantId(String occupantId) {
        this.occupantId = occupantId;
    }

    public boolean isEmpty() {
        return occupantId == null || occupantId.isEmpty();
    }
}

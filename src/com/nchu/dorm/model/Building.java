package com.nchu.dorm.model;

import com.nchu.dorm.util.TextUtil;

/**
 * 宿舍楼栋信息（对应南昌航空大学真实楼栋）。
 * 依据调研：全校共 32 栋标准学生公寓，1-6 栋无独立卫浴，7-28 栋有独立卫浴，25 栋为上床下桌。
 */
public class Building {

    /** 楼栋名称，如 "1栋"、"天清苑" */
    private String name;

    /** 别名（如"天清苑"），可为空 */
    private String alias;

    /** 所属学院代码，未分配为空 */
    private String collegeCode;

    /** 分管宿舍管理人员工号，未指定为空 */
    private String managerId;

    /** 层数 */
    private int floorCount;

    /** 是否有独立卫浴（真实信息：1-6 栋无，7-28 栋有） */
    private boolean hasBathroom;

    public Building() {
    }

    public Building(String name, String alias, String collegeCode, String managerId,
                    int floorCount, boolean hasBathroom) {
        this.name = name;
        this.alias = alias;
        this.collegeCode = collegeCode;
        this.managerId = managerId;
        this.floorCount = floorCount;
        this.hasBathroom = hasBathroom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCollegeCode() {
        return collegeCode;
    }

    public void setCollegeCode(String collegeCode) {
        this.collegeCode = collegeCode;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public int getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(int floorCount) {
        this.floorCount = floorCount;
    }

    public boolean isHasBathroom() {
        return hasBathroom;
    }

    public void setHasBathroom(boolean hasBathroom) {
        this.hasBathroom = hasBathroom;
    }

    /** 文本序列化：name|alias|collegeCode|managerId|floorCount|hasBathroom */
    public String toLine() {
        return TextUtil.escape(name) + "|"
                + TextUtil.escape(alias) + "|"
                + TextUtil.escape(collegeCode) + "|"
                + TextUtil.escape(managerId) + "|"
                + floorCount + "|"
                + hasBathroom;
    }

    public static Building fromLine(String line) {
        String[] f = TextUtil.split(line);
        Building b = new Building();
        b.name = f[0];
        b.alias = f[1];
        b.collegeCode = f[2];
        b.managerId = f[3];
        b.floorCount = Integer.parseInt(f[4]);
        b.hasBathroom = Boolean.parseBoolean(f[5]);
        return b;
    }

    @Override
    public String toString() {
        return name;
    }
}

package com.nchu.dorm.model.application;

import com.nchu.dorm.util.TextUtil;

/**
 * 购电记录。学生发起购电，宿管科负责售电。
 * 初版仅建立数据结构，售电闭环在后续迭代实现。
 */
public class ElectricityPurchase {

    /** 购电记录编号，如 EL0001 */
    private String id;

    /** 房间（楼栋-房间） */
    private String roomKey;

    /** 购买人学号 */
    private String buyerId;

    /** 购电度数 */
    private double degree;

    /** 单价（元/度） */
    private double unitPrice;

    /** 应付金额（元） */
    private double amount;

    /** 购买时间 */
    private String createTime;

    public ElectricityPurchase() {
    }

    public ElectricityPurchase(String id, String roomKey, String buyerId, double degree,
                               double unitPrice, double amount, String createTime) {
        this.id = id;
        this.roomKey = roomKey;
        this.buyerId = buyerId;
        this.degree = degree;
        this.unitPrice = unitPrice;
        this.amount = amount;
        this.createTime = createTime;
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

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public double getDegree() {
        return degree;
    }

    public void setDegree(double degree) {
        this.degree = degree;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(roomKey) + "|"
                + TextUtil.escape(buyerId) + "|"
                + degree + "|"
                + unitPrice + "|"
                + amount + "|"
                + TextUtil.escape(createTime);
    }

    public static ElectricityPurchase fromLine(String line) {
        String[] f = TextUtil.split(line);
        ElectricityPurchase p = new ElectricityPurchase();
        p.id = f[0];
        p.roomKey = f[1];
        p.buyerId = f[2];
        p.degree = Double.parseDouble(f[3]);
        p.unitPrice = Double.parseDouble(f[4]);
        p.amount = Double.parseDouble(f[5]);
        p.createTime = f[6];
        return p;
    }
}

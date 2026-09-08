package com.nchu.dorm.model.application;

import com.nchu.dorm.util.TextUtil;

/**
 * 购电记录。学生发起购电，宿管科负责售电。
 * <p>闭环：学生提交 → 状态 PENDING（待售电）→ 宿管科售电 → 状态 PAID（已售）、房间电表到账。</p>
 */
public class ElectricityPurchase {

    /** 状态：待售电（学生已提交，宿管科未收费） */
    public static final String STATUS_PENDING = "PENDING";

    /** 状态：已售（宿管科已收费，房间电表已增加对应电量） */
    public static final String STATUS_PAID = "PAID";

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

    /** 状态，见 STATUS_* 常量 */
    private String status;

    /** 售电处理人工号 */
    private String handlerId;

    /** 售电处理时间 */
    private String handleTime;

    public ElectricityPurchase() {
    }

    public ElectricityPurchase(String id, String roomKey, String buyerId, double degree,
                               double unitPrice, double amount, String createTime, String status) {
        this.id = id;
        this.roomKey = roomKey;
        this.buyerId = buyerId;
        this.degree = degree;
        this.unitPrice = unitPrice;
        this.amount = amount;
        this.createTime = createTime;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }

    public String getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(String handleTime) {
        this.handleTime = handleTime;
    }

    /** 状态中文名 */
    public String getStatusName() {
        if (STATUS_PAID.equals(status)) {
            return "已售";
        }
        return "待售电";
    }

    /** 是否仍待售电 */
    public boolean isPending() {
        return !STATUS_PAID.equals(status);
    }

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(roomKey) + "|"
                + TextUtil.escape(buyerId) + "|"
                + degree + "|"
                + unitPrice + "|"
                + amount + "|"
                + TextUtil.escape(createTime) + "|"
                + TextUtil.escape(status) + "|"
                + TextUtil.escape(handlerId) + "|"
                + TextUtil.escape(handleTime);
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
        // 迭代五新增列：旧 7 列数据向后兼容
        p.status = f.length > 7 && !f[7].isEmpty() ? f[7] : ElectricityPurchase.STATUS_PENDING;
        p.handlerId = f.length > 8 ? f[8] : null;
        p.handleTime = f.length > 9 ? f[9] : null;
        return p;
    }
}

package com.nchu.dorm.service;

import com.nchu.dorm.model.Admin;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.application.ElectricityPurchase;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.util.BusinessException;
import com.nchu.dorm.util.FormatUtil;
import com.nchu.dorm.util.TimeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 购电 / 售电服务。
 * <p>
 * 闭环：学生（已入住本房间）提交购电 → 记录 PENDING（待售电）→
 * 宿管科售电：确认收费、给房间电表增加电量、记录转 PAID（已售）。
 * </p>
 */
public class ElectricityService {

    /** 单价（元/度）。 */
    public static final double UNIT_PRICE = 0.60;

    /** 单次购电度数上限。 */
    public static final double MAX_DEGREE = 2000;

    private DataCenter dc() {
        return DataCenter.instance();
    }

    /**
     * 学生提交购电申请（为本人当前房间购电）。
     *
     * @throws BusinessException 未入住 / 度数非法
     */
    public ElectricityPurchase submitPurchase(Student student, double degree) {
        if (student == null || !student.isCheckedIn()) {
            throw new BusinessException("您尚未入住宿舍，无法购电");
        }
        if (Double.isNaN(degree) || degree <= 0) {
            throw new BusinessException("购电度数需为大于 0 的数字");
        }
        if (degree > MAX_DEGREE) {
            throw new BusinessException("单次购电不能超过 " + FormatUtil.num(MAX_DEGREE) + " 度");
        }
        double amount = Math.round(degree * UNIT_PRICE * 100) / 100.0;
        ElectricityPurchase purchase = new ElectricityPurchase(dc().nextElectricityId(),
                roomKey(student), student.getId(), degree, UNIT_PRICE, amount,
                TimeUtil.now(), ElectricityPurchase.STATUS_PENDING);
        dc().getElectricityPurchases().add(purchase);
        dc().saveAll();
        return purchase;
    }

    /**
     * 宿管科售电：校验仍待售电 → 房间电表到账 → 记录置为已售。
     *
     * @throws BusinessException 单据已处理 / 对应房间不存在
     */
    public ElectricityPurchase sell(ElectricityPurchase purchase, Admin admin) {
        if (purchase == null || !purchase.isPending()) {
            throw new BusinessException("该购电单已售，请勿重复操作");
        }
        Room room = dc().findRoomByKey(purchase.getRoomKey());
        if (room == null) {
            throw new BusinessException("购电单对应的房间不存在（可能该学生已退宿）");
        }
        room.creditElectricity(purchase.getDegree());
        purchase.setStatus(ElectricityPurchase.STATUS_PAID);
        purchase.setHandlerId(admin.getId());
        purchase.setHandleTime(TimeUtil.now());
        dc().saveAll();
        return purchase;
    }

    /** 学生本人的全部购电记录（时间倒序）。 */
    public List<ElectricityPurchase> ofStudent(String studentId) {
        List<ElectricityPurchase> result = new ArrayList<>();
        for (ElectricityPurchase p : dc().getElectricityPurchases()) {
            if (p.getBuyerId().equals(studentId)) {
                result.add(p);
            }
        }
        sortDesc(result);
        return result;
    }

    /** 宿管科视角全部购电单（时间倒序）。 */
    public List<ElectricityPurchase> findAll() {
        List<ElectricityPurchase> result = new ArrayList<>(dc().getElectricityPurchases());
        sortDesc(result);
        return result;
    }

    /** 某房间当前剩余电量（度）。 */
    public double balanceOf(Room room) {
        return room == null ? 0 : room.getElectricityBalance();
    }

    private String roomKey(Student student) {
        return student.getCurrentBuilding() + "-" + student.getCurrentRoom();
    }

    private void sortDesc(List<ElectricityPurchase> list) {
        Collections.sort(list, new Comparator<ElectricityPurchase>() {
            @Override
            public int compare(ElectricityPurchase a, ElectricityPurchase b) {
                return b.getCreateTime().compareTo(a.getCreateTime());
            }
        });
    }
}

package com.nchu.dorm.service;

import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.record.HygieneRecord;
import com.nchu.dorm.model.record.NightReturnRecord;
import com.nchu.dorm.model.record.ValuablesRecord;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.util.BusinessException;
import com.nchu.dorm.util.TimeUtil;

/**
 * 宿舍管理人员日常管理服务：夜归登记、卫生检查、贵重物品出入登记。
 * <p>宿舍管理人员只能登记 <b>本人分管楼栋</b> 内入住的在校学生 / 房间（规则收口在 service）。</p>
 */
public class DormStaffService {

    private DataCenter dc() {
        return DataCenter.instance();
    }

    /**
     * 界面预检：查询分管楼栋内入住的学生（不写库，供登记前展示学生姓名/宿舍）。
     * 规则同正式登记——不在本人分管楼栋则抛业务异常。
     */
    public Student findManagedStudent(DormStaff staff, String studentId) {
        return requireManagedOccupant(staff, studentId);
    }

    // ---------- 夜归登记 ----------

    /** 登记某学生夜归（学生须入住在本管理员分管的楼栋）。 */
    public NightReturnRecord registerNightReturn(DormStaff staff, String studentId,
                                                 String date, String returnTime, String reason) {
        Student student = requireManagedOccupant(staff, studentId);
        if (date == null || date.trim().isEmpty()) {
            throw new BusinessException("请填写夜归日期");
        }
        if (returnTime == null || returnTime.trim().isEmpty()) {
            throw new BusinessException("请填写夜归时间");
        }
        String cause = reason == null ? "" : reason.trim();
        NightReturnRecord record = new NightReturnRecord(dc().nextNightReturnId(),
                student.getId(), date.trim(), returnTime.trim(), cause);
        dc().getNightReturnRecords().add(record);
        dc().saveAll();
        return record;
    }

    // ---------- 卫生检查 ----------

    /** 登记某房间卫生检查（房间须在本管理员分管的楼栋内）。 */
    public HygieneRecord registerHygiene(DormStaff staff, String buildingName, String roomNo,
                                         String date, double score, String comment) {
        if (buildingName == null || !staff.manages(buildingName)) {
            throw new BusinessException("该楼栋不属于您分管范围，无法登记");
        }
        if (roomNo == null || roomNo.trim().isEmpty()) {
            throw new BusinessException("请填写/选择房间号");
        }
        Room room = dc().findRoom(buildingName, roomNo.trim());
        if (room == null) {
            throw new BusinessException("该房间不存在于 " + buildingName + " 中");
        }
        if (Double.isNaN(score) || score < 0 || score > 100) {
            throw new BusinessException("卫生得分需在 0~100 之间");
        }
        if (date == null || date.trim().isEmpty()) {
            throw new BusinessException("请填写检查日期");
        }
        HygieneRecord record = new HygieneRecord(dc().nextHygieneId(),
                room.displayKey(), date.trim(), score, staff.getId(),
                comment == null ? "" : comment.trim());
        dc().getHygieneRecords().add(record);
        dc().saveAll();
        return record;
    }

    // ---------- 贵重物品出入登记 ----------

    /** 登记某学生携带贵重物品出入（学生须入住在本管理员分管的楼栋内）。 */
    public ValuablesRecord registerValuables(DormStaff staff, String studentId,
                                             String itemName, String direction) {
        Student student = requireManagedOccupant(staff, studentId);
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new BusinessException("请填写物品名称");
        }
        if (!ValuablesRecord.DIRECTION_IN.equals(direction)
                && !ValuablesRecord.DIRECTION_OUT.equals(direction)) {
            throw new BusinessException("出入方向不正确");
        }
        ValuablesRecord record = new ValuablesRecord(dc().nextValuablesId(),
                student.getId(), itemName.trim(), direction, TimeUtil.now(), staff.getId());
        dc().getValuablesRecords().add(record);
        dc().saveAll();
        return record;
    }

    // ---------- 私有校验 ----------

    /**
     * 校验学生存在、已入住、且入住楼栋属于该管理员分管范围，返回该学生。
     */
    private Student requireManagedOccupant(DormStaff staff, String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new BusinessException("请填写学生学号");
        }
        Student student = dc().findStudentById(studentId.trim());
        if (student == null) {
            throw new BusinessException("未找到该学生，请核对学号");
        }
        if (!student.isCheckedIn()) {
            throw new BusinessException("该学生当前未入住宿舍，无法登记");
        }
        if (!staff.manages(student.getCurrentBuilding())) {
            throw new BusinessException("该学生不在您分管的楼栋（" + student.getCurrentBuilding()
                    + "）内，无法登记");
        }
        return student;
    }
}

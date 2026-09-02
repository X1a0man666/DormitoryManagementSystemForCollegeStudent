package com.nchu.dorm.service;

import com.nchu.dorm.model.Bed;
import com.nchu.dorm.model.College;
import com.nchu.dorm.model.Counselor;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.application.DormApplication;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.util.BusinessException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 宿舍申请服务。
 * <p>
 * 闭环：学生提交入住申请 -> 辅导员审批（分配本学院楼栋内的房间）或驳回。
 * 转移、退出类型在初版仅建模，完整流程后续迭代补充。
 */
public class DormApplicationService {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 学生提交入住申请。
     */
    public DormApplication submitApply(Student student, String targetBuilding, String reason) {
        DataCenter dc = DataCenter.instance();

        if (targetBuilding == null || targetBuilding.isEmpty()) {
            throw new BusinessException("请选择目标楼栋");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("请填写申请原因");
        }
        if (student.isCheckedIn()) {
            throw new BusinessException("您已入住宿舍，如需调整请走转移申请");
        }
        College college = dc.findCollegeByCode(student.getCollegeCode());
        if (college == null) {
            throw new BusinessException("未找到所属学院，无法申请");
        }
        if (!college.getBuildingNames().contains(targetBuilding)) {
            throw new BusinessException("目标楼栋不属于本学院分配范围");
        }

        DormApplication app = new DormApplication(dc.nextApplicationId(), student.getId(),
                DormApplication.TYPE_APPLY, targetBuilding, reason,
                DormApplication.STATUS_PENDING, now());
        dc.getDormApplications().add(app);
        dc.saveAll();
        return app;
    }

    /**
     * 查询某学院学生的待审批申请（辅导员视角）。
     */
    public List<DormApplication> findPendingOfCollege(String collegeCode) {
        DataCenter dc = DataCenter.instance();
        List<DormApplication> result = new ArrayList<>();
        for (DormApplication app : dc.getDormApplications()) {
            if (!DormApplication.STATUS_PENDING.equals(app.getStatus())) {
                continue;
            }
            Student student = dc.findStudentById(app.getStudentId());
            if (student != null && student.getCollegeCode().equals(collegeCode)) {
                result.add(app);
            }
        }
        return result;
    }

    /**
     * 查询某学生提交的全部申请（学生视角）。
     */
    public List<DormApplication> findApplicationsOfStudent(String studentId) {
        DataCenter dc = DataCenter.instance();
        List<DormApplication> result = new ArrayList<>();
        for (DormApplication app : dc.getDormApplications()) {
            if (app.getStudentId().equals(studentId)) {
                result.add(app);
            }
        }
        return result;
    }

    /**
     * 辅导员审批通过并分配房间床位。
     */
    public void approve(DormApplication app, Counselor counselor, String buildingName, String roomNo, String comment) {
        DataCenter dc = DataCenter.instance();

        Student student = dc.findStudentById(app.getStudentId());
        if (student == null) {
            throw new BusinessException("申请学生不存在");
        }
        if (!student.getCollegeCode().equals(counselor.getCollegeCode())) {
            throw new BusinessException("只能审批本学院学生的申请");
        }
        Room room = dc.findRoom(buildingName, roomNo);
        if (room == null) {
            throw new BusinessException("目标房间不存在");
        }
        Bed bed = room.findEmptyBed();
        if (bed == null) {
            throw new BusinessException("该房间已住满，请选择其他房间");
        }

        bed.setOccupantId(student.getId());
        student.setCurrentBuilding(buildingName);
        student.setCurrentRoom(roomNo);
        app.setStatus(DormApplication.STATUS_APPROVED);
        app.setTargetRoom(roomNo);
        app.setReviewerId(counselor.getId());
        app.setReviewTime(now());
        app.setReviewComment(comment == null ? "" : comment);
        dc.saveAll();
    }

    /**
     * 辅导员驳回申请。
     */
    public void reject(DormApplication app, Counselor counselor, String comment) {
        DataCenter dc = DataCenter.instance();

        Student student = dc.findStudentById(app.getStudentId());
        if (student == null) {
            throw new BusinessException("申请学生不存在");
        }
        if (!student.getCollegeCode().equals(counselor.getCollegeCode())) {
            throw new BusinessException("只能审批本学院学生的申请");
        }

        app.setStatus(DormApplication.STATUS_REJECTED);
        app.setReviewerId(counselor.getId());
        app.setReviewTime(now());
        app.setReviewComment(comment == null ? "" : comment);
        dc.saveAll();
    }

    private String now() {
        return DATE_FMT.format(new Date());
    }
}

package com.nchu.dorm.service;

import com.nchu.dorm.model.Admin;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.application.RepairTicket;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.util.BusinessException;
import com.nchu.dorm.util.TimeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 维修服务。
 * <p>
 * 闭环：学生（已入住）报修当前房间 → 工单 PENDING（待处理）→
 * 宿管科受理 → PROCESSING（处理中）→ 宿管科办结 → DONE（已完成），记录处理人/时间。
 * </p>
 */
public class RepairService {

    private DataCenter dc() {
        return DataCenter.instance();
    }

    /** 学生提交报修单（当前房间）。 */
    public RepairTicket submitRepair(Student student, String description) {
        if (student == null || !student.isCheckedIn()) {
            throw new BusinessException("您尚未入住宿舍，无法报修");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new BusinessException("请填写维修内容");
        }
        RepairTicket ticket = new RepairTicket(dc().nextRepairId(),
                student.getCurrentBuilding() + "-" + student.getCurrentRoom(),
                student.getId(), description.trim(), RepairTicket.STATUS_PENDING, TimeUtil.now());
        dc().getRepairTickets().add(ticket);
        dc().saveAll();
        return ticket;
    }

    /** 宿管科受理：待处理 → 处理中。 */
    public RepairTicket accept(RepairTicket ticket, Admin admin) {
        checkOpen(ticket);
        if (!RepairTicket.STATUS_PENDING.equals(ticket.getStatus())) {
            throw new BusinessException("该工单当前不在待处理状态，无法受理");
        }
        ticket.setStatus(RepairTicket.STATUS_PROCESSING);
        ticket.setHandlerId(admin.getId());
        ticket.setHandleTime(TimeUtil.now());
        dc().saveAll();
        return ticket;
    }

    /** 宿管科办结：处理中 → 已完成。 */
    public RepairTicket finish(RepairTicket ticket, Admin admin) {
        checkOpen(ticket);
        if (!RepairTicket.STATUS_PROCESSING.equals(ticket.getStatus())) {
            throw new BusinessException("该工单当前不在处理中状态，无法办结");
        }
        ticket.setStatus(RepairTicket.STATUS_DONE);
        if (ticket.getHandlerId() == null) {
            ticket.setHandlerId(admin.getId());
        }
        ticket.setHandleTime(TimeUtil.now());
        dc().saveAll();
        return ticket;
    }

    /** 学生本人的报修记录（时间倒序）。 */
    public List<RepairTicket> ofStudent(String studentId) {
        List<RepairTicket> result = new ArrayList<>();
        for (RepairTicket t : dc().getRepairTickets()) {
            if (t.getReporterId().equals(studentId)) {
                result.add(t);
            }
        }
        sortDesc(result);
        return result;
    }

    /** 宿管科视角全部工单（时间倒序）。 */
    public List<RepairTicket> findAll() {
        List<RepairTicket> result = new ArrayList<>(dc().getRepairTickets());
        sortDesc(result);
        return result;
    }

    private void checkOpen(RepairTicket ticket) {
        if (ticket == null) {
            throw new BusinessException("请先选择一张维修工单");
        }
    }

    private void sortDesc(List<RepairTicket> list) {
        Collections.sort(list, new Comparator<RepairTicket>() {
            @Override
            public int compare(RepairTicket a, RepairTicket b) {
                return b.getCreateTime().compareTo(a.getCreateTime());
            }
        });
    }
}

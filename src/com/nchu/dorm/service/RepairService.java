package com.nchu.dorm.service;

import com.nchu.dorm.model.Admin;
import com.nchu.dorm.model.DormStaff;
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
 * 三级闭环：学生（已入住）报修当前房间 → 工单 SUBMITTED（待宿管上报，尚未到达宿管科）→
 * 分管宿舍管理人员核实后上报（见 {@link #escalate}）→ PENDING（待处理）→
 * 宿管科受理 → PROCESSING（处理中）→ 宿管科办结 → DONE（已完成），记录处理人/时间。
 * </p>
 * <p>旧数据里既有的 PENDING 工单（本次改造前由学生直达宿管科）一律视为已上报，无需迁移。</p>
 */
public class RepairService {

    private DataCenter dc() {
        return DataCenter.instance();
    }

    /** 学生提交报修单（当前房间）：工单先进入分管宿管的接收队列，由宿管上报宿管科。 */
    public RepairTicket submitRepair(Student student, String description) {
        if (student == null || !student.isCheckedIn()) {
            throw new BusinessException("您尚未入住宿舍，无法报修");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new BusinessException("请填写维修内容");
        }
        RepairTicket ticket = new RepairTicket(dc().nextRepairId(),
                student.getCurrentBuilding() + "-" + student.getCurrentRoom(),
                student.getId(), description.trim(), RepairTicket.STATUS_SUBMITTED, TimeUtil.now());
        dc().getRepairTickets().add(ticket);
        dc().saveAll();
        return ticket;
    }

    /**
     * 本人分管范围内的维修工单，按提交时间倒序。
     * 判据：工单房间所属楼栋在分管范围内，<b>或</b>该工单由本人上报（楼栋易主后仍可追溯）。
     * <p>按工单自带的 roomKey 判楼栋而非学生现住信息，学生换宿后旧工单仍归原楼栋宿管。</p>
     */
    public List<RepairTicket> repairTicketsOfStaff(DormStaff staff) {
        List<RepairTicket> result = new ArrayList<>();
        for (RepairTicket t : dc().getRepairTickets()) {
            if (staff.manages(buildingOfRoomKey(t.getRoomKey()))
                    || staff.getId().equals(t.getEscalatorId())) {
                result.add(t);
            }
        }
        sortDesc(result);
        return result;
    }

    /** 分管范围内学生已提交、待本人上报宿管科的工单，按提交时间倒序。 */
    public List<RepairTicket> pendingEscalateOfStaff(DormStaff staff) {
        List<RepairTicket> result = new ArrayList<>();
        for (RepairTicket t : repairTicketsOfStaff(staff)) {
            if (t.isSubmitted()) {
                result.add(t);
            }
        }
        return result;
    }

    /** 把单条学生报修单上报宿管科（状态须为「待宿管上报」，且须在本人分管范围内）。 */
    public void escalate(RepairTicket ticket, DormStaff staff) {
        if (ticket == null) {
            throw new BusinessException("请先选择一条待上报的报修单");
        }
        if (!ticket.isSubmitted()) {
            throw new BusinessException("该工单当前状态为「" + ticket.getStatusName() + "」，无需上报");
        }
        if (!staff.manages(buildingOfRoomKey(ticket.getRoomKey()))
                && !staff.getId().equals(ticket.getEscalatorId())) {
            throw new BusinessException("该工单不在您分管的楼栋范围内，无法上报");
        }
        markEscalated(ticket, staff, TimeUtil.now());
        dc().saveAll();
    }

    /** 把本人分管范围内全部「待上报」的学生报修单一键上报宿管科，返回处理条数。 */
    public int escalateAll(DormStaff staff) {
        List<RepairTicket> pending = pendingEscalateOfStaff(staff);
        String now = TimeUtil.now();
        for (RepairTicket t : pending) {
            markEscalated(t, staff, now);
        }
        if (!pending.isEmpty()) {
            dc().saveAll();
        }
        return pending.size();
    }

    /** 打上上报标记；不落盘，由调用方决定逐单还是批量统一保存。 */
    private void markEscalated(RepairTicket ticket, DormStaff staff, String time) {
        ticket.setStatus(RepairTicket.STATUS_PENDING);
        ticket.setEscalatorId(staff.getId());
        ticket.setEscalateTime(time);
    }

    /** 从「楼栋-房间」组合键中取出楼栋名；空值返回空串。 */
    private String buildingOfRoomKey(String roomKey) {
        return roomKey == null ? "" : roomKey.split("-", -1)[0];
    }

    /** 宿管科受理：待处理 → 处理中。 */
    public RepairTicket accept(RepairTicket ticket, Admin admin) {
        checkOpen(ticket);
        if (!RepairTicket.STATUS_PENDING.equals(ticket.getStatus())) {
            throw new BusinessException("该工单当前不在待处理状态，无法受理");
        }
        stampAccepted(ticket, admin);
        dc().saveAll();
        return ticket;
    }

    /**
     * 一键受理：把全部「待处理」工单批量置为「处理中」（全校范围，不受界面筛选影响）。
     *
     * @return 本次受理的工单数
     */
    public int acceptAll(Admin admin) {
        int count = 0;
        for (RepairTicket ticket : dc().getRepairTickets()) {
            if (!RepairTicket.STATUS_PENDING.equals(ticket.getStatus())) {
                continue;
            }
            stampAccepted(ticket, admin);
            count++;
        }
        if (count > 0) {
            dc().saveAll();
        }
        return count;
    }

    /** 宿管科办结：处理中 → 已完成。 */
    public RepairTicket finish(RepairTicket ticket, Admin admin) {
        checkOpen(ticket);
        if (!RepairTicket.STATUS_PROCESSING.equals(ticket.getStatus())) {
            throw new BusinessException("该工单当前不在处理中状态，无法办结");
        }
        stampFinished(ticket, admin);
        dc().saveAll();
        return ticket;
    }

    /**
     * 一键办结：把全部未办结工单（待处理 + 处理中）批量置为「已完成」（全校范围，不受界面筛选影响）。
     * 待处理工单会先被打上受理人，与逐单「直接办结」的结果一致。
     * <p>尚未上报宿管科的工单（{@link RepairTicket#STATUS_SUBMITTED}）不在宿管科职责范围内，
     * 一律跳过，避免绕过宿管核实环节。</p>
     *
     * @return 本次办结的工单数
     */
    public int finishAll(Admin admin) {
        int count = 0;
        for (RepairTicket ticket : dc().getRepairTickets()) {
            if (RepairTicket.STATUS_DONE.equals(ticket.getStatus()) || ticket.isSubmitted()) {
                continue;
            }
            if (RepairTicket.STATUS_PENDING.equals(ticket.getStatus())) {
                stampAccepted(ticket, admin);
            }
            stampFinished(ticket, admin);
            count++;
        }
        if (count > 0) {
            dc().saveAll();
        }
        return count;
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

    /** 宿管科视角全部工单（时间倒序）；尚未被宿管上报的工单不出现，避免越级直达。 */
    public List<RepairTicket> findAll() {
        List<RepairTicket> result = new ArrayList<>();
        for (RepairTicket t : dc().getRepairTickets()) {
            if (!t.isSubmitted()) {
                result.add(t);
            }
        }
        sortDesc(result);
        return result;
    }

    /** 打上受理标记；不落盘，由调用方决定逐单还是批量统一保存。 */
    private void stampAccepted(RepairTicket ticket, Admin admin) {
        ticket.setStatus(RepairTicket.STATUS_PROCESSING);
        ticket.setHandlerId(admin.getId());
        ticket.setHandleTime(TimeUtil.now());
    }

    /** 打上办结标记；不落盘，由调用方决定逐单还是批量统一保存。 */
    private void stampFinished(RepairTicket ticket, Admin admin) {
        ticket.setStatus(RepairTicket.STATUS_DONE);
        if (ticket.getHandlerId() == null) {
            ticket.setHandlerId(admin.getId());
        }
        ticket.setHandleTime(TimeUtil.now());
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

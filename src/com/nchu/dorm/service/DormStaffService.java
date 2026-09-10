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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 宿舍管理人员日常管理服务：夜归登记、卫生检查、贵重物品出入登记，学生异议复核，以及学生报损后的遗失上报。
 * <p>宿舍管理人员只能登记 / 复核 / 上报 <b>本人分管楼栋</b> 内入住的在校学生 / 房间（规则收口在 service）。</p>
 * <p>学生端的对应反馈见 {@link StudentLifeService}；上报后的公告发布见 {@link AnnouncementService}；
 * 学生报修的接收与转报见 {@link RepairService#escalate}。</p>
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

    /**
     * 本人分管范围内的卫生检查记录，按检查日期倒序。
     * 判据：房间所属楼栋在分管范围内，<b>或</b>该记录由本人检查（楼栋易主后仍可追溯）。
     */
    public List<HygieneRecord> hygieneRecordsOfStaff(DormStaff staff) {
        List<HygieneRecord> list = new ArrayList<>();
        for (HygieneRecord r : dc().getHygieneRecords()) {
            if (staff.manages(buildingOfRoomKey(r.getRoomKey()))
                    || staff.getId().equals(r.getInspectorId())) {
                list.add(r);
            }
        }
        Collections.sort(list, new Comparator<HygieneRecord>() {
            @Override
            public int compare(HygieneRecord a, HygieneRecord b) {
                return b.getDate().compareTo(a.getDate());
            }
        });
        return list;
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

    // ---------- 遗失上报（学生报损 → 宿管 → 宿管科） ----------

    /**
     * 本人分管范围内的夜归记录，按日期倒序。
     * <p>夜归记录不存登记人工号，只按"学生是否住在分管楼栋内"过滤——与登记时的校验口径一致。</p>
     */
    public List<NightReturnRecord> nightReturnsOfStaff(DormStaff staff) {
        List<NightReturnRecord> list = new ArrayList<>();
        for (NightReturnRecord r : dc().getNightReturnRecords()) {
            if (inManagedStudent(staff, r.getStudentId())) {
                list.add(r);
            }
        }
        Collections.sort(list, new Comparator<NightReturnRecord>() {
            @Override
            public int compare(NightReturnRecord a, NightReturnRecord b) {
                return (b.getDate() + " " + b.getReturnTime()).compareTo(a.getDate() + " " + a.getReturnTime());
            }
        });
        return list;
    }

    /**
     * 本人分管范围内的贵重物品记录，按登记时间倒序。
     * 判据：学生正住在分管楼栋内，<b>或</b>该记录由本人登记（学生后来搬走也仍可追溯）。
     */
    public List<ValuablesRecord> valuablesOfStaff(DormStaff staff) {
        List<ValuablesRecord> list = new ArrayList<>();
        for (ValuablesRecord r : dc().getValuablesRecords()) {
            if (inManagedValuables(staff, r)) {
                list.add(r);
            }
        }
        Collections.sort(list, new Comparator<ValuablesRecord>() {
            @Override
            public int compare(ValuablesRecord a, ValuablesRecord b) {
                return b.getRecordTime().compareTo(a.getRecordTime());
            }
        });
        return list;
    }

    /** 分管范围内学生已报遗失、待本人上报宿管科的记录，按学生报损时间倒序。 */
    public List<ValuablesRecord> pendingEscalateOfStaff(DormStaff staff) {
        List<ValuablesRecord> list = new ArrayList<>();
        for (ValuablesRecord r : valuablesOfStaff(staff)) {
            if (ValuablesRecord.REPORT_STUDENT_REPORTED.equals(r.getReportStatus())) {
                list.add(r);
            }
        }
        return list;
    }

    /** 分管范围内已上报宿管科的记录（含已发布公告的），按上报时间倒序，供追溯查看。 */
    public List<ValuablesRecord> escalatedOfStaff(DormStaff staff) {
        List<ValuablesRecord> list = new ArrayList<>();
        for (ValuablesRecord r : valuablesOfStaff(staff)) {
            if (ValuablesRecord.REPORT_ESCALATED.equals(r.getReportStatus())
                    || ValuablesRecord.REPORT_PUBLISHED.equals(r.getReportStatus())) {
                list.add(r);
            }
        }
        Collections.sort(list, new Comparator<ValuablesRecord>() {
            @Override
            public int compare(ValuablesRecord a, ValuablesRecord b) {
                String ta = a.getReportTime() == null ? "" : a.getReportTime();
                String tb = b.getReportTime() == null ? "" : b.getReportTime();
                return tb.compareTo(ta);
            }
        });
        return list;
    }

    /** 把本人分管范围内全部「待上报」的遗失记录一键上报宿管科，返回处理条数。 */
    public int escalateLostValuables(DormStaff staff) {
        List<ValuablesRecord> pending = pendingEscalateOfStaff(staff);
        String now = TimeUtil.now();
        for (ValuablesRecord r : pending) {
            markEscalated(r, staff, now);
        }
        if (!pending.isEmpty()) {
            dc().saveAll();
        }
        return pending.size();
    }

    /** 把单条学生已报遗失的记录上报宿管科（须在本人分管范围内）。 */
    public void escalate(ValuablesRecord record, DormStaff staff) {
        if (record == null) {
            throw new BusinessException("请先选择一条遗失记录");
        }
        if (!ValuablesRecord.REPORT_STUDENT_REPORTED.equals(record.getReportStatus())) {
            throw new BusinessException("该记录当前状态为「" + record.getReportStatusName() + "」，无需上报");
        }
        if (!inManagedValuables(staff, record)) {
            throw new BusinessException("该记录不在您分管的楼栋范围内，无法上报");
        }
        markEscalated(record, staff, TimeUtil.now());
        dc().saveAll();
    }

    private void markEscalated(ValuablesRecord record, DormStaff staff, String time) {
        record.setReportStatus(ValuablesRecord.REPORT_ESCALATED);
        record.setReporterId(staff.getId());
        record.setReportTime(time);
    }

    // ---------- 学生异议复核（学生提异议 → 分管宿管复核收尾） ----------

    /** 分管范围内学生已提异议、待本人复核的夜归记录，按异议时间倒序。 */
    public List<NightReturnRecord> pendingNightReviewOfStaff(DormStaff staff) {
        List<NightReturnRecord> list = new ArrayList<>();
        for (NightReturnRecord r : nightReturnsOfStaff(staff)) {
            if (r.needsReview()) {
                list.add(r);
            }
        }
        Collections.sort(list, new Comparator<NightReturnRecord>() {
            @Override
            public int compare(NightReturnRecord a, NightReturnRecord b) {
                return safeTime(b.getConfirmTime()).compareTo(safeTime(a.getConfirmTime()));
            }
        });
        return list;
    }

    /** 分管范围内学生已反馈「有问题」、待本人复核的卫生检查记录，按反馈时间倒序。 */
    public List<HygieneRecord> pendingHygieneReviewOfStaff(DormStaff staff) {
        List<HygieneRecord> list = new ArrayList<>();
        for (HygieneRecord r : hygieneRecordsOfStaff(staff)) {
            if (r.needsReview()) {
                list.add(r);
            }
        }
        Collections.sort(list, new Comparator<HygieneRecord>() {
            @Override
            public int compare(HygieneRecord a, HygieneRecord b) {
                return safeTime(b.getFeedbackTime()).compareTo(safeTime(a.getFeedbackTime()));
            }
        });
        return list;
    }

    /**
     * 复核夜归异议（须在本人分管范围内、且记录处于「有异议」状态）：
     * 维持原登记，或用更正后的日期 / 时间 / 原因更正登记内容后通过。
     * 复核后记录终结，学生不可再改（见 {@link StudentLifeService}）。
     *
     * @param keepOriginal true=维持原登记；false=按入参更正后通过
     * @param date         更正后的日期（keepOriginal 为 false 时必填）
     * @param returnTime   更正后的夜归时间（keepOriginal 为 false 时必填）
     * @param reason       更正后的夜归原因（可空）
     * @param reviewComment 复核意见（可空）
     */
    public void reviewNightReturn(NightReturnRecord record, DormStaff staff, boolean keepOriginal,
                                  String date, String returnTime, String reason, String reviewComment) {
        if (record == null) {
            throw new BusinessException("请先选择一条待复核的夜归异议记录");
        }
        if (!record.needsReview()) {
            throw new BusinessException("该记录当前状态为「" + record.getConfirmStatusName() + "」，无需复核");
        }
        if (!inManagedStudent(staff, record.getStudentId())) {
            throw new BusinessException("该学生不在您分管的楼栋范围内，无法复核");
        }
        if (!keepOriginal) {
            if (date == null || date.trim().isEmpty()) {
                throw new BusinessException("请填写更正后的夜归日期");
            }
            if (returnTime == null || returnTime.trim().isEmpty()) {
                throw new BusinessException("请填写更正后的夜归时间");
            }
            record.setDate(date.trim());
            record.setReturnTime(returnTime.trim());
            record.setReason(reason == null ? "" : reason.trim());
        }
        markReviewed(record, staff, keepOriginal ? NightReturnRecord.REVIEW_KEPT
                : NightReturnRecord.REVIEW_ADJUSTED, reviewComment);
        dc().saveAll();
    }

    /** 一键维持分管范围内全部待复核的夜归异议，返回复核条数。 */
    public int reviewAllNightReturnsKeep(DormStaff staff) {
        List<NightReturnRecord> pending = pendingNightReviewOfStaff(staff);
        String now = TimeUtil.now();
        for (NightReturnRecord r : pending) {
            markReviewed(r, staff, NightReturnRecord.REVIEW_KEPT, "经核实，维持原登记", now);
        }
        if (!pending.isEmpty()) {
            dc().saveAll();
        }
        return pending.size();
    }

    /**
     * 复核卫生检查反馈（须在本人分管范围内、且记录处于「有问题」状态）：
     * 维持原记录，或用更正后的得分 / 评语更正检查结果后通过。
     * 复核后记录终结，学生不可再改（见 {@link StudentLifeService}）。
     *
     * @param keepOriginal  true=维持原记录；false=按入参更正后通过
     * @param score         更正后的得分（keepOriginal 为 false 时须在 0~100）
     * @param comment       更正后的评语（可空）
     * @param reviewComment 复核意见（可空）
     */
    public void reviewHygiene(HygieneRecord record, DormStaff staff, boolean keepOriginal,
                              double score, String comment, String reviewComment) {
        if (record == null) {
            throw new BusinessException("请先选择一条待复核的卫生检查反馈");
        }
        if (!record.needsReview()) {
            throw new BusinessException("该记录当前状态为「" + record.getStudentStatusName() + "」，无需复核");
        }
        if (!inManagedHygiene(staff, record)) {
            throw new BusinessException("该记录不在您分管的楼栋范围内，无法复核");
        }
        if (!keepOriginal) {
            if (Double.isNaN(score) || score < 0 || score > 100) {
                throw new BusinessException("卫生得分需在 0~100 之间");
            }
            record.setScore(score);
            record.setComment(comment == null ? "" : comment.trim());
        }
        markReviewed(record, staff, keepOriginal ? HygieneRecord.REVIEW_KEPT
                : HygieneRecord.REVIEW_ADJUSTED, reviewComment);
        dc().saveAll();
    }

    /** 一键维持分管范围内全部待复核的卫生检查反馈，返回复核条数。 */
    public int reviewAllHygieneKeep(DormStaff staff) {
        List<HygieneRecord> pending = pendingHygieneReviewOfStaff(staff);
        String now = TimeUtil.now();
        for (HygieneRecord r : pending) {
            markReviewed(r, staff, HygieneRecord.REVIEW_KEPT, "经核实，维持原记录", now);
        }
        if (!pending.isEmpty()) {
            dc().saveAll();
        }
        return pending.size();
    }

    /** 打上复核标记；不落盘，由调用方决定逐单还是批量统一保存。 */
    private void markReviewed(NightReturnRecord record, DormStaff staff, String conclusion, String comment) {
        markReviewed(record, staff, conclusion, comment, TimeUtil.now());
    }

    private void markReviewed(NightReturnRecord record, DormStaff staff, String conclusion,
                              String comment, String time) {
        record.setConfirmStatus(conclusion);
        record.setReviewerId(staff.getId());
        record.setReviewTime(time);
        record.setReviewComment(comment == null ? "" : comment.trim());
    }

    /** 打上复核标记；不落盘，由调用方决定逐单还是批量统一保存。 */
    private void markReviewed(HygieneRecord record, DormStaff staff, String conclusion, String comment) {
        markReviewed(record, staff, conclusion, comment, TimeUtil.now());
    }

    private void markReviewed(HygieneRecord record, DormStaff staff, String conclusion,
                              String comment, String time) {
        record.setStudentStatus(conclusion);
        record.setReviewerId(staff.getId());
        record.setReviewTime(time);
        record.setReviewComment(comment == null ? "" : comment.trim());
    }

    // ---------- 私有校验 ----------

    /** 学生是否正入住在本管理员分管的楼栋内。 */
    private boolean inManagedStudent(DormStaff staff, String studentId) {
        Student s = dc().findStudentById(studentId);
        return s != null && s.isCheckedIn() && staff.manages(s.getCurrentBuilding());
    }

    /** 记录是否属于本人分管范围（学生现住分管楼栋，或该记录由本人登记）。 */
    private boolean inManagedValuables(DormStaff staff, ValuablesRecord record) {
        return inManagedStudent(staff, record.getStudentId())
                || staff.getId().equals(record.getHandlerId());
    }

    /** 卫生记录是否属于本人分管范围（房间现属分管楼栋，或该记录由本人检查）。 */
    private boolean inManagedHygiene(DormStaff staff, HygieneRecord record) {
        return staff.manages(buildingOfRoomKey(record.getRoomKey()))
                || staff.getId().equals(record.getInspectorId());
    }

    /** 从「楼栋-房间」组合键中取出楼栋名；空值返回空串。 */
    private String buildingOfRoomKey(String roomKey) {
        return roomKey == null ? "" : roomKey.split("-", -1)[0];
    }

    /** 时间字段排序用：null 视作空串，避免比较时抛空指针。 */
    private String safeTime(String time) {
        return time == null ? "" : time;
    }


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

package com.nchu.dorm.service;

import com.nchu.dorm.model.Counselor;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.announcement.Announcement;
import com.nchu.dorm.model.application.ElectricityPurchase;
import com.nchu.dorm.model.application.RepairTicket;
import com.nchu.dorm.model.record.HygieneRecord;
import com.nchu.dorm.model.record.NightReturnRecord;
import com.nchu.dorm.model.record.ValuablesRecord;
import com.nchu.dorm.storage.DataCenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 辅导员查询服务：对外称谓，以及本专业（本年级）学生各项记录的<b>只读</b>查询。
 * <p>
 * 辅导员工号即「专业年级代码」（入学年后两位 + 学院代码 + 专业代码，如 25201 = 25级·软件学院·软件工程），
 * 姓名默认与工号一致。因此界面上对未改名的辅导员显示「xx专业辅导员」（如「软件工程专业辅导员」），
 * 而不是裸露的工号；辅导员在「设置」里改过名后，直接显示本人姓名。
 * </p>
 * <p>
 * 查范围（{@link #studentsOf}）与「宿舍审批」里转专业换宿的归属判据一致：学生<b>当前</b>班级编码
 * 的专业年级代码等于辅导员工号。各项记录的可见性由此派生——购电/报修/夜归/贵重物品按学生学号过滤，
 * 卫生检查按房间登记、记录里不含学号，故按本专业学生当前入住房间过滤（见 {@link #roomStudentLabels}）。
 * 本类只提供读方法，任何修改仍走各自的业务服务（{@link ElectricityService} / {@link RepairService} /
 * {@link DormStaffService} / {@link AnnouncementService} 等）。
 * </p>
 * <p>
 * <b>为什么判据是班级编码而不是学号：</b>学号编码的是<b>入学时</b>的学院/专业/班级，转专业后学号不变
 * （见 {@link com.nchu.dorm.model.StudentId}），故按学号前 5 位取人会把转专业学生判给原专业辅导员。
 * 班级编码（{@link Student#getClassName()}）才是当前归属：转专业经
 * {@link DormApplicationService#approveMajorAccept} 审批通过时，学院/专业/班级三个字段被一并改写，
 * 转入学生随即纳入本范围、转出学生随即移出，无需任何额外标记——该字段也正是转专业审批自身用来
 * 判定审批权限的字段，查询范围与审批权限因此天然一致。转专业的完整痕迹（原班级 → 目标班级、
 * 两级审批人与时间）留在对应的 {@link DormApplication} 记录里，可回溯，不在学生表重复存一份。
 * </p>
 */
public class CounselorService {

    private DataCenter dc() {
        return DataCenter.instance();
    }

    // ==================== 对外称谓 ====================

    /**
     * 辅导员称谓：改过名（姓名与工号不同）用本人姓名，否则用「xx专业辅导员」；
     * 专业代码解析不出时退回姓名，避免显示空白。
     */
    public String displayName(Counselor counselor) {
        if (counselor == null) {
            return "";
        }
        if (!isRenamed(counselor)) {
            String major = majorNameOf(counselor);
            if (major != null) {
                return major + "专业辅导员";
            }
        }
        return counselor.getName();
    }

    /** 是否已自行改过名：姓名仍等于工号即视为未改名（与初始数据一致）。 */
    public boolean isRenamed(Counselor counselor) {
        return counselor != null && counselor.getName() != null
                && !counselor.getName().equals(counselor.getId());
    }

    /**
     * 查询范围称谓，如「软件工程专业 · 2025级」；专业代码或工号非标准格式时逐级降级
     * （无年级 →「xx专业」，无专业 → 学院名），避免显示空白。
     */
    public String scopeName(Counselor counselor) {
        if (counselor == null) {
            return "";
        }
        String major = majorNameOf(counselor);
        if (major == null) {
            return dc().collegeName(counselor.getCollegeCode());
        }
        String year = gradeYearOf(counselor);
        return year.isEmpty() ? major + "专业" : major + "专业 · " + year + "级";
    }

    // ==================== 本专业（本年级）学生范围 ====================

    /**
     * 本专业（本年级）全部学生。
     * <p>判据是<b>当前班级编码</b>而非学号（理由见类注释）：转专业学生在
     * {@link DormApplicationService#approveMajorAccept} 里被改写班级编码，故转出学生自然移出、
     * 转入学生自然纳入，与转专业换宿的归属判据保持一致。例如学号 24112322（入学时属 11 学院）
     * 转入 252016 班后即出现在 25201 辅导员的范围内。</p>
     */
    public List<Student> studentsOf(Counselor counselor) {
        List<Student> result = new ArrayList<>();
        if (counselor == null) {
            return result;
        }
        for (Student s : dc().getStudents()) {
            if (belongsTo(counselor, s)) {
                result.add(s);
            }
        }
        return result;
    }

    /** 本专业学生的学号集合（购电/报修/夜归/贵重物品按学号过滤）。 */
    public Set<String> studentIdsOf(Counselor counselor) {
        Set<String> ids = new LinkedHashSet<>();
        for (Student s : studentsOf(counselor)) {
            ids.add(s.getId());
        }
        return ids;
    }

    /** 本专业学生当前入住房间的「楼栋-房间」集合（卫生检查按房间登记，无学号可过滤）。 */
    public Set<String> roomKeysOf(Counselor counselor) {
        Set<String> keys = new LinkedHashSet<>();
        for (Student s : studentsOf(counselor)) {
            if (s.isCheckedIn()) {
                keys.add(s.getCurrentBuilding() + "-" + s.getCurrentRoom());
            }
        }
        return keys;
    }

    /**
     * 房间 → 该房间内本专业学生的展示名（「姓名（学号）」顿号连接，同寝室多人一并列出）。
     * 卫生检查记录不含学号，界面据此把房间还原到具体学生；无本专业学生的房间不在键中。
     */
    public Map<String, String> roomStudentLabels(Counselor counselor) {
        Map<String, List<String>> names = new LinkedHashMap<>();
        for (Student s : studentsOf(counselor)) {
            if (!s.isCheckedIn()) {
                continue;
            }
            String key = s.getCurrentBuilding() + "-" + s.getCurrentRoom();
            List<String> list = names.get(key);
            if (list == null) {
                list = new ArrayList<>();
                names.put(key, list);
            }
            list.add(s.getName() + "（" + s.getId() + "）");
        }
        Map<String, String> labels = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : names.entrySet()) {
            labels.put(entry.getKey(), String.join("、", entry.getValue()));
        }
        return labels;
    }

    // ==================== 本专业学生的各项记录（只读） ====================

    /** 本专业学生的购电记录（提交时间倒序）。 */
    public List<ElectricityPurchase> electricityOfCounselor(Counselor counselor) {
        Set<String> ids = studentIdsOf(counselor);
        List<ElectricityPurchase> result = new ArrayList<>();
        for (ElectricityPurchase p : dc().getElectricityPurchases()) {
            if (ids.contains(p.getBuyerId())) {
                result.add(p);
            }
        }
        Collections.sort(result, new Comparator<ElectricityPurchase>() {
            @Override
            public int compare(ElectricityPurchase a, ElectricityPurchase b) {
                return safe(b.getCreateTime()).compareTo(safe(a.getCreateTime()));
            }
        });
        return result;
    }

    /** 本专业学生提交的维修工单（提交时间倒序，含尚未由宿管上报宿管科的工单）。 */
    public List<RepairTicket> repairsOfCounselor(Counselor counselor) {
        Set<String> ids = studentIdsOf(counselor);
        List<RepairTicket> result = new ArrayList<>();
        for (RepairTicket t : dc().getRepairTickets()) {
            if (ids.contains(t.getReporterId())) {
                result.add(t);
            }
        }
        Collections.sort(result, new Comparator<RepairTicket>() {
            @Override
            public int compare(RepairTicket a, RepairTicket b) {
                return safe(b.getCreateTime()).compareTo(safe(a.getCreateTime()));
            }
        });
        return result;
    }

    /** 本专业学生的夜归记录（日期 + 夜归时间倒序）。 */
    public List<NightReturnRecord> nightReturnsOfCounselor(Counselor counselor) {
        Set<String> ids = studentIdsOf(counselor);
        List<NightReturnRecord> result = new ArrayList<>();
        for (NightReturnRecord r : dc().getNightReturnRecords()) {
            if (ids.contains(r.getStudentId())) {
                result.add(r);
            }
        }
        Collections.sort(result, new Comparator<NightReturnRecord>() {
            @Override
            public int compare(NightReturnRecord a, NightReturnRecord b) {
                return (safe(b.getDate()) + " " + safe(b.getReturnTime()))
                        .compareTo(safe(a.getDate()) + " " + safe(a.getReturnTime()));
            }
        });
        return result;
    }

    /** 本专业学生所在寝室的卫生检查记录（检查日期倒序）。 */
    public List<HygieneRecord> hygieneRecordsOfCounselor(Counselor counselor) {
        Set<String> roomKeys = roomKeysOf(counselor);
        List<HygieneRecord> result = new ArrayList<>();
        for (HygieneRecord r : dc().getHygieneRecords()) {
            if (roomKeys.contains(r.getRoomKey())) {
                result.add(r);
            }
        }
        Collections.sort(result, new Comparator<HygieneRecord>() {
            @Override
            public int compare(HygieneRecord a, HygieneRecord b) {
                return safe(b.getDate()).compareTo(safe(a.getDate()));
            }
        });
        return result;
    }

    /** 本专业学生所在寝室的卫生平均分（无记录返回 -1）。 */
    public double averageHygieneScoreOfCounselor(Counselor counselor) {
        List<HygieneRecord> records = hygieneRecordsOfCounselor(counselor);
        if (records.isEmpty()) {
            return -1;
        }
        double sum = 0;
        for (HygieneRecord r : records) {
            sum += r.getScore();
        }
        return sum / records.size();
    }

    /** 本专业学生的贵重物品出入登记（登记时间倒序，含带入与带出两个方向）。 */
    public List<ValuablesRecord> valuablesOfCounselor(Counselor counselor) {
        Set<String> ids = studentIdsOf(counselor);
        List<ValuablesRecord> result = new ArrayList<>();
        for (ValuablesRecord r : dc().getValuablesRecords()) {
            if (ids.contains(r.getStudentId())) {
                result.add(r);
            }
        }
        Collections.sort(result, new Comparator<ValuablesRecord>() {
            @Override
            public int compare(ValuablesRecord a, ValuablesRecord b) {
                return safe(b.getRecordTime()).compareTo(safe(a.getRecordTime()));
            }
        });
        return result;
    }

    /** 全校公告（发布时间倒序）。公告面向全校发布，不按专业过滤。 */
    public List<Announcement> announcements() {
        List<Announcement> result = new ArrayList<>(dc().getAnnouncements());
        Collections.sort(result, new Comparator<Announcement>() {
            @Override
            public int compare(Announcement a, Announcement b) {
                return safe(b.getPublishTime()).compareTo(safe(a.getPublishTime()));
            }
        });
        return result;
    }

    // ==================== 私有工具 ====================

    /** 学生是否属于该辅导员所带的专业年级：当前班级编码的专业年级代码 = 辅导员工号。 */
    private boolean belongsTo(Counselor counselor, Student student) {
        String id = counselor.getId();
        return id != null && id.equals(DormApplicationService.gradeOfClass(student.getClassName()));
    }

    /** 辅导员所带专业名；专业代码解析不出时返回 null。 */
    private String majorNameOf(Counselor counselor) {
        String major = dc().majorName(counselor.getCollegeCode(), counselor.getMajorCode());
        return major == null || major.isEmpty() ? null : major;
    }

    /** 入学年份（工号前 2 位 + 20），如 "2025"；工号非标准 5 位格式时返回空串。 */
    private String gradeYearOf(Counselor counselor) {
        String id = counselor.getId();
        if (id == null || id.length() != 5
                || !Character.isDigit(id.charAt(0)) || !Character.isDigit(id.charAt(1))) {
            return "";
        }
        return "20" + id.substring(0, 2);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}

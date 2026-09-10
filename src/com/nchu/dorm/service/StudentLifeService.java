package com.nchu.dorm.service;

import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.announcement.Announcement;
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
 * 学生生活服务：学生端对宿舍管理人员三类日常登记（夜归 / 卫生 / 贵重物品）的查看与反馈，
 * 以及全校公告的查看。
 * <p>与宿舍管理人员端 {@link DormStaffService} 一一对应，把「登记 → 学生确认」这条链补全：</p>
 * <ul>
 *   <li><b>夜归</b>：确认「已到寝」或提异议（{@link #confirmNightReturn} / {@link #disputeNightReturn}）；</li>
 *   <li><b>卫生检查</b>：查看本寝室评分与评语的历史记录，反馈「无异议」或「有问题」；
 *       异议由分管宿管复核（见 {@link DormStaffService#reviewHygiene}），复核后不可再反馈；</li>
 *   <li><b>贵重物品</b>：对本人被登记的<b>带入</b>、<b>带出</b>物品都确认「未丢失」或报「已遗失」，
 *       遗失后由分管宿管上报宿管科、宿管科发布公告（见 {@link DormStaffService} / {@link AnnouncementService}）。</li>
 * </ul>
 * <p>所有操作只允许作用于<b>学生本人</b>的记录，规则收口在本类。</p>
 */
public class StudentLifeService {

    private DataCenter dc() {
        return DataCenter.instance();
    }

    // ==================== 夜归确认 ====================

    /** 本人夜归记录，按日期+夜归时间倒序。 */
    public List<NightReturnRecord> nightReturnsOf(Student student) {
        List<NightReturnRecord> list = new ArrayList<>();
        if (student == null) {
            return list;
        }
        for (NightReturnRecord r : dc().getNightReturnRecords()) {
            if (student.getId().equals(r.getStudentId())) {
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

    /** 本人待确认的夜归记录条数（首页/页面提示用）。 */
    public int pendingNightReturnCount(Student student) {
        int n = 0;
        for (NightReturnRecord r : nightReturnsOf(student)) {
            if (r.isPendingConfirm()) {
                n++;
            }
        }
        return n;
    }

    /** 学生确认夜归记录「已到寝」。 */
    public void confirmNightReturn(Student student, NightReturnRecord record) {
        requireOwnNightReturn(student, record);
        if (record.isReviewed()) {
            throw new BusinessException("该记录已由宿管复核（" + record.getConfirmStatusName()
                    + "），复核结论为最终结果，不可再修改");
        }
        if (NightReturnRecord.CONFIRM_DISPUTED.equals(record.getConfirmStatus())) {
            throw new BusinessException("该记录已提交异议，如需改为确认请联系宿管重新登记");
        }
        record.setConfirmStatus(NightReturnRecord.CONFIRM_CONFIRMED);
        record.setStudentRemark("");
        record.setConfirmTime(TimeUtil.now());
        dc().saveAll();
    }

    /** 学生对夜归记录提出异议（说明必填）。 */
    public void disputeNightReturn(Student student, NightReturnRecord record, String remark) {
        requireOwnNightReturn(student, record);
        requireNotReviewed(record.isReviewed(), record.getConfirmStatusName());
        String note = requireRemark(remark, "请填写异议说明（如登记的时间或原因与实际不符）");
        record.setConfirmStatus(NightReturnRecord.CONFIRM_DISPUTED);
        record.setStudentRemark(note);
        record.setConfirmTime(TimeUtil.now());
        dc().saveAll();
    }

    private void requireOwnNightReturn(Student student, NightReturnRecord record) {
        if (student == null || record == null) {
            throw new BusinessException("请先在表格中选择一条夜归记录");
        }
        if (!student.getId().equals(record.getStudentId())) {
            throw new BusinessException("只能确认本人的夜归记录");
        }
    }

    // ==================== 卫生检查（查看与反馈） ====================

    /**
     * 本寝室（本人当前入住房间）的卫生检查历史记录，按检查日期倒序。
     * 未入住或房间不存在时返回空列表。
     */
    public List<HygieneRecord> hygieneRecordsOf(Student student) {
        List<HygieneRecord> list = new ArrayList<>();
        String roomKey = currentRoomKey(student);
        if (roomKey == null) {
            return list;
        }
        for (HygieneRecord r : dc().getHygieneRecords()) {
            if (roomKey.equals(r.getRoomKey())) {
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

    /** 本寝室卫生得分的平均分（无记录返回 -1）。 */
    public double averageScoreOf(Student student) {
        List<HygieneRecord> list = hygieneRecordsOf(student);
        if (list.isEmpty()) {
            return -1;
        }
        double sum = 0;
        for (HygieneRecord r : list) {
            sum += r.getScore();
        }
        return sum / list.size();
    }

    /** 学生对本次卫生检查反馈「无异议」。 */
    public void acknowledgeHygiene(Student student, HygieneRecord record) {
        requireOwnHygiene(student, record);
        requireNotReviewed(record.isReviewed(), record.getStudentStatusName());
        record.setStudentId(student.getId());
        record.setStudentStatus(HygieneRecord.FEEDBACK_ACKNOWLEDGED);
        record.setStudentRemark("");
        record.setFeedbackTime(TimeUtil.now());
        dc().saveAll();
    }

    /** 学生对本次卫生检查反馈「有问题」（说明必填）。 */
    public void disputeHygiene(Student student, HygieneRecord record, String remark) {
        requireOwnHygiene(student, record);
        requireNotReviewed(record.isReviewed(), record.getStudentStatusName());
        String note = requireRemark(remark, "请填写反馈说明（如对评分或评语有疑问的地方）");
        record.setStudentId(student.getId());
        record.setStudentStatus(HygieneRecord.FEEDBACK_DISPUTED);
        record.setStudentRemark(note);
        record.setFeedbackTime(TimeUtil.now());
        dc().saveAll();
    }

    private void requireOwnHygiene(Student student, HygieneRecord record) {
        if (student == null || record == null) {
            throw new BusinessException("请先在表格中选择一条卫生检查记录");
        }
        String roomKey = currentRoomKey(student);
        if (roomKey == null) {
            throw new BusinessException("您当前未入住宿舍，无法反馈卫生检查");
        }
        if (!roomKey.equals(record.getRoomKey())) {
            throw new BusinessException("只能反馈本人所住寝室的卫生检查记录");
        }
    }

    // ==================== 贵重物品确认 / 报遗失 ====================

    /**
     * 本人名下的贵重物品出入登记记录：<b>带入</b>与<b>带出</b>两个方向都需学生确认「未丢失」
     * （带入的物品在楼内同样有遗失风险），按登记时间倒序。
     */
    public List<ValuablesRecord> valuablesOf(Student student) {
        List<ValuablesRecord> list = new ArrayList<>();
        if (student == null) {
            return list;
        }
        for (ValuablesRecord r : dc().getValuablesRecords()) {
            if (student.getId().equals(r.getStudentId())) {
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

    /** 本人待确认的贵重物品记录条数。 */
    public int pendingValuablesCount(Student student) {
        int n = 0;
        for (ValuablesRecord r : valuablesOf(student)) {
            if (ValuablesRecord.CONFIRM_PENDING.equals(r.getConfirmStatus())) {
                n++;
            }
        }
        return n;
    }

    /** 学生确认贵重物品「未丢失」。 */
    public void confirmValuablesIntact(Student student, ValuablesRecord record) {
        requireOwnValuables(student, record);
        if (ValuablesRecord.CONFIRM_LOST.equals(record.getConfirmStatus())) {
            throw new BusinessException("该物品已报遗失并进入上报流程，无法改为「未丢失」");
        }
        record.setConfirmStatus(ValuablesRecord.CONFIRM_INTACT);
        record.setStudentRemark("");
        record.setConfirmTime(TimeUtil.now());
        dc().saveAll();
    }

    /**
     * 学生报告贵重物品「已遗失」（说明必填）。
     * 记录进入「待宿管上报」状态，由分管宿管核实后上报宿管科发布公告。
     */
    public void reportValuablesLost(Student student, ValuablesRecord record, String remark) {
        requireOwnValuables(student, record);
        if (ValuablesRecord.REPORT_PUBLISHED.equals(record.getReportStatus())) {
            throw new BusinessException("该遗失记录已由宿管科发布公告，无法重复上报");
        }
        String note = requireRemark(remark, "请填写遗失说明（物品特征、遗失时间地点等，便于宿管科通报查找）");
        record.setConfirmStatus(ValuablesRecord.CONFIRM_LOST);
        record.setStudentRemark(note);
        record.setConfirmTime(TimeUtil.now());
        if (ValuablesRecord.REPORT_NONE.equals(record.getReportStatus())) {
            record.setReportStatus(ValuablesRecord.REPORT_STUDENT_REPORTED);
        }
        dc().saveAll();
    }

    private void requireOwnValuables(Student student, ValuablesRecord record) {
        if (student == null || record == null) {
            throw new BusinessException("请先在表格中选择一条贵重物品记录");
        }
        if (!student.getId().equals(record.getStudentId())) {
            throw new BusinessException("只能确认本人的贵重物品记录");
        }
    }

    // ==================== 公告 ====================

    /** 全校公告，按发布时间倒序。 */
    public List<Announcement> announcements() {
        List<Announcement> list = new ArrayList<>(dc().getAnnouncements());
        Collections.sort(list, new Comparator<Announcement>() {
            @Override
            public int compare(Announcement a, Announcement b) {
                return b.getPublishTime().compareTo(a.getPublishTime());
            }
        });
        return list;
    }

    // ==================== 私有工具 ====================

    /** 本人当前入住房间的「楼栋-房间」组合键；未入住返回 null。 */
    private String currentRoomKey(Student student) {
        if (student == null || !student.isCheckedIn()) {
            return null;
        }
        Room room = dc().findRoom(student.getCurrentBuilding(), student.getCurrentRoom());
        return room == null ? null : room.displayKey();
    }

    /** 终态校验：记录一经宿管复核即为最终结果，学生不可再确认 / 反馈。 */
    private void requireNotReviewed(boolean reviewed, String statusName) {
        if (reviewed) {
            throw new BusinessException("该记录已由宿管复核（" + statusName
                    + "），复核结论为最终结果，不可再修改");
        }
    }

    /** 反馈类说明的统一校验：去空白后不得为空。 */
    private String requireRemark(String remark, String emptyMessage) {
        String note = remark == null ? "" : remark.trim();
        if (note.isEmpty()) {
            throw new BusinessException(emptyMessage);
        }
        return note;
    }
}

package com.nchu.dorm.service;

import com.nchu.dorm.model.Admin;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.announcement.Announcement;
import com.nchu.dorm.model.record.ValuablesRecord;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.util.BusinessException;
import com.nchu.dorm.util.TimeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 公告服务（宿管科）：把宿管上报的贵重物品遗失记录审核后发布为全校公告。
 * <p>完整链路：学生报遗失（{@link StudentLifeService#reportValuablesLost}）
 * → 分管宿管上报（{@link DormStaffService#escalateLostValuables}）
 * → 宿管科在此发布公告，学生端「公告」页可见。</p>
 * <p>公告文案按需生成默认模板（只露学号不露姓名），发布前可由宿管科在界面上修改。</p>
 */
public class AnnouncementService {

    private DataCenter dc() {
        return DataCenter.instance();
    }

    // ==================== 待发布队列 ====================

    /** 宿管已上报、待宿管科发布公告的记录，按上报时间倒序。 */
    public List<ValuablesRecord> pendingPublish() {
        List<ValuablesRecord> list = new ArrayList<>();
        for (ValuablesRecord r : dc().getValuablesRecords()) {
            if (ValuablesRecord.REPORT_ESCALATED.equals(r.getReportStatus())) {
                list.add(r);
            }
        }
        sortByReportTime(list);
        return list;
    }

    /** 已上报（含已发布）的记录——即宿管上报到宿管科的完整历史，供公告页查看。 */
    public List<ValuablesRecord> escalatedAll() {
        List<ValuablesRecord> list = new ArrayList<>();
        for (ValuablesRecord r : dc().getValuablesRecords()) {
            if (ValuablesRecord.REPORT_ESCALATED.equals(r.getReportStatus())
                    || ValuablesRecord.REPORT_PUBLISHED.equals(r.getReportStatus())) {
                list.add(r);
            }
        }
        sortByReportTime(list);
        return list;
    }

    /** 已发布的公告，按发布时间倒序。 */
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

    /** 待发布公告条数（宿管科工作台概览用）。 */
    public int pendingPublishCount() {
        return pendingPublish().size();
    }

    // ==================== 公告文案模板 ====================

    /** 默认公告标题，如「关于某学生遗失「笔记本电脑」的情况通报」。 */
    public String defaultTitle(ValuablesRecord record) {
        return "关于某学生遗失「" + safe(record == null ? null : record.getItemName()) + "」的情况通报";
    }

    /**
     * 默认公告正文：按用户要求的形式「某学生（学号：…）xx 物品遗失…」，
     * 只公布学号不公布姓名，并附上登记时间与楼栋、遗失说明、招领指引。
     */
    public String defaultContent(ValuablesRecord record) {
        if (record == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("经宿舍管理人员核实上报，某学生（学号：").append(safe(record.getStudentId()))
                .append("）的「").append(safe(record.getItemName())).append("」遗失。");
        String place = recordPlace(record.getStudentId());
        String direction = record.getDirectionName();
        if (!place.isEmpty()) {
            sb.append("该物品于 ").append(record.getRecordTime()).append(" 在 ").append(place)
                    .append(" 登记").append(direction).append("。");
        } else {
            sb.append("该物品于 ").append(record.getRecordTime()).append(" 登记").append(direction).append("。");
        }
        if (record.getStudentRemark() != null && !record.getStudentRemark().isEmpty()) {
            sb.append("遗失说明：").append(record.getStudentRemark()).append("。");
        }
        sb.append("如有拾获或发现线索，请及时联系宿管科或该同学。");
        return sb.toString();
    }

    // ==================== 发布 ====================

    /**
     * 发布遗失公告：写入一条 {@link Announcement}，并把来源记录置为「已发布公告」。
     *
     * @param title   公告标题（空则用默认模板）
     * @param content 公告正文（空则用默认模板）
     */
    public Announcement publishLostItem(Admin admin, ValuablesRecord record, String title, String content) {
        if (admin == null) {
            throw new BusinessException("请先登录宿管科账号");
        }
        if (record == null) {
            throw new BusinessException("请先在列表中选择一条待发布的遗失记录");
        }
        if (ValuablesRecord.REPORT_PUBLISHED.equals(record.getReportStatus())) {
            throw new BusinessException("该记录已发布过公告，请勿重复发布");
        }
        if (!ValuablesRecord.REPORT_ESCALATED.equals(record.getReportStatus())) {
            throw new BusinessException("该记录尚未由宿舍管理人员上报宿管科，无法发布公告");
        }
        String finalTitle = title == null || title.trim().isEmpty() ? defaultTitle(record) : title.trim();
        String finalContent = content == null || content.trim().isEmpty() ? defaultContent(record) : content.trim();

        Announcement announcement = new Announcement(dc().nextAnnouncementId(),
                Announcement.TYPE_LOST_ITEM, finalTitle, finalContent,
                admin.getId(), TimeUtil.now(), record.getId());
        dc().getAnnouncements().add(announcement);
        record.setReportStatus(ValuablesRecord.REPORT_PUBLISHED);
        dc().saveAll();
        return announcement;
    }

    // ==================== 私有工具 ====================

    /** 记录所属学生的当前楼栋，如「01A栋」；查不到时返回空串。 */
    private String recordPlace(String studentId) {
        Student s = dc().findStudentById(studentId);
        if (s == null || !s.isCheckedIn()) {
            return "";
        }
        return s.getCurrentBuilding();
    }

    private void sortByReportTime(List<ValuablesRecord> list) {
        Collections.sort(list, new Comparator<ValuablesRecord>() {
            @Override
            public int compare(ValuablesRecord a, ValuablesRecord b) {
                return safe(b.getReportTime()).compareTo(safe(a.getReportTime()));
            }
        });
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}

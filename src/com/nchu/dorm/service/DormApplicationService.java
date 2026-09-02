package com.nchu.dorm.service;

import com.nchu.dorm.model.Bed;
import com.nchu.dorm.model.Building;
import com.nchu.dorm.model.College;
import com.nchu.dorm.model.Counselor;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.application.DormApplication;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.util.BusinessException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 宿舍申请服务。
 * <p>
 * 闭环：学生提交（入住 / 转移 / 退宿 / 转专业换宿）→ 辅导员按类型审批并即时执行数据变更 → 落库。
 * <ul>
 *   <li>入住/转移/退宿：本学院辅导员单级审批（普通三类维持按学院作用域）。</li>
 *   <li>转专业换宿：两级审批——本专业辅导员同意迁出 → 目标专业辅导员同意接收，
 *       两级均同意才成功；任一级拒绝即失败。跨学院转专业才更换宿舍楼（目标学院同性别楼）。</li>
 * </ul>
 * 规则收口在 service，UI 只调用方法。
 */
public class DormApplicationService {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** 班级展示文案缓存：班级编码(6位) -> "学院 · 专业 · 年份级N班"。数据在单进程运行期不变，懒加载一次。 */
    private static final Object CLASS_LABEL_LOCK = new Object();
    private static volatile Map<String, String> CLASS_LABELS;

    // ==================== 提交（学生） ====================

    /**
     * 入住申请：学生须未入住；目标楼栋须属本院、性别匹配、且有可分配房间。
     */
    public DormApplication submitApply(Student student, String targetBuilding, String reason) {
        requireReason(reason);
        if (student.isCheckedIn()) {
            throw new BusinessException("您已入住宿舍，如需调整请走转移/转专业/退宿流程");
        }
        checkOneInFlight(student);
        College college = requireCollegeOf(student);
        checkTargetBuildingOfCollege(student, college, targetBuilding);
        requireBuildingSpare(student, targetBuilding, null);

        DormApplication app = newApplication(student, DormApplication.TYPE_APPLY, targetBuilding, reason);
        dc().getDormApplications().add(app);
        dc().saveAll();
        return app;
    }

    /**
     * 转宿申请：学生须已入住；目标楼栋须属本院、性别匹配，且存在除现居房外的空床房间。
     */
    public DormApplication submitTransfer(Student student, String targetBuilding, String reason) {
        requireReason(reason);
        if (!student.isCheckedIn()) {
            throw new BusinessException("您未入住宿舍，无法申请转宿");
        }
        checkOneInFlight(student);
        College college = requireCollegeOf(student);
        checkTargetBuildingOfCollege(student, college, targetBuilding);
        requireBuildingSpare(student, targetBuilding, student.getCurrentRoom());

        DormApplication app = newApplication(student, DormApplication.TYPE_TRANSFER, targetBuilding, reason);
        snapshotOrigin(app, student);
        dc().getDormApplications().add(app);
        dc().saveAll();
        return app;
    }

    /**
     * 退宿申请：学生须已入住；通过后床位释放、置为未入住。
     */
    public DormApplication submitExit(Student student, String reason) {
        requireReason(reason);
        if (!student.isCheckedIn()) {
            throw new BusinessException("您未入住宿舍，无法申请退宿");
        }
        checkOneInFlight(student);
        requireCollegeOf(student);

        DormApplication app = newApplication(student, DormApplication.TYPE_EXIT, null, reason);
        snapshotOrigin(app, student);
        dc().getDormApplications().add(app);
        dc().saveAll();
        return app;
    }

    /**
     * 转专业换宿申请：学生须已入住；选择目标班级（可跨学院/跨届，但不能与本班同专业年级）。
     * <p>本专业辅导员同意迁出后流转至目标专业辅导员接收；任一级拒绝即失败。
     * 跨学院转专业才搬迁（目标学院同性别楼，目标辅导员选房），同学院不换房。</p>
     */
    public DormApplication submitMajorTransfer(Student student, String targetClass, String reason) {
        requireReason(reason);
        if (!student.isCheckedIn()) {
            throw new BusinessException("您未入住宿舍，无法申请转专业换宿");
        }
        checkOneInFlight(student);
        requireCollegeOf(student);

        if (targetClass == null || targetClass.trim().isEmpty()) {
            throw new BusinessException("请选择转专业后的班级");
        }
        targetClass = targetClass.trim();
        if (!isRealClass(targetClass)) {
            throw new BusinessException("目标班级不存在，请从下拉中选择正确班级");
        }
        if (gradeOfClass(targetClass).equals(gradeOfClass(student.getClassName()))) {
            throw new BusinessException("目标班级与本班属于同一专业年级，无法转专业");
        }
        String targetCollege = collegeOfClass(targetClass);
        String targetBuilding = null;
        if (!targetCollege.equals(student.getCollegeCode())) {
            // 跨学院转专业：需搬迁到目标学院同性别楼
            targetBuilding = genderBuildingOf(targetCollege, student.getGender());
            Building building = findBuilding(targetBuilding);
            if (building == null || !targetCollege.equals(building.getCollegeCode())) {
                throw new BusinessException("目标学院暂未分配该性别宿舍楼");
            }
            requireBuildingSpare(student, targetBuilding, null);
        }

        DormApplication app = newApplication(student, DormApplication.TYPE_MAJOR_TRANSFER, targetBuilding, reason);
        snapshotOrigin(app, student);
        app.setOriginClass(student.getClassName());
        app.setTargetClass(targetClass);
        dc().getDormApplications().add(app);
        dc().saveAll();
        return app;
    }

    // ==================== 辅导员审批 ====================

    /**
     * 通过入住/转宿申请并分配房间床位（APPLY/TRANSFER，PENDING）。
     */
    public void approve(DormApplication app, Counselor counselor, String buildingName, String roomNo, String comment) {
        DataCenter dc = dc();
        checkApprovalOpen(app);
        if (!DormApplication.TYPE_APPLY.equals(app.getType()) && !DormApplication.TYPE_TRANSFER.equals(app.getType())) {
            throw new BusinessException("该申请不适用普通通过流程");
        }
        Student student = requireStudent(app);
        checkCounselorOfCollege(app, counselor, student);
        if (DormApplication.TYPE_APPLY.equals(app.getType())) {
            if (student.isCheckedIn()) {
                throw new BusinessException("该学生已入住，无法执行入住审批");
            }
        } else {
            if (!student.isCheckedIn()) {
                throw new BusinessException("该学生未入住，无法执行转宿审批");
            }
        }
        checkBuildingSame(app, buildingName);
        if (!buildingMatchesGender(buildingName, student.getGender())) {
            throw new BusinessException("目标楼栋与学生性别不匹配");
        }
        Room room = requireRoomOfBuilding(buildingName, roomNo);
        if (DormApplication.TYPE_TRANSFER.equals(app.getType())
                && buildingName.equals(student.getCurrentBuilding())
                && roomNo.equals(student.getCurrentRoom())) {
            throw new BusinessException("目标房间不能是学生现居房间");
        }

        // 先释放原床位（转宿），再占新空床（入住/转宿）
        if (DormApplication.TYPE_TRANSFER.equals(app.getType())) {
            releaseCurrentRoom(student);
        }
        occupyBed(room, student);
        student.setCurrentBuilding(buildingName);
        student.setCurrentRoom(roomNo);
        finishApproved(app, counselor, comment, roomNo);
        dc.saveAll();
    }

    /**
     * 通过退宿申请（EXIT，PENDING）：释放床位、清空住宿，忽略房间参数。
     */
    public void approveExit(DormApplication app, Counselor counselor, String comment) {
        DataCenter dc = dc();
        checkApprovalOpen(app);
        if (!DormApplication.TYPE_EXIT.equals(app.getType())) {
            throw new BusinessException("该申请不是退宿申请");
        }
        Student student = requireStudent(app);
        checkCounselorOfCollege(app, counselor, student);
        if (!student.isCheckedIn()) {
            throw new BusinessException("该学生未入住，无法执行退宿审批");
        }
        releaseCurrentRoom(student);
        finishApproved(app, counselor, comment, null);
        dc.saveAll();
    }

    /**
     * 转专业第一级：本专业辅导员同意迁出（MAJOR_TRANSFER，PENDING → AWAITING_TARGET）。
     * 仅记录同意、不释放床位，目标辅导员接收成功才真正搬迁（目标拒绝可回退原位）。
     */
    public void approveMajorMoveOut(DormApplication app, Counselor counselor, String comment) {
        DataCenter dc = dc();
        checkApprovalOpen(app);
        if (!DormApplication.TYPE_MAJOR_TRANSFER.equals(app.getType())) {
            throw new BusinessException("该申请不是转专业换宿申请");
        }
        Student student = requireStudent(app);
        if (!counselor.getId().equals(gradeOfClass(student.getClassName()))) {
            throw new BusinessException("只能由该生本专业辅导员同意迁出");
        }
        app.setStatus(DormApplication.STATUS_AWAITING_TARGET);
        app.setStep1ReviewerId(counselor.getId());
        app.setStep1ReviewTime(now());
        app.setStep1ReviewComment(comment == null ? "" : comment);
        dc.saveAll();
    }

    /**
     * 转专业第二级：目标专业辅导员同意接收（MAJOR_TRANSFER，AWAITING_TARGET → APPROVED）。
     * 跨学院：释放原房床位 → 目标学院同性别楼占新床 → 更新学生学院/专业/班级档案。
     * 同学院：不换房，仅更新专业/班级档案。
     */
    public void approveMajorAccept(DormApplication app, Counselor counselor,
                                   String buildingName, String roomNo, String comment) {
        DataCenter dc = dc();
        checkApprovalOpen(app);
        if (!DormApplication.TYPE_MAJOR_TRANSFER.equals(app.getType())) {
            throw new BusinessException("该申请不是转专业换宿申请");
        }
        if (!DormApplication.STATUS_AWAITING_TARGET.equals(app.getStatus())) {
            throw new BusinessException("当前未处于待目标专业审批阶段");
        }
        Student student = requireStudent(app);
        String targetClass = app.getTargetClass();
        if (!counselor.getId().equals(gradeOfClass(targetClass))) {
            throw new BusinessException("只能由该生目标专业辅导员同意接收");
        }
        String targetCollege = collegeOfClass(targetClass);
        boolean crossCollege = !targetCollege.equals(student.getCollegeCode());

        if (crossCollege) {
            checkBuildingSame(app, buildingName);
            if (!targetCollege.equals(counselor.getCollegeCode())) {
                throw new BusinessException("目标专业不在您所属学院");
            }
            if (!buildingMatchesGender(buildingName, student.getGender())) {
                throw new BusinessException("目标楼栋与学生性别不匹配");
            }
            Room room = requireRoomOfBuilding(buildingName, roomNo);
            if (buildingName.equals(student.getCurrentBuilding())
                    && roomNo.equals(student.getCurrentRoom())) {
                throw new BusinessException("目标房间不能是学生现居房间");
            }
            releaseCurrentRoom(student);
            occupyBed(room, student);
            student.setCurrentBuilding(buildingName);
            student.setCurrentRoom(roomNo);
        }
        // 学生档案更新为转专业目标（学号不变，仅当前学院/专业/班级可变）
        student.setCollegeCode(targetCollege);
        student.setMajor(majorOfClass(targetClass));
        student.setClassName(targetClass);

        finishApproved(app, counselor, comment, crossCollege ? roomNo : null);
        dc.saveAll();
    }

    /**
     * 辅导员驳回。适用于：普通三类的 PENDING（本学院辅导员）；转专业换宿的 PENDING（本专业辅导员）
     * 或 AWAITING_TARGET（目标专业辅导员）。驳回无任何数据变更。
     */
    public void reject(DormApplication app, Counselor counselor, String comment) {
        DataCenter dc = dc();
        if (app.isTerminal()) {
            throw new BusinessException("该申请已被处理，无法驳回");
        }
        Student student = requireStudent(app);
        if (DormApplication.TYPE_MAJOR_TRANSFER.equals(app.getType())) {
            if (DormApplication.STATUS_PENDING.equals(app.getStatus())
                    && !counselor.getId().equals(gradeOfClass(student.getClassName()))) {
                throw new BusinessException("只能由该生本专业辅导员驳回");
            }
            if (DormApplication.STATUS_AWAITING_TARGET.equals(app.getStatus())
                    && !counselor.getId().equals(gradeOfClass(app.getTargetClass()))) {
                throw new BusinessException("只能由该生目标专业辅导员驳回");
            }
        } else {
            checkCounselorOfCollege(app, counselor, student);
        }
        app.setStatus(DormApplication.STATUS_REJECTED);
        app.setReviewerId(counselor.getId());
        app.setReviewTime(now());
        app.setReviewComment(comment == null ? "" : comment);
        dc.saveAll();
    }

    /**
     * 学生撤销自己的在办申请（PENDING / AWAITING_TARGET）。撤销无任何床位变更。
     */
    public void cancel(Student student, DormApplication app) {
        if (app == null || !student.getId().equals(app.getStudentId())) {
            throw new BusinessException("只能撤销本人提交的申请");
        }
        if (!app.isPendingLike()) {
            throw new BusinessException("该申请已被处理，无法撤销");
        }
        app.setStatus(DormApplication.STATUS_CANCELLED);
        dc().saveAll();
    }

    // ==================== 查询 ====================

    /**
     * 辅导员当前可处理清单：
     * <ul>
     *   <li>普通 APPLY/TRANSFER/EXIT 的 PENDING（按学生当前学院，本院任意辅导员可批）；</li>
     *   <li>转专业换宿 PENDING（仅该生本专业辅导员，按辅导员 id=专业年级代码）；</li>
     *   <li>转专业换宿 AWAITING_TARGET（仅目标专业辅导员）。</li>
     * </ul>
     */
    public List<DormApplication> findActionableOf(Counselor counselor) {
        List<DormApplication> result = new ArrayList<>();
        for (DormApplication app : dc().getDormApplications()) {
            if (app.isTerminal()) {
                continue;
            }
            Student student = dc().findStudentById(app.getStudentId());
            if (student == null) {
                continue;
            }
            if (DormApplication.TYPE_MAJOR_TRANSFER.equals(app.getType())) {
                if (DormApplication.STATUS_PENDING.equals(app.getStatus())) {
                    if (counselor.getId().equals(gradeOfClass(student.getClassName()))) {
                        result.add(app);
                    }
                } else if (DormApplication.STATUS_AWAITING_TARGET.equals(app.getStatus())
                        && counselor.getId().equals(gradeOfClass(app.getTargetClass()))) {
                    result.add(app);
                }
            } else if (DormApplication.STATUS_PENDING.equals(app.getStatus())
                    && student.getCollegeCode().equals(counselor.getCollegeCode())) {
                result.add(app);
            }
        }
        return result;
    }

    /**
     * 辅导员历史（终态记录）。可见性 = 学生当前学院 == 本学院；
     * 或转专业换宿的迁出/目标班级所属学院 == 本学院（保证跨学院转专业两侧都能追溯）。
     */
    public List<DormApplication> findHistoryOf(Counselor counselor) {
        List<DormApplication> result = new ArrayList<>();
        for (DormApplication app : dc().getDormApplications()) {
            if (!app.isTerminal()) {
                continue;
            }
            Student student = dc().findStudentById(app.getStudentId());
            boolean visible = student != null && student.getCollegeCode().equals(counselor.getCollegeCode());
            if (!visible && DormApplication.TYPE_MAJOR_TRANSFER.equals(app.getType())) {
                visible = collegeOfClass(app.getOriginClass()).equals(counselor.getCollegeCode())
                        || collegeOfClass(app.getTargetClass()).equals(counselor.getCollegeCode());
            }
            if (visible) {
                result.add(app);
            }
        }
        Collections.sort(result, new Comparator<DormApplication>() {
            @Override
            public int compare(DormApplication a, DormApplication b) {
                return b.getCreateTime().compareTo(a.getCreateTime());
            }
        });
        return result;
    }

    /**
     * 查询某学生提交的全部申请（学生视角，时间倒序）。
     */
    public List<DormApplication> findApplicationsOfStudent(String studentId) {
        List<DormApplication> result = new ArrayList<>();
        for (DormApplication app : dc().getDormApplications()) {
            if (app.getStudentId().equals(studentId)) {
                result.add(app);
            }
        }
        Collections.sort(result, new Comparator<DormApplication>() {
            @Override
            public int compare(DormApplication a, DormApplication b) {
                return b.getCreateTime().compareTo(a.getCreateTime());
            }
        });
        return result;
    }

    // ==================== 班级编码工具（供 UI 复用） ====================

    /** 班级编码 -> 学院代码：第 3-4 位。 */
    public static String collegeOfClass(String className) {
        return className != null && className.length() >= 6 ? className.substring(2, 4) : "";
    }

    /** 班级编码 -> 专业年级代码（辅导员 id）：前 5 位。 */
    public static String gradeOfClass(String className) {
        return className != null && className.length() >= 5 ? className.substring(0, 5) : (className == null ? "" : className);
    }

    /** 班级编码是否真实存在（6 位且为运行数据中的班级）。 */
    public static boolean isRealClass(String className) {
        if (className == null || className.length() != 6) {
            return false;
        }
        for (int i = 0; i < className.length(); i++) {
            if (!Character.isDigit(className.charAt(i))) {
                return false;
            }
        }
        ensureClassLabels();
        return CLASS_LABELS.containsKey(className);
    }

    /** 班级展示文案，如 "外国语学院 · 英语 · 2025级3班"；非法/未知班级返回原编码。 */
    public static String classLabel(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        if (!isRealClass(className)) {
            return className;
        }
        ensureClassLabels();
        return CLASS_LABELS.get(className);
    }

    // ==================== 私有工具 ====================

    private DataCenter dc() {
        return DataCenter.instance();
    }

    private String now() {
        return DATE_FMT.format(new Date());
    }

    private DormApplication newApplication(Student student, String type, String targetBuilding, String reason) {
        return new DormApplication(dc().nextApplicationId(), student.getId(), type, targetBuilding,
                reason, DormApplication.STATUS_PENDING, now());
    }

    private void snapshotOrigin(DormApplication app, Student student) {
        app.setOriginBuilding(student.getCurrentBuilding());
        app.setOriginRoom(student.getCurrentRoom());
    }

    private void requireReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("请填写申请原因");
        }
    }

    private void checkOneInFlight(Student student) {
        for (DormApplication app : dc().getDormApplications()) {
            if (app.getStudentId().equals(student.getId()) && app.isPendingLike()) {
                throw new BusinessException("您已有待审批申请（编号 " + app.getId() + "），请等待处理或先撤销");
            }
        }
    }

    private College requireCollegeOf(Student student) {
        College college = dc().findCollegeByCode(student.getCollegeCode());
        if (college == null) {
            throw new BusinessException("未找到所属学院，无法申请");
        }
        return college;
    }

    private void checkTargetBuildingOfCollege(Student student, College college, String buildingName) {
        if (buildingName == null || buildingName.isEmpty()) {
            throw new BusinessException("请选择目标楼栋");
        }
        if (!college.getBuildingNames().contains(buildingName)) {
            throw new BusinessException("目标楼栋不属于本学院分配范围");
        }
        if (!buildingMatchesGender(buildingName, student.getGender())) {
            throw new BusinessException("目标楼栋与学生性别不匹配");
        }
        if (findBuilding(buildingName) == null) {
            throw new BusinessException("目标楼栋不存在");
        }
    }

    /** 目标楼栋内是否有（除指定房间外的）空床房间。roomNo 为 null 时不排除。 */
    private void requireBuildingSpare(Student student, String buildingName, String excludeRoomNo) {
        for (Room r : dc().findRoomsOfBuilding(buildingName)) {
            if (r.isFull()) {
                continue;
            }
            if (excludeRoomNo != null && excludeRoomNo.equals(r.getRoomNo())) {
                continue;
            }
            return; // 存在可用房间
        }
        throw new BusinessException("目标楼栋暂无空床房间，请联系辅导员");
    }

    private Student requireStudent(DormApplication app) {
        Student student = dc().findStudentById(app.getStudentId());
        if (student == null) {
            throw new BusinessException("申请学生不存在");
        }
        return student;
    }

    private void checkCounselorOfCollege(DormApplication app, Counselor counselor, Student student) {
        if (!student.getCollegeCode().equals(counselor.getCollegeCode())) {
            throw new BusinessException("只能审批本学院学生的申请");
        }
    }

    private void checkApprovalOpen(DormApplication app) {
        if (app == null || !DormApplication.STATUS_PENDING.equals(app.getStatus())
                && !DormApplication.STATUS_AWAITING_TARGET.equals(app.getStatus())) {
            throw new BusinessException("该申请已被处理，无法重复操作");
        }
    }

    private void checkBuildingSame(DormApplication app, String buildingName) {
        String expected = app.getTargetBuilding();
        if (expected == null || expected.isEmpty() || !expected.equals(buildingName)) {
            throw new BusinessException("目标楼栋与申请不符，请选择正确房间");
        }
    }

    private Room requireRoomOfBuilding(String buildingName, String roomNo) {
        if (roomNo == null || roomNo.trim().isEmpty()) {
            throw new BusinessException("请选择分配房间");
        }
        Room room = dc().findRoom(buildingName, roomNo.trim());
        if (room == null) {
            throw new BusinessException("目标房间不存在");
        }
        return room;
    }

    private void releaseCurrentRoom(Student student) {
        Room room = dc().findRoom(student.getCurrentBuilding(), student.getCurrentRoom());
        if (room != null) {
            room.removeStudent(student.getId());
        }
        student.setCurrentBuilding(null);
        student.setCurrentRoom(null);
    }

    private void occupyBed(Room room, Student student) {
        Bed bed = room.findEmptyBed();
        if (bed == null) {
            throw new BusinessException("该房间已住满，请选择其他房间");
        }
        bed.setOccupantId(student.getId());
    }

    private void finishApproved(DormApplication app, Counselor counselor, String comment, String roomNo) {
        app.setTargetRoom(roomNo == null ? null : roomNo);
        app.setStatus(DormApplication.STATUS_APPROVED);
        app.setReviewerId(counselor.getId());
        app.setReviewTime(now());
        app.setReviewComment(comment == null ? "" : comment);
    }

    private Building findBuilding(String name) {
        return dc().findBuilding(name);
    }

    private boolean buildingMatchesGender(String buildingName, String gender) {
        boolean male = "男".equals(gender);
        return male == (buildingName != null && buildingName.endsWith("A栋"));
    }

    private String genderBuildingOf(String collegeCode, String gender) {
        return collegeCode + ("男".equals(gender) ? "A栋" : "B栋");
    }

    /** 某班级对应的专业名（取该班任一学生的 major）。 */
    private String majorOfClass(String className) {
        for (Student s : dc().getStudents()) {
            if (className.equals(s.getClassName())) {
                return s.getMajor();
            }
        }
        return null;
    }

    // ==================== 班级展示缓存 ====================

    private static void ensureClassLabels() {
        if (CLASS_LABELS != null) {
            return;
        }
        synchronized (CLASS_LABEL_LOCK) {
            if (CLASS_LABELS != null) {
                return;
            }
            DataCenter dc = DataCenter.instance();
            Map<String, String> majorOf = new HashMap<>();
            for (Student s : dc.getStudents()) {
                String cls = s.getClassName();
                if (cls == null || cls.length() < 6) {
                    continue;
                }
                String key = cls.substring(0, 6);
                if (!majorOf.containsKey(key)) {
                    majorOf.put(key, s.getMajor());
                }
            }
            Map<String, String> labels = new HashMap<>();
            for (Map.Entry<String, String> e : majorOf.entrySet()) {
                String cls = e.getKey();
                labels.put(cls, describeClass(cls, e.getValue()));
            }
            CLASS_LABELS = labels;
        }
    }

    private static String describeClass(String cls, String major) {
        String college = DataCenter.instance().collegeName(cls.substring(2, 4));
        String yearText = "20" + cls.substring(0, 2);
        return (college == null || college.isEmpty() ? cls.substring(2, 4) : college)
                + " · " + (major == null ? "" : major)
                + " · " + yearText + "级" + cls.substring(5, 6) + "班";
    }
}

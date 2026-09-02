package com.nchu.dorm.storage;

import com.nchu.dorm.model.Account;
import com.nchu.dorm.model.Admin;
import com.nchu.dorm.model.Building;
import com.nchu.dorm.model.College;
import com.nchu.dorm.model.Counselor;
import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.model.Person;
import com.nchu.dorm.model.RoleKey;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.StudentId;
import com.nchu.dorm.model.application.DormApplication;
import com.nchu.dorm.model.application.ElectricityPurchase;
import com.nchu.dorm.model.application.RepairTicket;
import com.nchu.dorm.model.record.HygieneRecord;
import com.nchu.dorm.model.record.NightReturnRecord;
import com.nchu.dorm.model.record.ValuablesRecord;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * 数据中心（单例门面）。
 * <p>
 * 持有全部实体列表，统一负责文本文件的加载/保存，并提供业务层使用的查询与编号生成方法。
 * 业务层与界面层一律通过 {@link #instance()} 访问。
 */
public class DataCenter {

    /** 数据目录：项目根目录下的 data 文件夹（必须声明在 INSTANCE 之前，避免静态初始化顺序问题） */
    private static final java.nio.file.Path DATA_DIR = Paths.get("data");

    private static final DataCenter INSTANCE = new DataCenter();

    // ---- 数据文件名 ----
    private static final String FILE_COLLEGES = "colleges.txt";
    private static final String FILE_BUILDINGS = "buildings.txt";
    private static final String FILE_ROOMS = "rooms.txt";
    private static final String FILE_STUDENTS = "students.txt";
    private static final String FILE_COUNSELORS = "counselors.txt";
    private static final String FILE_DORM_STAFFS = "dorm_staffs.txt";
    private static final String FILE_ADMINS = "admins.txt";
    private static final String FILE_ACCOUNTS = "accounts.txt";
    private static final String FILE_APPLICATIONS = "dorm_applications.txt";
    private static final String FILE_NIGHT_RETURNS = "night_return_records.txt";
    private static final String FILE_HYGIENE = "hygiene_records.txt";
    private static final String FILE_VALUABLES = "valuables_records.txt";
    private static final String FILE_REPAIRS = "repair_tickets.txt";
    private static final String FILE_ELECTRICITY = "electricity_purchases.txt";

    // ==================== 全量数据生成参数（课程设定，可整体调整） ====================

    /** 需创建账号的入学年份 */
    private static final int[] ADMISSION_YEARS = {2024, 2025};

    /** 每学院专业数 */
    private static final int MAJORS_PER_COLLEGE = 4;

    /** 每专业班级数 */
    private static final int CLASSES_PER_MAJOR = 4;

    /** 每班学生数 */
    private static final int STUDENTS_PER_CLASS = 40;

    /** 每层宿舍间数（按用户设定改为 45 间/层） */
    private static final int ROOMS_PER_FLOOR = 45;

    /** 每间床位（4 人间） */
    private static final int BEDS_PER_ROOM = 4;

    /**
     * 学院清单：{学院代码, 学院名称, 文理分类("文"/"理"), 专业1..专业4}。
     * 分类决定男女配比：文学院每班 32 女 + 8 男，理学院每班 8 女 + 32 男（女学号在前）。
     * 专业顺序即专业代码 1-4；软件学院（20）第 1 个专业为软件工程 → 专业代码 1，
     * 其专业年级代码为 25201（25级+20学院+1专业），辅导员账号 counselor25201。
     */
    private static final String[][] COLLEGE_MAJORS = {
            {"01", "材料科学与工程学院", "理", "金属材料工程", "材料成型及控制工程", "焊接技术与工程", "高分子材料与工程"},
            {"02", "环境与化学工程学院", "理", "环境工程", "化学工程与工艺", "应用化学", "材料化学"},
            {"03", "机电学院", "理", "机械设计制造及其自动化", "机械电子工程", "飞行器制造工程", "电气工程及其自动化"},
            {"04", "信息工程学院", "理", "计算机科学与技术", "电子信息工程", "通信工程", "自动化"},
            {"05", "航空宇航学院", "理", "飞行器设计与工程", "航空航天工程", "探测制导与控制技术", "无人驾驶航空器系统工程"},
            {"06", "动力与能源学院", "理", "能源与动力工程", "飞行器动力工程", "新能源科学与工程", "储能科学与工程"},
            {"07", "数学与信息科学学院", "理", "数学与应用数学", "信息与计算科学", "统计学", "数据科学与大数据技术"},
            {"08", "仪器科学与光电工程学院", "理", "测控技术与仪器", "光电信息科学与工程", "生物医学工程", "智能感知工程"},
            {"09", "经济管理学院", "文", "经济学", "国际经济与贸易", "工商管理", "会计学"},
            {"10", "体育学院", "理", "体育教育", "社会体育指导与管理", "休闲体育", "运动训练"},
            {"11", "民航与交通学院", "理", "交通运输", "飞行技术", "交通工程", "民航安全技术管理"},
            {"12", "艺术设计学院", "文", "视觉传达设计", "环境设计", "产品设计", "数字媒体艺术"},
            {"13", "马克思主义学院", "文", "思想政治教育", "马克思主义理论", "中国共产党历史", "哲学"},
            {"14", "文法学院", "文", "法学", "汉语言文学", "新闻学", "行政管理"},
            {"15", "航空服务与音乐学院", "文", "音乐表演", "航空服务艺术与管理", "舞蹈表演", "作曲与作曲技术理论"},
            {"16", "外国语学院", "文", "英语", "日语", "翻译", "商务英语"},
            {"20", "软件学院", "理", "软件工程", "信息安全", "人工智能", "网络工程"},
    };

    private final Storage storage = new TextStorage(DATA_DIR);

    // ---- 内存数据 ----
    private final List<College> colleges = new ArrayList<>();
    private final List<Building> buildings = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private final List<Student> students = new ArrayList<>();
    private final List<Counselor> counselors = new ArrayList<>();
    private final List<DormStaff> dormStaffs = new ArrayList<>();
    private final List<Admin> admins = new ArrayList<>();
    private final List<Account> accounts = new ArrayList<>();
    private final List<DormApplication> dormApplications = new ArrayList<>();
    private final List<NightReturnRecord> nightReturnRecords = new ArrayList<>();
    private final List<HygieneRecord> hygieneRecords = new ArrayList<>();
    private final List<ValuablesRecord> valuablesRecords = new ArrayList<>();
    private final List<RepairTicket> repairTickets = new ArrayList<>();
    private final List<ElectricityPurchase> electricityPurchases = new ArrayList<>();

    private DataCenter() {
    }

    public static DataCenter instance() {
        return INSTANCE;
    }

    // ==================== 加载 / 保存 ====================

    /**
     * 加载全部数据。首次运行（无账号文件）时自动生成演示数据并保存。
     */
    public void loadAll() {
        try {
            if (!storage.hasFile(FILE_ACCOUNTS)) {
                seedPreset();
            }
            load(FILE_COLLEGES, College::fromLine, colleges);
            load(FILE_BUILDINGS, Building::fromLine, buildings);
            load(FILE_ROOMS, Room::fromLine, rooms);
            load(FILE_STUDENTS, Student::fromLine, students);
            load(FILE_COUNSELORS, Counselor::fromLine, counselors);
            load(FILE_DORM_STAFFS, DormStaff::fromLine, dormStaffs);
            load(FILE_ADMINS, Admin::fromLine, admins);
            load(FILE_ACCOUNTS, Account::fromLine, accounts);
            load(FILE_APPLICATIONS, DormApplication::fromLine, dormApplications);
            load(FILE_NIGHT_RETURNS, NightReturnRecord::fromLine, nightReturnRecords);
            load(FILE_HYGIENE, HygieneRecord::fromLine, hygieneRecords);
            load(FILE_VALUABLES, ValuablesRecord::fromLine, valuablesRecords);
            load(FILE_REPAIRS, RepairTicket::fromLine, repairTickets);
            load(FILE_ELECTRICITY, ElectricityPurchase::fromLine, electricityPurchases);
        } catch (IOException e) {
            throw new IllegalStateException("数据加载失败：" + e.getMessage(), e);
        }
    }

    /**
     * 保存全部数据到文本文件。
     */
    public void saveAll() {
        try {
            save(FILE_COLLEGES, "学院表：code|name|楼栋1;楼栋2", colleges, College::toLine);
            save(FILE_BUILDINGS, "楼栋表：name|alias|collegeCode|managerId|floorCount|hasBathroom", buildings, Building::toLine);
            save(FILE_ROOMS, "房间表：buildingName|roomNo|floor|capacity|床位号=学号;床位号=学号", rooms, Room::toLine);
            save(FILE_STUDENTS, "学生表：id|name|gender|phone|collegeCode|major|className|currentBuilding|currentRoom", students, Student::toLine);
            save(FILE_COUNSELORS, "辅导员表：id|name|gender|phone|collegeCode|jobTitle", counselors, Counselor::toLine);
            save(FILE_DORM_STAFFS, "宿舍管理人员表：id|name|gender|phone|collegeCode|jobTitle|负责楼栋;楼栋", dormStaffs, DormStaff::toLine);
            save(FILE_ADMINS, "宿管科表：id|name|gender|phone|collegeCode|jobTitle", admins, Admin::toLine);
            save(FILE_ACCOUNTS, "账号表：username|password|personId|roleKey", accounts, Account::toLine);
            save(FILE_APPLICATIONS, "宿舍申请表：id|studentId|type|targetBuilding|targetRoom|reason|status|createTime|reviewerId|reviewTime|reviewComment|originBuilding|originRoom|targetClass|originClass|step1ReviewerId|step1ReviewTime|step1ReviewComment", dormApplications, DormApplication::toLine);
            save(FILE_NIGHT_RETURNS, "夜归记录表：id|studentId|date|returnTime|reason", nightReturnRecords, NightReturnRecord::toLine);
            save(FILE_HYGIENE, "卫生检查表：id|roomKey|date|score|inspectorId|comment", hygieneRecords, HygieneRecord::toLine);
            save(FILE_VALUABLES, "贵重物品出入表：id|studentId|itemName|direction|recordTime|handlerId", valuablesRecords, ValuablesRecord::toLine);
            save(FILE_REPAIRS, "维修工单表：id|roomKey|reporterId|description|status|createTime|handlerId|handleTime", repairTickets, RepairTicket::toLine);
            save(FILE_ELECTRICITY, "购电记录表：id|roomKey|buyerId|degree|unitPrice|amount|createTime", electricityPurchases, ElectricityPurchase::toLine);
        } catch (IOException e) {
            throw new IllegalStateException("数据保存失败：" + e.getMessage(), e);
        }
    }

    private <T> void load(String fileKey, Function<String, T> parser, List<T> target) throws IOException {
        target.clear();
        target.addAll(storage.load(fileKey, parser));
    }

    private <T> void save(String fileKey, String header, List<T> items, Function<T, String> serializer) throws IOException {
        storage.save(fileKey, header, items, serializer);
    }

    // ==================== 查询方法 ====================

    public List<College> getColleges() {
        return colleges;
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public List<Student> getStudents() {
        return students;
    }

    public List<Counselor> getCounselors() {
        return counselors;
    }

    public List<DormStaff> getDormStaffs() {
        return dormStaffs;
    }

    public List<Admin> getAdmins() {
        return admins;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public List<DormApplication> getDormApplications() {
        return dormApplications;
    }

    public List<NightReturnRecord> getNightReturnRecords() {
        return nightReturnRecords;
    }

    public List<HygieneRecord> getHygieneRecords() {
        return hygieneRecords;
    }

    public List<ValuablesRecord> getValuablesRecords() {
        return valuablesRecords;
    }

    public List<RepairTicket> getRepairTickets() {
        return repairTickets;
    }

    public List<ElectricityPurchase> getElectricityPurchases() {
        return electricityPurchases;
    }

    public Account findAccount(String username) {
        for (Account a : accounts) {
            if (a.getUsername().equals(username)) {
                return a;
            }
        }
        return null;
    }

    /**
     * 按人员唯一标识查找人员（返回基类，体现多态）。
     */
    public Person findPersonById(String id) {
        for (Student s : students) {
            if (s.getId().equals(id)) {
                return s;
            }
        }
        for (Counselor c : counselors) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        for (DormStaff d : dormStaffs) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        for (Admin a : admins) {
            if (a.getId().equals(id)) {
                return a;
            }
        }
        return null;
    }

    public Student findStudentById(String id) {
        for (Student s : students) {
            if (s.getId().equals(id)) {
                return s;
            }
        }
        return null;
    }

    public Counselor findCounselorById(String id) {
        for (Counselor c : counselors) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    public College findCollegeByCode(String code) {
        for (College c : colleges) {
            if (c.getCode().equals(code)) {
                return c;
            }
        }
        return null;
    }

    public College findCollegeByName(String name) {
        for (College c : colleges) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public String collegeName(String code) {
        College c = findCollegeByCode(code);
        return c == null ? code : c.getName();
    }

    public Building findBuilding(String name) {
        for (Building b : buildings) {
            if (b.getName().equals(name)) {
                return b;
            }
        }
        return null;
    }

    public Room findRoom(String buildingName, String roomNo) {
        for (Room r : rooms) {
            if (r.getBuildingName().equals(buildingName) && r.getRoomNo().equals(roomNo)) {
                return r;
            }
        }
        return null;
    }

    public List<Room> findRoomsOfBuilding(String buildingName) {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.getBuildingName().equals(buildingName)) {
                result.add(r);
            }
        }
        return result;
    }

    /** 某楼栋内还有空床的房间 */
    public List<Room> findAvailableRooms(String buildingName) {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.getBuildingName().equals(buildingName) && !r.isFull()) {
                result.add(r);
            }
        }
        return result;
    }

    /** 某学院被分配的楼栋 */
    public List<Building> findBuildingsOfCollege(String collegeCode) {
        List<Building> result = new ArrayList<>();
        for (Building b : buildings) {
            if (collegeCode.equals(b.getCollegeCode())) {
                result.add(b);
            }
        }
        return result;
    }

    /** 分管某楼栋的宿舍管理人员 */
    public DormStaff findDormStaffByBuilding(String buildingName) {
        for (DormStaff d : dormStaffs) {
            if (d.manages(buildingName)) {
                return d;
            }
        }
        return null;
    }

    // ==================== 编号生成 ====================

    private <T> int maxSuffix(List<T> items, Function<T, String> idGetter, String prefix) {
        int max = 0;
        for (T item : items) {
            String id = idGetter.apply(item);
            if (id != null && id.startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                    // 非纯数字后缀则忽略
                }
            }
        }
        return max;
    }

    public String nextApplicationId() {
        return "AP" + String.format("%04d", maxSuffix(dormApplications, DormApplication::getId, "AP") + 1);
    }

    public String nextRepairId() {
        return "RP" + String.format("%04d", maxSuffix(repairTickets, RepairTicket::getId, "RP") + 1);
    }

    public String nextElectricityId() {
        return "EL" + String.format("%04d", maxSuffix(electricityPurchases, ElectricityPurchase::getId, "EL") + 1);
    }

    public String nextNightReturnId() {
        return "NR" + String.format("%04d", maxSuffix(nightReturnRecords, NightReturnRecord::getId, "NR") + 1);
    }

    public String nextHygieneId() {
        return "HG" + String.format("%04d", maxSuffix(hygieneRecords, HygieneRecord::getId, "HG") + 1);
    }

    public String nextValuablesId() {
        return "VA" + String.format("%04d", maxSuffix(valuablesRecords, ValuablesRecord::getId, "VA") + 1);
    }

    // ==================== 首次运行演示数据 ====================

    /**
     * 生成初始全量数据。
     * 依据课程设定：每学院 4 专业 × 每专业 4 班 × 每班 40 人，覆盖 2024、2025 两届；
     * 宿舍按"每学院 A=男 / B=女 两栋，每层 45 间、每间 4 人，按学号顺序排宿"。
     */
    private void seedPreset() {
        seedColleges();
        seedStudents();
        seedCounselors();
        seedDormStaffAndAdmin();
        seedBuildingsAndRooms();
        seedAccounts();
        saveAll();
    }

    /**
     * 学院代码与 StudentId.VALID_COLLEGE_CODES、data/colleges.txt 保持一致。
     * 每个学院分配两栋宿舍：<学院代码>A栋（男）、<学院代码>B栋（女），楼栋对象在 seedBuildingsAndRooms 中生成。
     */
    private void seedColleges() {
        for (String[] row : COLLEGE_MAJORS) {
            String code = row[0];
            List<String> buildingNames = new ArrayList<>();
            buildingNames.add(code + "A栋");
            buildingNames.add(code + "B栋");
            colleges.add(new College(code, row[1], buildingNames));
        }
    }

    /** 每个专业每年级建一个辅导员账号：专业年级代码 = 入学年后两位 + 学院代码 + 专业代码，账号前缀 counselor。 */
    private void seedCounselors() {
        for (String[] row : COLLEGE_MAJORS) {
            String collegeCode = row[0];
            for (int year : ADMISSION_YEARS) {
                for (int majorCode = 1; majorCode <= MAJORS_PER_COLLEGE; majorCode++) {
                    String gradeCode = String.format("%02d", year % 100) + collegeCode + majorCode;
                    counselors.add(new Counselor(gradeCode, gradeCode, "", "", collegeCode, "辅导员"));
                }
            }
        }
    }

    /**
     * 生成全部学生。女生学号在前（文科 32 女 + 8 男、理科 8 女 + 32 男），
     * 学号序号按"先女后男"的自然序 01-40 递增；姓名默认 = 学号。
     */
    private void seedStudents() {
        for (String[] row : COLLEGE_MAJORS) {
            String collegeCode = row[0];
            boolean liberalArts = "文".equals(row[2]);
            int girlsPerClass = liberalArts ? 32 : 8;
            for (int year : ADMISSION_YEARS) {
                int yearCode = year % 100; // StudentId 只存年份后两位
                for (int majorCode = 1; majorCode <= MAJORS_PER_COLLEGE; majorCode++) {
                    String majorName = row[2 + majorCode]; // row[3..6] 为专业1..4
                    for (int classCode = 1; classCode <= CLASSES_PER_MAJOR; classCode++) {
                        for (int no = 1; no <= STUDENTS_PER_CLASS; no++) {
                            String gender = no <= girlsPerClass ? "女" : "男";
                            String id = StudentId.of(yearCode, collegeCode, majorCode, classCode, no);
                            String className = id.substring(0, 6); // 前 6 位=届+院+专业+班
                            students.add(new Student(id, id, gender, "", collegeCode, majorName, className));
                        }
                    }
                }
            }
        }
    }

    /** 宿管科、宿舍管理人员（演示账号保留）。 */
    private void seedDormStaffAndAdmin() {
        DormStaff dormStaff = new DormStaff("LD001", "李芳", "女", "13900000002", "00", "楼栋管理员");
        dormStaff.getManageBuildingNames().addAll(Arrays.asList("01A栋", "01B栋"));
        dormStaffs.add(dormStaff);

        admins.add(new Admin("SK001", "刘敏", "女", "13900000003", "00", "宿管科科长"));
    }

    private void seedBuildingsAndRooms() {
        for (String[] row : COLLEGE_MAJORS) {
            String collegeCode = row[0];
            List<Student> males = new ArrayList<>();
            List<Student> females = new ArrayList<>();
            for (Student s : students) {
                if (!collegeCode.equals(s.getCollegeCode())) {
                    continue;
                }
                if ("男".equals(s.getGender())) {
                    males.add(s);
                } else {
                    females.add(s);
                }
            }
            // students 按学号升序生成，分区后各性别仍保持升序 → 按学号顺序排宿
            addBuildingAndAssign(collegeCode, collegeCode + "A栋", males);
            addBuildingAndAssign(collegeCode, collegeCode + "B栋", females);
        }
    }

    /** 为某性别的宿舍楼建楼并逐层逐间排宿：房间号=层*100+本层序号(1..45)，床位 1..4。 */
    private void addBuildingAndAssign(String collegeCode, String buildingName, List<Student> occupants) {
        int floors = neededFloors(occupants.size());
        buildings.add(new Building(buildingName, "", collegeCode, "", floors, true));
        int idx = 0;
        for (int f = 1; f <= floors; f++) {
            for (int n = 1; n <= ROOMS_PER_FLOOR; n++) {
                String roomNo = "" + (f * 100 + n);
                Room room = new Room(buildingName, roomNo, f, BEDS_PER_ROOM);
                rooms.add(room);
                for (int bed = 0; bed < BEDS_PER_ROOM && idx < occupants.size(); bed++) {
                    Student s = occupants.get(idx);
                    room.getBeds().get(bed).setOccupantId(s.getId());
                    s.setCurrentBuilding(buildingName);
                    s.setCurrentRoom(roomNo);
                    idx++;
                }
            }
        }
    }

    /** 依该性别总人数计算宿舍楼层数：ceil(ceil(n/4)/45)。 */
    private int neededFloors(int studentCount) {
        int rooms = (studentCount + BEDS_PER_ROOM - 1) / BEDS_PER_ROOM;
        return (rooms + ROOMS_PER_FLOOR - 1) / ROOMS_PER_FLOOR;
    }

    private void seedAccounts() {
        accounts.add(new Account("admin", "admin123", "SK001", RoleKey.ADMIN));
        accounts.add(new Account("ld001", "123456", "LD001", RoleKey.DORM_STAFF));
        for (Student s : students) {
            accounts.add(new Account(s.getId(), "123456", s.getId(), RoleKey.STUDENT));
        }
        for (Counselor c : counselors) {
            accounts.add(new Account("counselor" + c.getId(), "123456", c.getId(), RoleKey.COUNSELOR));
        }
    }
}

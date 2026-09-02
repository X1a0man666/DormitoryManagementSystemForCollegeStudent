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
            save(FILE_APPLICATIONS, "宿舍申请表：id|studentId|type|targetBuilding|targetRoom|reason|status|createTime|reviewerId|reviewTime|reviewComment", dormApplications, DormApplication::toLine);
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
     * 生成初始演示数据。
     * 学院、楼栋信息依据南昌航空大学公开资料；楼栋与学院的具体分配为占位数据，可由宿管科后续调整。
     */
    private void seedPreset() {
        seedColleges();
        seedBuildings();
        seedRooms();
        seedPersons();
        seedAccounts();
        seedApplications();
        saveAll();
    }

    /**
     * 学院代码与 StudentId.VALID_COLLEGE_CODES、data/colleges.txt 保持一致。
     * 楼栋->学院分配为占位数据，可由宿管科后续调整。
     */
    private void seedColleges() {
        colleges.add(new College("01", "材料科学与工程学院", Arrays.asList("1栋", "2栋")));
        colleges.add(new College("02", "环境与化学工程学院", Arrays.asList("3栋", "4栋")));
        colleges.add(new College("03", "机电学院", Arrays.asList("5栋")));
        colleges.add(new College("04", "信息工程学院", Arrays.asList("6栋")));
        colleges.add(new College("05", "航空宇航学院", new ArrayList<String>()));
        colleges.add(new College("06", "动力与能源学院", Arrays.asList("7栋")));
        colleges.add(new College("07", "数学与信息科学学院", new ArrayList<String>()));
        colleges.add(new College("08", "仪器科学与光电工程学院", Arrays.asList("8栋")));
        colleges.add(new College("09", "经济管理学院", new ArrayList<String>()));
        colleges.add(new College("10", "体育学院", new ArrayList<String>()));
        colleges.add(new College("11", "民航与交通学院", new ArrayList<String>()));
        colleges.add(new College("12", "艺术设计学院", new ArrayList<String>()));
        colleges.add(new College("13", "马克思主义学院", new ArrayList<String>()));
        colleges.add(new College("14", "文法学院", new ArrayList<String>()));
        colleges.add(new College("15", "航空服务与音乐学院", new ArrayList<String>()));
        colleges.add(new College("16", "外国语学院", new ArrayList<String>()));
        colleges.add(new College("20", "软件学院", new ArrayList<String>()));
    }

    private void seedBuildings() {
        // 依据调研：全校共32栋标准学生公寓；1-6栋无独立卫浴，7-28栋有独立卫浴；楼栋最高7层。
        for (int i = 1; i <= 32; i++) {
            String name = i + "栋";
            String collegeCode = buildingCollege(name);
            String manager = ("1栋".equals(name) || "2栋".equals(name) || "3栋".equals(name)) ? "LD001" : "";
            String alias = "1栋".equals(name) ? "天清苑" : "";
            buildings.add(new Building(name, alias, collegeCode, manager, 7, i >= 7));
        }
    }

    private String buildingCollege(String name) {
        switch (name) {
            case "1栋":
            case "2栋":
                return "01";
            case "3栋":
            case "4栋":
                return "02";
            case "5栋":
                return "03";
            case "6栋":
                return "04";
            case "7栋":
                return "06";
            case "8栋":
                return "08";
            default:
                return "";
        }
    }

    private void seedRooms() {
        addRooms("1栋", 3, 10);
        addRooms("2栋", 2, 10);
        addRooms("3栋", 1, 6);
        // 给部分床位安排预置入住学生
        Room r101 = findRoom("1栋", "101");
        r101.getBeds().get(0).setOccupantId("24010101");
        r101.getBeds().get(1).setOccupantId("24010102");
        Room r102 = findRoom("1栋", "102");
        r102.getBeds().get(0).setOccupantId("24011201");
    }

    private void addRooms(String building, int floors, int roomsPerFloor) {
        for (int f = 1; f <= floors; f++) {
            for (int n = 1; n <= roomsPerFloor; n++) {
                rooms.add(new Room(building, "" + (f * 100 + n), f, 4));
            }
        }
    }

    private void seedPersons() {
        // 学号遵循 8 位规则：入学年份后两位 + 学院代码 + 专业代码 + 班级代码 + 学号序号（转专业后学号不变）
        Student s1 = new Student(StudentId.of(24, "01", 0, 1, 1), "张三", "男", "13800000001", "01", "金属材料工程", "240101");
        s1.setCurrentBuilding("1栋");
        s1.setCurrentRoom("101");
        students.add(s1);

        Student s2 = new Student(StudentId.of(24, "01", 0, 1, 2), "李四", "男", "13800000002", "01", "金属材料工程", "240101");
        s2.setCurrentBuilding("1栋");
        s2.setCurrentRoom("101");
        students.add(s2);

        Student s3 = new Student(StudentId.of(24, "01", 1, 2, 1), "王五", "男", "13800000003", "01", "材料成型及控制工程", "240112");
        s3.setCurrentBuilding("1栋");
        s3.setCurrentRoom("102");
        students.add(s3);

        students.add(new Student(StudentId.of(24, "01", 2, 3, 1), "赵六", "男", "13800000004", "01", "焊接技术与工程", "240123"));
        students.add(new Student(StudentId.of(24, "01", 3, 4, 1), "钱七", "男", "13800000005", "01", "高分子材料与工程", "240134"));
        students.add(new Student(StudentId.of(24, "02", 0, 1, 1), "孙八", "男", "13800000010", "02", "环境工程", "240201"));

        counselors.add(new Counselor("G001", "陈静", "女", "13900000001", "01", "辅导员"));

        DormStaff dormStaff = new DormStaff("LD001", "李芳", "女", "13900000002", "00", "楼栋管理员");
        dormStaff.getManageBuildingNames().addAll(Arrays.asList("1栋", "2栋", "3栋"));
        dormStaffs.add(dormStaff);

        admins.add(new Admin("SK001", "刘敏", "女", "13900000003", "00", "宿管科科长"));
    }

    private void seedAccounts() {
        accounts.add(new Account("admin", "admin123", "SK001", RoleKey.ADMIN));
        accounts.add(new Account("counselor", "123456", "G001", RoleKey.COUNSELOR));
        accounts.add(new Account("ld001", "123456", "LD001", RoleKey.DORM_STAFF));
        accounts.add(new Account("24010101", "123456", "24010101", RoleKey.STUDENT));
        accounts.add(new Account("24010102", "123456", "24010102", RoleKey.STUDENT));
        accounts.add(new Account("24011201", "123456", "24011201", RoleKey.STUDENT));
        accounts.add(new Account("24012301", "123456", "24012301", RoleKey.STUDENT));
        accounts.add(new Account("24013401", "123456", "24013401", RoleKey.STUDENT));
        accounts.add(new Account("24020101", "123456", "24020101", RoleKey.STUDENT));
    }

    private void seedApplications() {
        dormApplications.add(new DormApplication("AP0001", "24012301", DormApplication.TYPE_APPLY,
                "1栋", "入学报到，需要安排宿舍", DormApplication.STATUS_PENDING, "2026-09-01 10:00:00"));
        dormApplications.add(new DormApplication("AP0002", "24013401", DormApplication.TYPE_APPLY,
                "1栋", "原宿舍调整，申请入住", DormApplication.STATUS_PENDING, "2026-09-01 11:00:00"));
    }
}

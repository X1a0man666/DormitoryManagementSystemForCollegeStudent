package com.nchu.dorm.model;

import java.util.Arrays;
import java.util.List;

/**
 * 学号（8 位数字）规则值对象。
 * <p>构成规则：第 1-2 位=入学年份后两位；第 3-4 位=学院代码（官方 01-16、20）；
 * 第 5 位=专业代码（0-9，默认每学院 10 个专业）；第 6 位=班级代码（1-9，默认每专业 9 个班）；
 * 第 7-8 位=学号序号（01-99，默认每班 99 人）。</p>
 * <p>学号一经生成永不改变（转专业后学号不变），因此学号编码的是<b>入学时</b>的
 * 学院/专业/班级，学生当前学院/专业可与学号不一致。</p>
 */
public final class StudentId {

    public static final int LENGTH = 8;

    /** 官方学院代码（南昌航空大学学院名单）：01-16、20；00、17-19 未分配。与 seedColleges()/colleges.txt 保持一致 */
    private static final List<String> VALID_COLLEGE_CODES = Arrays.asList(
            "01", "02", "03", "04", "05", "06", "07", "08", "09",
            "10", "11", "12", "13", "14", "15", "16", "20");

    /** 入学年份后两位，如 24 */
    private final int admissionYear;

    /** 学院代码，两位，如 "11" */
    private final String collegeCode;

    /** 专业代码 0-9 */
    private final int majorCode;

    /** 班级代码 1-9 */
    private final int classCode;

    /** 学号序号 1-99 */
    private final int studentNo;

    private StudentId(int admissionYear, String collegeCode, int majorCode, int classCode, int studentNo) {
        this.admissionYear = admissionYear;
        this.collegeCode = collegeCode;
        this.majorCode = majorCode;
        this.classCode = classCode;
        this.studentNo = studentNo;
    }

    /** 是否为合法 8 位学号。 */
    public static boolean isValid(String id) {
        return parseOrNull(id) != null;
    }

    /**
     * 解析学号；不合法返回 null（供界面友好降级）。
     */
    public static StudentId parseOrNull(String id) {
        if (id == null || id.length() != LENGTH) {
            return null;
        }
        for (int i = 0; i < id.length(); i++) {
            if (!Character.isDigit(id.charAt(i))) {
                return null;
            }
        }
        String collegeCode = id.substring(2, 4);
        if (!VALID_COLLEGE_CODES.contains(collegeCode)) {
            return null; // 学院代码 00、17-19 未分配
        }
        int majorCode = id.charAt(4) - '0';   // 0-9，任意值合法（默认每学院 10 个专业）
        int classCode = id.charAt(5) - '0';   // 1-9
        if (classCode < 1 || classCode > 9) {
            return null;
        }
        int studentNo = Integer.parseInt(id.substring(6, 8)); // 01-99
        if (studentNo < 1 || studentNo > 99) {
            return null;
        }
        int admissionYear = Integer.parseInt(id.substring(0, 2)); // 00-99，规则未作限制
        return new StudentId(admissionYear, collegeCode, majorCode, classCode, studentNo);
    }

    /**
     * 解析学号；不合法抛 IllegalArgumentException（供必须合法的场景）。
     */
    public static StudentId parse(String id) {
        StudentId sid = parseOrNull(id);
        if (sid == null) {
            throw new IllegalArgumentException("学号格式不合法：" + id);
        }
        return sid;
    }

    /**
     * 静态工厂：由组成部分构建 8 位学号字符串。
     * 如 {@code of(24, "11", 3, 2, 22)} → "24113222"。
     */
    public static String of(int admissionYear, String collegeCode, int majorCode, int classCode, int studentNo) {
        return String.format("%02d", admissionYear)
                + collegeCode
                + majorCode
                + classCode
                + String.format("%02d", studentNo);
    }

    /** 入学年份后两位，如 24 */
    public int getAdmissionYear() {
        return admissionYear;
    }

    /** 完整入学年份（演示取 21 世纪），如 2024 */
    public int getFullAdmissionYear() {
        return 2000 + admissionYear;
    }

    /** 学院代码，两位，如 "11" */
    public String getCollegeCode() {
        return collegeCode;
    }

    /** 专业代码 0-9 */
    public int getMajorCode() {
        return majorCode;
    }

    /** 班级代码 1-9 */
    public int getClassCode() {
        return classCode;
    }

    /** 学号序号 1-99 */
    public int getStudentNo() {
        return studentNo;
    }

    /** 8 位学号字符串。 */
    public String value() {
        return of(admissionYear, collegeCode, majorCode, classCode, studentNo);
    }

    @Override
    public String toString() {
        return value();
    }
}

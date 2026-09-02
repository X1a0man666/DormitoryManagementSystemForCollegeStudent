package com.nchu.dorm.model;

import com.nchu.dorm.util.TextUtil;

/**
 * 学生（继承 {@link Person}）。
 * 学生可进行宿舍申请、转移、退出、购电、维修申请。
 */
public class Student extends Person {

    /** 专业 */
    private String major;

    /** 班级 */
    private String className;

    /** 当前入住楼栋，未入住为空 */
    private String currentBuilding;

    /** 当前入住房间号，未入住为空 */
    private String currentRoom;

    public Student() {
    }

    public Student(String id, String name, String gender, String phone, String collegeCode,
                   String major, String className) {
        super(id, name, gender, phone, collegeCode);
        this.major = major;
        this.className = className;
    }

    @Override
    public String getRoleName() {
        return "学生";
    }

    @Override
    public String getRoleKey() {
        return RoleKey.STUDENT;
    }

    @Override
    public String getDutyDescription() {
        return "进行宿舍申请、转移、退出，以及购电、维修申请等日常业务。";
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getCurrentBuilding() {
        return currentBuilding;
    }

    public void setCurrentBuilding(String currentBuilding) {
        this.currentBuilding = currentBuilding;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    /** 是否已入住 */
    public boolean isCheckedIn() {
        return currentBuilding != null && !currentBuilding.isEmpty()
                && currentRoom != null && !currentRoom.isEmpty();
    }

    /** 文本序列化 */
    public String toLine() {
        return baseToLine() + "|"
                + TextUtil.escape(major) + "|"
                + TextUtil.escape(className) + "|"
                + TextUtil.escape(currentBuilding) + "|"
                + TextUtil.escape(currentRoom);
    }

    public static Student fromLine(String line) {
        String[] f = TextUtil.split(line);
        Student s = new Student();
        s.baseFromLine(f);
        s.major = f[5];
        s.className = f[6];
        s.currentBuilding = f[7];
        s.currentRoom = f[8];
        return s;
    }
}

package com.nchu.dorm.model;

import com.nchu.dorm.util.TextUtil;

/**
 * 辅导员（继承 {@link Staff}）。
 * 负责本学院宿舍的分配，审批本学院学生的宿舍申请。
 * 所属学院即为 {@link Person#getCollegeCode()}。
 */
public class Counselor extends Staff {

    public Counselor() {
    }

    public Counselor(String id, String name, String gender, String phone,
                     String collegeCode, String jobTitle) {
        super(id, name, gender, phone, collegeCode, jobTitle);
    }

    @Override
    public String getRoleName() {
        return "辅导员";
    }

    @Override
    public String getRoleKey() {
        return RoleKey.COUNSELOR;
    }

    @Override
    public String getDutyDescription() {
        return "负责本学院学生的宿舍分配与申请审批。";
    }

    public String toLine() {
        return staffToLine();
    }

    public static Counselor fromLine(String line) {
        String[] f = TextUtil.split(line);
        Counselor c = new Counselor();
        c.staffFromLine(f);
        return c;
    }
}

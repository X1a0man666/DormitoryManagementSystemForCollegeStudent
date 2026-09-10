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

    /**
     * 专业代码（1-4）：工号即「专业年级代码」= 入学年后两位 + 学院代码 + 专业代码（如 25201 → 25级·20学院·1专业），
     * 第 5 位即专业代码。工号非 5 位标准格式（如手工新增）时返回 0，调用方按「专业未知」降级。
     */
    public int getMajorCode() {
        String id = getId();
        if (id == null || id.length() != 5 || !Character.isDigit(id.charAt(4))) {
            return 0;
        }
        return id.charAt(4) - '0';
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

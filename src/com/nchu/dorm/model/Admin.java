package com.nchu.dorm.model;

import com.nchu.dorm.util.TextUtil;

/**
 * 宿管科（继承 {@link Staff}）。
 * 负责楼栋分配、宿舍管理人员管理、修理管理、售电。
 */
public class Admin extends Staff {

    public Admin() {
    }

    public Admin(String id, String name, String gender, String phone,
                 String collegeCode, String jobTitle) {
        super(id, name, gender, phone, collegeCode, jobTitle);
    }

    @Override
    public String getRoleName() {
        return "宿管科";
    }

    @Override
    public String getRoleKey() {
        return RoleKey.ADMIN;
    }

    @Override
    public String getDutyDescription() {
        return "负责楼栋分配、宿舍管理人员管理、修理工单处理与售电管理。";
    }

    public String toLine() {
        return staffToLine();
    }

    public static Admin fromLine(String line) {
        String[] f = TextUtil.split(line);
        Admin a = new Admin();
        a.staffFromLine(f);
        return a;
    }
}

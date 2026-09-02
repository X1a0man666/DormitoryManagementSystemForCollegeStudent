package com.nchu.dorm.model;

import com.nchu.dorm.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 宿舍管理人员（继承 {@link Staff}）。
 * 负责日常管理：夜归管理、卫生管理、贵重物品出入登记。
 * 可分管若干栋楼，见 {@link #manageBuildingNames}。
 */
public class DormStaff extends Staff {

    /** 负责管理的楼栋名称列表 */
    private final List<String> manageBuildingNames = new ArrayList<>();

    public DormStaff() {
    }

    public DormStaff(String id, String name, String gender, String phone,
                     String collegeCode, String jobTitle) {
        super(id, name, gender, phone, collegeCode, jobTitle);
    }

    @Override
    public String getRoleName() {
        return "宿舍管理人员";
    }

    @Override
    public String getRoleKey() {
        return RoleKey.DORM_STAFF;
    }

    @Override
    public String getDutyDescription() {
        return "负责楼栋日常管理：夜归登记、卫生检查、贵重物品出入登记。";
    }

    public List<String> getManageBuildingNames() {
        return manageBuildingNames;
    }

    /** 是否分管指定楼栋 */
    public boolean manages(String buildingName) {
        return manageBuildingNames.contains(buildingName);
    }

    public String toLine() {
        StringBuilder sb = new StringBuilder(staffToLine());
        sb.append("|");
        for (int i = 0; i < manageBuildingNames.size(); i++) {
            if (i > 0) {
                sb.append(";");
            }
            sb.append(TextUtil.escape(manageBuildingNames.get(i)));
        }
        return sb.toString();
    }

    public static DormStaff fromLine(String line) {
        String[] f = TextUtil.split(line);
        DormStaff d = new DormStaff();
        d.staffFromLine(f);
        String buildings = f[6];
        if (buildings != null && !buildings.isEmpty()) {
            for (String b : buildings.split(";")) {
                if (!b.isEmpty()) {
                    d.manageBuildingNames.add(b);
                }
            }
        }
        return d;
    }
}

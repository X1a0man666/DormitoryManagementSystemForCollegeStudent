package com.nchu.dorm.service;

import com.nchu.dorm.model.Account;
import com.nchu.dorm.model.Building;
import com.nchu.dorm.model.College;
import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.model.RoleKey;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.util.BusinessException;

import java.util.ArrayList;
import java.util.List;

/**
 * 宿管科管理服务：楼栋分配、宿舍管理人员管理（增删改 + 登录账号）、学生账号密码重置。
 * <ul>
 *   <li>楼栋分配：把（空置）楼栋分配给学院 / 解除分配。已入住学生的楼栋不允许改归属，
 *       避免学生档案与所在楼栋归属矛盾；</li>
 *   <li>宿舍管理人员：新增/编辑/删除，新员工同时生成登录账号（用户名=工号小写，默认密码 123456）；</li>
 *   <li>学生账号：学生忘记密码时，宿管科按学号/姓名定位账号并重置为默认密码 123456
 *       （学生自改密码见 {@link AccountService#changePassword}）。</li>
 * </ul>
 */
public class AdminService {

    /** 默认初始密码（与种子一致）。 */
    public static final String DEFAULT_PASSWORD = "123456";

    private DataCenter dc() {
        return DataCenter.instance();
    }

    // ==================== 楼栋分配 ====================

    /**
     * 分配楼栋给学院；collegeCode 传空串表示"解除分配"。
     * 已有学生入住的楼栋不得调整归属。
     */
    public void assignBuilding(String buildingName, String collegeCode) {
        Building building = dc().findBuilding(buildingName);
        if (building == null) {
            throw new BusinessException("楼栋不存在：" + buildingName);
        }
        collegeCode = collegeCode == null ? "" : collegeCode.trim();

        if (collegeCode.isEmpty()) {
            unassign(building);
            return;
        }
        if (dc().occupiedCountOfBuilding(building.getName()) > 0) {
            throw new BusinessException("该楼栋仍有 " + dc().occupiedCountOfBuilding(building.getName())
                    + " 名学生入住，无法调整归属，请先腾空楼栋");
        }
        College target = dc().findCollegeByCode(collegeCode);
        if (target == null) {
            throw new BusinessException("目标学院不存在（代码 " + collegeCode + "）");
        }
        if (collegeCode.equals(building.getCollegeCode())) {
            return; // 已在目标学院，无需变更
        }
        detachFromCollege(building);
        building.setCollegeCode(collegeCode);
        if (!target.getBuildingNames().contains(building.getName())) {
            target.getBuildingNames().add(building.getName());
        }
        dc().saveAll();
    }

    private void unassign(Building building) {
        if (dc().occupiedCountOfBuilding(building.getName()) > 0) {
            throw new BusinessException("该楼栋仍有学生入住，无法解除分配");
        }
        detachFromCollege(building);
        building.setCollegeCode(null);
        dc().saveAll();
    }

    /** 从原归属学院的楼栋列表中摘除。 */
    private void detachFromCollege(Building building) {
        String oldCode = building.getCollegeCode();
        if (oldCode == null || oldCode.isEmpty()) {
            return;
        }
        College old = dc().findCollegeByCode(oldCode);
        if (old != null) {
            old.getBuildingNames().remove(building.getName());
        }
    }

    // ==================== 宿舍管理人员管理 ====================

    /**
     * 新增宿舍管理人员，并生成登录账号。
     *
     * @param id           工号；传 null/空 时自动生成下一个 LD 号
     * @param buildings    负责楼栋（可空）
     */
    public DormStaff addDormStaff(String id, String name, String gender,
                                  String phone, String jobTitle, List<String> buildings) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("请填写姓名");
        }
        if (gender == null || (!"男".equals(gender) && !"女".equals(gender))) {
            throw new BusinessException("请选择性别");
        }
        String newId = (id == null || id.trim().isEmpty()) ? dc().nextDormStaffId() : id.trim();
        if (dc().findDormStaffById(newId) != null) {
            throw new BusinessException("工号 " + newId + " 已存在");
        }
        DormStaff staff = new DormStaff(newId, name.trim(), gender,
                phone == null ? "" : phone.trim(),
                "00", jobTitle == null || jobTitle.isEmpty() ? "楼栋管理员" : jobTitle.trim());
        if (buildings != null) {
            for (String b : buildings) {
                if (b != null && !b.trim().isEmpty() && !staff.getManageBuildingNames().contains(b.trim())) {
                    staff.getManageBuildingNames().add(b.trim());
                }
            }
        }
        // 登录账号：用户名 = 工号小写
        String username = newId.toLowerCase();
        if (dc().findAccount(username) != null) {
            throw new BusinessException("登录账号 " + username + " 已存在");
        }
        dc().getDormStaffs().add(staff);
        dc().getAccounts().add(new Account(username, DEFAULT_PASSWORD, newId, RoleKey.DORM_STAFF));
        dc().saveAll();
        return staff;
    }

    /** 编辑宿舍管理人员基本信息与负责楼栋。 */
    public void updateDormStaff(DormStaff staff, String name, String gender,
                                String phone, String jobTitle, List<String> buildings) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("请填写姓名");
        }
        if (gender == null || (!"男".equals(gender) && !"女".equals(gender))) {
            throw new BusinessException("请选择性别");
        }
        staff.setName(name.trim());
        staff.setGender(gender);
        staff.setPhone(phone == null ? "" : phone.trim());
        staff.setJobTitle(jobTitle == null || jobTitle.isEmpty() ? "楼栋管理员" : jobTitle.trim());
        staff.getManageBuildingNames().clear();
        if (buildings != null) {
            for (String b : buildings) {
                if (b != null && !b.trim().isEmpty() && !staff.getManageBuildingNames().contains(b.trim())) {
                    staff.getManageBuildingNames().add(b.trim());
                }
            }
        }
        dc().saveAll();
    }

    /**
     * 删除宿舍管理人员（须已无分管楼栋）并同步删除其登录账号。
     */
    public void removeDormStaff(DormStaff staff) {
        if (staff == null) {
            throw new BusinessException("请先选择一名宿舍管理人员");
        }
        if (!staff.getManageBuildingNames().isEmpty()) {
            throw new BusinessException("该员工仍负责 " + staff.getManageBuildingNames().size()
                    + " 栋楼，请先在编辑中解除其负责楼栋再删除");
        }
        dc().getDormStaffs().remove(staff);
        for (int i = dc().getAccounts().size() - 1; i >= 0; i--) {
            Account a = dc().getAccounts().get(i);
            if (staff.getId().equals(a.getPersonId())) {
                dc().getAccounts().remove(i);
            }
        }
        dc().saveAll();
    }

    // ==================== 学生账号（密码重置） ====================

    /**
     * 按学号或姓名定位学生：学号按前缀匹配（可只输前几位），姓名按包含匹配，两者取并集。
     * <p>全校 4 万余名学生，关键词过宽时结果会很多，故由调用方传入 {@code limit} 限定返回条数。</p>
     *
     * @param limit 最多返回条数（{@code <=0} 表示不限）
     */
    public List<Student> searchStudents(String keyword, int limit) {
        List<Student> result = new ArrayList<>();
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return result;
        }
        for (Student s : dc().getStudents()) {
            if (s.getId().startsWith(kw) || (s.getName() != null && s.getName().contains(kw))) {
                result.add(s);
                if (limit > 0 && result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 重置学生登录密码为默认密码（学生忘记密码时由宿管科代为重置）。
     *
     * @throws BusinessException 未选择学生 / 该学生没有绑定登录账号
     */
    public void resetStudentPassword(Student student) {
        if (student == null) {
            throw new BusinessException("请先选择一名学生");
        }
        Account account = dc().findAccountByPersonId(student.getId());
        if (account == null) {
            throw new BusinessException("该学生（" + student.getId() + "）没有绑定的登录账号，无法重置密码");
        }
        account.setPassword(DEFAULT_PASSWORD);
        dc().saveAll();
    }
}

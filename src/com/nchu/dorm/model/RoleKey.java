package com.nchu.dorm.model;

/**
 * 角色常量接口。
 * 集中管理四种系统角色的唯一标识，避免散落的魔法字符串。
 */
public interface RoleKey {

    /** 学生 */
    String STUDENT = "STUDENT";

    /** 辅导员 */
    String COUNSELOR = "COUNSELOR";

    /** 宿舍管理人员（楼栋管理员） */
    String DORM_STAFF = "DORM_STAFF";

    /** 宿管科 */
    String ADMIN = "ADMIN";
}

package com.nchu.dorm.model;

/**
 * 角色能力接口（体现接口与多态）。
 * 凡是"系统角色"的对象都必须能回答自己是谁、拥有哪个角色键，
 * 上层（登录、界面、权限分发）只需面向该接口编程，无需关心具体类。
 */
public interface RoleCapable {

    /** 返回角色唯一键，用于界面/权限分发，见 {@link RoleKey} */
    String getRoleKey();

    /** 返回角色中文名，用于界面展示 */
    String getRoleName();
}

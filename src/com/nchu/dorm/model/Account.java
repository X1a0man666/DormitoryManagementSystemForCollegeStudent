package com.nchu.dorm.model;

import com.nchu.dorm.util.TextUtil;

/**
 * 登录账号。绑定一个系统人员（{@link Person}）及其角色。
 */
public class Account {

    /** 登录用户名 */
    private String username;

    /** 登录密码（课程设计演示，明文存储，见设计文档说明） */
    private String password;

    /** 绑定的人员唯一标识（学号/工号） */
    private String personId;

    /** 角色键，见 {@link RoleKey} */
    private String roleKey;

    public Account() {
    }

    public Account(String username, String password, String personId, String roleKey) {
        this.username = username;
        this.password = password;
        this.personId = personId;
        this.roleKey = roleKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public String toLine() {
        return TextUtil.escape(username) + "|"
                + TextUtil.escape(password) + "|"
                + TextUtil.escape(personId) + "|"
                + TextUtil.escape(roleKey);
    }

    public static Account fromLine(String line) {
        String[] f = TextUtil.split(line);
        Account a = new Account();
        a.username = f[0];
        a.password = f[1];
        a.personId = f[2];
        a.roleKey = f[3];
        return a;
    }
}

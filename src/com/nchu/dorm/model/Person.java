package com.nchu.dorm.model;

import com.nchu.dorm.util.TextUtil;

/**
 * 人员抽象基类（体现继承与多态）。
 * <p>
 * 所有系统用户（学生、辅导员、宿舍管理人员、宿管科）共有的属性都收敛到本类，
 * 子类只需补充各自特有属性，并实现抽象的"角色描述"方法。
 */
public abstract class Person implements RoleCapable {

    /** 唯一标识：学生为学号，教职工为工号 */
    private String id;

    /** 姓名 */
    private String name;

    /** 性别 */
    private String gender;

    /** 联系电话 */
    private String phone;

    /** 所属学院代码，见 {@link College} */
    private String collegeCode;

    protected Person() {
    }

    protected Person(String id, String name, String gender, String phone, String collegeCode) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.phone = phone;
        this.collegeCode = collegeCode;
    }

    /**
     * 抽象方法：返回角色名称，由各子类实现——这正是多态的体现。
     */
    @Override
    public abstract String getRoleName();

    /**
     * 抽象方法：返回角色键，由各子类实现。
     */
    @Override
    public abstract String getRoleKey();

    /**
     * 抽象方法：返回该角色在本系统中的职责说明，用于界面展示与文档。
     */
    public abstract String getDutyDescription();

    // ---------- getter / setter（封装：字段私有，仅通过方法访问） ----------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCollegeCode() {
        return collegeCode;
    }

    public void setCollegeCode(String collegeCode) {
        this.collegeCode = collegeCode;
    }

    @Override
    public String toString() {
        return name + "（" + getRoleName() + "）";
    }

    /**
     * 序列化为文本行（供子类复用公共字段）。
     */
    protected String baseToLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(name) + "|"
                + TextUtil.escape(gender) + "|"
                + TextUtil.escape(phone) + "|"
                + TextUtil.escape(collegeCode);
    }

    /**
     * 从基础字段恢复公共属性。
     */
    protected void baseFromLine(String[] f) {
        this.id = f[0];
        this.name = f[1];
        this.gender = f[2];
        this.phone = f[3];
        this.collegeCode = f[4];
    }
}

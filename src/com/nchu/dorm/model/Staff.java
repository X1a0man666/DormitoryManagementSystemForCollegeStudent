package com.nchu.dorm.model;

import com.nchu.dorm.util.TextUtil;

/**
 * 教职工抽象基类（继承链的中间层）。
 * 辅导员、宿舍管理人员、宿管科都是教职工，共享"工号 + 职务"两个属性。
 */
public abstract class Staff extends Person {

    /** 职务 */
    private String jobTitle;

    protected Staff() {
    }

    protected Staff(String id, String name, String gender, String phone,
                    String collegeCode, String jobTitle) {
        super(id, name, gender, phone, collegeCode);
        this.jobTitle = jobTitle;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    /**
     * 教职工在基类公共字段后追加"职务"字段。
     */
    protected String staffToLine() {
        return baseToLine() + "|" + TextUtil.escape(jobTitle);
    }

    protected void staffFromLine(String[] f) {
        baseFromLine(f);
        this.jobTitle = f[5];
    }
}

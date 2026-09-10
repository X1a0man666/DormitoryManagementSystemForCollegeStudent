package com.nchu.dorm.service;

import com.nchu.dorm.model.Account;
import com.nchu.dorm.model.Person;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.util.BusinessException;

/**
 * 账号自助服务：登录用户修改本人姓名与登录密码。
 * <p>
 * 校验规则统一收口在本类，界面层只负责收集输入：
 * 改密码必须先验证当前密码，且两次输入的新密码一致；姓名不能为空（学号/工号为唯一标识，不可更改）。
 */
public class AccountService {

    /**
     * 修改某人绑定账号的登录密码。
     *
     * @param person          当前登录人员
     * @param currentPassword 当前密码（用于验证身份）
     * @param newPassword     新密码
     * @param confirmPassword 再次输入的新密码
     * @throws BusinessException 未绑定账号 / 当前密码错误 / 新密码为空 / 两次输入不一致
     */
    public void changePassword(Person person, String currentPassword, String newPassword, String confirmPassword) {
        Account account = findAccountOf(person);
        if (account == null) {
            throw new BusinessException("未找到该用户的登录账号");
        }
        // 密码不做 trim，与登录时的比对口径保持一致
        if (!account.getPassword().equals(currentPassword)) {
            throw new BusinessException("当前密码不正确，请重新输入");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new BusinessException("请输入您的新密码");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("两次输入的新密码不一致，修改失败");
        }
        account.setPassword(newPassword);
        DataCenter.instance().saveAll();
    }

    /**
     * 修改人员姓名（学生姓名默认等于学号，可自行修改）。
     *
     * @throws BusinessException 姓名为空
     */
    public void changeName(Person person, String newName) {
        String name = newName == null ? "" : newName.trim();
        if (name.isEmpty()) {
            throw new BusinessException("请输入新的姓名");
        }
        person.setName(name);
        DataCenter.instance().saveAll();
    }

    /** 按人员标识找其绑定账号（学生为学号，教职工为工号）。 */
    private Account findAccountOf(Person person) {
        return person == null ? null : DataCenter.instance().findAccountByPersonId(person.getId());
    }
}

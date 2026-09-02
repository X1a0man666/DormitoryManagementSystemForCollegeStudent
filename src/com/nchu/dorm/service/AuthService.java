package com.nchu.dorm.service;

import com.nchu.dorm.model.Account;
import com.nchu.dorm.model.Person;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.util.BusinessException;

/**
 * 登录认证服务。
 * <p>
 * 登录成功后返回 {@link Person}（基类引用指向具体子类对象），
 * 调用方借助多态获得具体的角色行为——这里即体现了"面向基类编程"。
 */
public class AuthService {

    /**
     * 校验账号密码，返回登录人员。
     *
     * @throws BusinessException 账号不存在 / 密码错误 / 未绑定人员
     */
    public Person login(String username, String password) {
        DataCenter dc = DataCenter.instance();
        Account account = dc.findAccount(username);
        if (account == null) {
            throw new BusinessException("账号不存在");
        }
        if (!account.getPassword().equals(password)) {
            throw new BusinessException("密码错误");
        }
        Person person = dc.findPersonById(account.getPersonId());
        if (person == null) {
            throw new BusinessException("该账号未绑定有效人员信息");
        }
        return person;
    }
}

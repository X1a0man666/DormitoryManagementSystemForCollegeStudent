package com.nchu.dorm.util;

/**
 * 业务异常：用于在业务逻辑中抛出可被界面捕获并友好提示的错误。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}

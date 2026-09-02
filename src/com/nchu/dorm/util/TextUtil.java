package com.nchu.dorm.util;

/**
 * 文本工具类：负责文本文件中字段的安全转义。
 * 约定：字段分隔符使用英文竖线 |，列表分隔符使用分号 ;，键值对使用冒号 :。
 * 用户输入的自由文本（原因、备注等）可能包含这些字符，写入前统一转义为全角字符。
 */
public final class TextUtil {

    private TextUtil() {
    }

    /**
     * 字段转义：把英文竖线、分号、换行替换为全角字符，防止破坏文件结构。
     */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("|", "｜")
                .replace(";", "；")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    /**
     * 安全地取分段字段：null 一律返回空字符串。
     */
    public static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * 字段拆分：按约定的 | 拆分。
     */
    public static String[] split(String line) {
        return line.split("\\|", -1);
    }
}

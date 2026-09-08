package com.nchu.dorm.util;

/**
 * 展示格式化工具：统一金额与数值的界面显示。
 */
public final class FormatUtil {

    private FormatUtil() {
    }

    /** 金额：保留两位小数（如 12.60）。 */
    public static String money(double value) {
        return String.format("%.2f", value);
    }

    /** 数值展示：整数值去掉多余的 .0（如 200 而非 200.0），非整数保留原样。 */
    public static String num(double value) {
        if (!Double.isInfinite(value) && value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}

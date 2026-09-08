package com.nchu.dorm.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间工具类：集中提供本系统常用的时间字符串格式。
 * <ul>
 *   <li>{@link #now()}     完整时间 yyyy-MM-dd HH:mm:ss（业务记录的默认值）；</li>
 *   <li>{@link #today()}   日期 yyyy-MM-dd（夜归/卫生检查默认日期）；</li>
 *   <li>{@link #time()}    时刻 HH:mm:ss（夜归默认时间）。</li>
 * </ul>
 */
public final class TimeUtil {

    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat CLOCK = new SimpleDateFormat("HH:mm:ss");

    private TimeUtil() {
    }

    /** 当前完整时间，如 2026-09-09 10:00:00 */
    public static String now() {
        return DATE_TIME.format(new Date());
    }

    /** 当前日期，如 2026-09-09 */
    public static String today() {
        return DATE.format(new Date());
    }

    /** 当前时刻，如 10:00:00 */
    public static String time() {
        return CLOCK.format(new Date());
    }
}

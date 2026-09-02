package com.nchu.dorm.storage;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * 数据存储接口（体现接口与多态）。
 * <p>
 * 上层业务只面向本接口编程，不关心底层是文本文件、二进制序列化还是数据库。
 * 当前实现为 {@link TextStorage}（文本文件），后续可新增二进制等实现而不影响业务层。
 */
public interface Storage {

    /**
     * 判断某数据文件是否存在。
     */
    boolean hasFile(String fileKey);

    /**
     * 从文件中读取全部记录。
     *
     * @param fileKey 数据文件标识（不含目录）
     * @param parser  文本行 -> 对象的解析函数（各实体类的静态工厂方法）
     */
    <T> List<T> load(String fileKey, Function<String, T> parser) throws IOException;

    /**
     * 将记录列表写入文件。
     *
     * @param fileKey    数据文件标识（不含目录）
     * @param header     文件头注释（用于说明字段格式，帮助人工查阅与报告展示）
     * @param items      记录列表
     * @param serializer 对象 -> 文本行的序列化函数
     */
    <T> void save(String fileKey, String header, List<T> items, Function<T, String> serializer) throws IOException;
}

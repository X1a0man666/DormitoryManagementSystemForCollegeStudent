package com.nchu.dorm.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 文本文件存储实现（{@link Storage} 的具体实现之一）。
 * <p>
 * 约定：
 * <ul>
 *   <li>每个实体一个文本文件，一行一条记录，字段以 | 分隔；</li>
 *   <li>以 # 开头的行是注释/文件头说明，加载时跳过；</li>
 *   <li>统一 UTF-8 编码，避免平台差异。</li>
 * </ul>
 */
public class TextStorage implements Storage {

    private final Path dir;

    public TextStorage(Path dir) {
        this.dir = dir;
    }

    @Override
    public boolean hasFile(String fileKey) {
        return Files.exists(dir.resolve(fileKey));
    }

    @Override
    public <T> List<T> load(String fileKey, Function<String, T> parser) throws IOException {
        List<T> result = new ArrayList<>();
        Path file = dir.resolve(fileKey);
        if (!Files.exists(file)) {
            return result;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            result.add(parser.apply(trimmed));
        }
        return result;
    }

    @Override
    public <T> void save(String fileKey, String header, List<T> items, Function<T, String> serializer)
            throws IOException {
        Files.createDirectories(dir);
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(header).append(System.lineSeparator());
        sb.append("# 字段以 | 分隔，一行一条记录；# 开头为注释行。").append(System.lineSeparator());
        for (T item : items) {
            sb.append(serializer.apply(item)).append(System.lineSeparator());
        }
        Files.write(dir.resolve(fileKey), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}

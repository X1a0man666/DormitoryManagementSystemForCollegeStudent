package com.nchu.dorm.ui.component;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期选择框：统一本系统的日期录入方式——显示与取值一律为 {@code yyyy-MM-dd}，
 * 既可直接输入，也可点右侧按钮展开日历可视化选择。
 * <p>取值请走 {@link #textOf(DatePicker)}：未选择时返回空串，
 * 由各 Service 既有的「请填写…日期」校验给出提示，而不是在后面抛 NullPointerException。
 */
public final class DatePickers {

    /** 展示与解析共用的格式，与业务记录里日期列的写法保持一致。 */
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DatePickers() {
    }

    /**
     * 创建日期选择框。
     *
     * @param initial 预填日期（yyyy-MM-dd）；为空或格式非法时留空，不抛异常
     */
    public static DatePicker create(String initial) {
        DatePicker picker = new DatePicker(parse(initial));
        picker.setConverter(converter());
        picker.setPromptText("yyyy-MM-dd");
        picker.setPrefWidth(150);
        // 可编辑框里手输的日期要按回车或失焦才写回 value；若用户改完直接点「登记」按钮，
        // 中间隔着一次焦点转移，这里补一道保险，保证取到的是眼前框里的日期。
        picker.getEditor().focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                picker.setValue(parse(picker.getEditor().getText()));
            }
        });
        return picker;
    }

    /** 读取日期选择框的值，格式 yyyy-MM-dd；未选择或输入非法时返回空串。 */
    public static String textOf(DatePicker picker) {
        LocalDate date = picker == null ? null : picker.getValue();
        return date == null ? "" : FORMAT.format(date);
    }

    /** yyyy-MM-dd 与 LocalDate 互转；输入为空或非法时给 null（等价于「未选择」）。 */
    private static StringConverter<LocalDate> converter() {
        return new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : FORMAT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                return parse(text);
            }
        };
    }

    private static LocalDate parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

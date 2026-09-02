package com.nchu.dorm.ui.component;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * 界面提示工具：统一封装信息/警告/错误弹窗与二次确认。
 */
public final class AlertUtil {

    private AlertUtil() {
    }

    public static void info(String message) {
        show(Alert.AlertType.INFORMATION, message);
    }

    public static void warn(String message) {
        show(Alert.AlertType.WARNING, message);
    }

    public static void error(String message) {
        show(Alert.AlertType.ERROR, message);
    }

    /** 二次确认弹窗：确定返回 true。 */
    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static void show(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "错误"
                : type == Alert.AlertType.WARNING ? "警告" : "提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

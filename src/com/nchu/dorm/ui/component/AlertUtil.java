package com.nchu.dorm.ui.component;

import javafx.scene.control.Alert;

/**
 * 界面提示工具：统一封装信息/警告/错误弹窗。
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

    private static void show(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "错误"
                : type == Alert.AlertType.WARNING ? "警告" : "提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

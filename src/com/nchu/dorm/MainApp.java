package com.nchu.dorm;

import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.ui.LoginView;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * 系统入口（JavaFX Application）。
 * 启动时加载数据，随后显示登录界面。
 */
public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            DataCenter.instance().loadAll();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("启动失败");
            alert.setHeaderText("数据加载失败");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            System.exit(1);
            return;
        }
        new LoginView(stage).show();
    }
}

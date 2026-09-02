package com.nchu.dorm.ui;

import com.nchu.dorm.model.Person;
import com.nchu.dorm.service.AuthService;
import com.nchu.dorm.util.BusinessException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * 登录界面。账号密码校验通过后按角色进入对应主界面。
 */
public class LoginView {

    private final Stage stage;
    private final AuthService authService = new AuthService();

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        Label title = new Label("南昌航空大学 · 学生宿舍管理系统");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subTitle = new Label("Nanchang Hangkong University Dormitory System");
        subTitle.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");

        Label userLabel = new Label("账号：");
        TextField usernameField = new TextField();
        usernameField.setPromptText("请输入账号");

        Label passLabel = new Label("密码：");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");

        Button loginButton = new Button("登 录");
        loginButton.setPrefWidth(200);
        loginButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14;");

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #e74c3c;");

        Label hint = new Label("演示账号（密码除 admin 外均为 123456）：\n"
                + "学生 25201101（2025级软件学院）· 辅导员 counselor25201（2025级软件工程）\n"
                + "楼栋管理员 ld001 · 宿管科 admin / admin123");
        hint.setStyle("-fx-font-size: 12; -fx-text-fill: #95a5a6;");
        hint.setAlignment(Pos.CENTER);
        hint.setWrapText(true);
        hint.setMaxWidth(480);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.setAlignment(Pos.CENTER);
        form.add(userLabel, 0, 0);
        form.add(usernameField, 1, 0);
        form.add(passLabel, 0, 1);
        form.add(passwordField, 1, 1);

        VBox root = new VBox(14, title, subTitle, form, loginButton, messageLabel, hint);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        loginButton.setOnAction(e -> doLogin(usernameField.getText(), passwordField.getText(), messageLabel));
        passwordField.setOnAction(e -> doLogin(usernameField.getText(), passwordField.getText(), messageLabel));

        Scene scene = new Scene(root, 520, 400);
        stage.setTitle("南昌航空大学 · 学生宿舍管理系统");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private void doLogin(String username, String password, Label messageLabel) {
        try {
            Person person = authService.login(username.trim(), password);
            new MainFrame(stage, person).show();
        } catch (BusinessException ex) {
            messageLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            messageLabel.setText("登录失败：" + ex.getMessage());
        }
    }
}

package com.nchu.dorm.ui;

import com.nchu.dorm.model.Person;
import com.nchu.dorm.service.AuthService;
import com.nchu.dorm.ui.component.FxEffects;
import com.nchu.dorm.ui.component.UI;
import com.nchu.dorm.util.BusinessException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * 登录界面。居中白卡 + 品牌渐变背景，进场带动效；
 * 账号密码校验通过后按角色进入对应主界面。
 */
public class LoginView {

    private final Stage stage;
    private final AuthService authService = new AuthService();

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        // ---- 登录卡片 ----
        VBox card = new VBox(14);
        card.setMaxSize(430, Region.USE_PREF_SIZE);
        UI.style(card, "login-card");

        Label title = new Label("南昌航空大学");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        UI.style(title, "login-title");

        Label subTitle = new Label("学生宿舍管理系统 · Dormitory Management System");
        subTitle.setAlignment(Pos.CENTER);
        subTitle.setMaxWidth(Double.MAX_VALUE);
        UI.style(subTitle, "login-sub");
        card.getChildren().addAll(title, subTitle);

        // ---- 账号 ----
        Label userLabel = new Label("账号");
        UI.style(userLabel, "login-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("请输入账号");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(usernameField, Priority.NEVER);

        // ---- 密码 ----
        Label passLabel = new Label("密码");
        UI.style(passLabel, "login-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        VBox userBox = new VBox(6, userLabel, usernameField);
        VBox passBox = new VBox(6, passLabel, passwordField);
        card.getChildren().addAll(userBox, passBox);

        // ---- 登录按钮 ----
        Button loginButton = new Button("登  录");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        UI.style(loginButton, "login-btn");

        // ---- 错误提示 ----
        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        UI.style(messageLabel, "login-error");
        messageLabel.setVisible(false);

        // ---- 演示账号提示 ----
        VBox hintBox = new VBox(4);
        UI.style(hintBox, "login-hint");
        Label hint = new Label("演示账号（默认密码 123456）：\n"
                + "学生 25201101 / 26201101 · 辅导员 counselor25201 / 26201 · 楼栋管理员 ld001 · 宿管科 admin / admin123");
        hint.setWrapText(true);
        hint.setMaxWidth(360);
        UI.style(hint, "faint");
        hintBox.getChildren().add(hint);

        card.getChildren().addAll(loginButton, messageLabel, hintBox);

        // ---- 背景层：品牌渐变 + 白卡居中 ----
        StackPane bg = new StackPane(card);
        UI.style(bg, "login-bg");
        StackPane.setMargin(card, new Insets(24));

        Scene scene = new Scene(bg, 560, 540);
        UI.apply(scene);
        stage.setTitle("南昌航空大学 · 学生宿舍管理系统");
        stage.setScene(scene);
        stage.centerOnScreen();

        // 进场动效：卡片淡入上移。
        // 注意：若窗口已处于显示中（从主界面「退出登录」返回本页），
        // setOnShown 不会再触发，卡片会一直停在 opacity=0 的透明态，界面看似卡死。
        // 因此在窗口已显示时需直接播放入场动画。
        card.setOpacity(0);
        card.setTranslateY(24);
        Runnable enterFx = () -> FxEffects.fadeInUp(card, Duration.millis(400), 24, Duration.ZERO);
        if (stage.isShowing()) {
            enterFx.run();
        } else {
            stage.setOnShown(e -> enterFx.run());
        }
        stage.show();

        // ---- 事件 ----
        Runnable tryLogin = () -> doLogin(usernameField.getText(), passwordField.getText(), messageLabel);
        loginButton.setOnAction(e -> tryLogin.run());
        passwordField.setOnAction(e -> tryLogin.run());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        usernameField.requestFocus();
    }

    private void doLogin(String username, String password, Label messageLabel) {
        try {
            Person person = authService.login(username.trim(), password);
            new MainFrame(stage, person).show();
        } catch (BusinessException ex) {
            showError(messageLabel, ex.getMessage());
        } catch (Exception ex) {
            showError(messageLabel, "登录失败：" + ex.getMessage());
        }
    }

    private void showError(Label messageLabel, String text) {
        messageLabel.setText(text);
        messageLabel.setVisible(true);
        FxEffects.shake(messageLabel);
    }
}

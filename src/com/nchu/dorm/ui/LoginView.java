package com.nchu.dorm.ui;

import com.nchu.dorm.model.Person;
import com.nchu.dorm.model.StudentId;
import com.nchu.dorm.service.AuthService;
import com.nchu.dorm.service.AvatarService;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.ui.component.AlertUtil;
import com.nchu.dorm.ui.component.AuroraBackground;
import com.nchu.dorm.ui.component.FxEffects;
import com.nchu.dorm.ui.component.UI;
import com.nchu.dorm.util.BusinessException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * 登录界面。居中白卡 + 品牌渐变背景，进场带动效；
 * 账号密码校验通过后按角色进入对应主界面。
 * <p>卡片顶部显示头像：默认校徽，账号框里填入完整合法学号且该学生更换过头像时，自动换成其头像。</p>
 */
public class LoginView {

    /** 登录卡片宽度（窗口内水平居中）。 */
    private static final double CARD_WIDTH = 430;

    /** 卡片上方的留白。 */
    private static final double GAP_TOP = 82;

    /**
     * 卡片下方的留白，比上方多出约一个标题栏的高度（24px）。
     * <p>窗口按屏幕可视区居中时，标题栏会把窗口内容整体往下压；
     * 下方多留这一截，卡片的视觉重心才落回屏幕可视区的中心。
     */
    private static final double GAP_BOTTOM = 106;

    /** 登录卡片高度（无报错时；报错文案会让它在下方多占一行）。 */
    private static final double CARD_HEIGHT = 486;

    /**
     * 窗口高度 = 上留白 + 卡片 + 下留白。
     * <p>卡片贴顶摆放，多余的高度自然落在下方，故卡片内容增减只会改变下方留白，不会顶出窗口。
     * <p>不按卡片实时测量：建场景时样式表还没挂上，那时量出来的高度是不准的。
     */
    private static final double SCENE_HEIGHT = GAP_TOP + CARD_HEIGHT + GAP_BOTTOM;

    private final Stage stage;
    private final AuthService authService = new AuthService();
    private final AvatarService avatarService = new AvatarService();

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        // ---- 登录卡片 ----
        VBox card = new VBox(14);
        card.setMaxSize(CARD_WIDTH, Region.USE_PREF_SIZE);
        UI.style(card, "login-card");

        // ---- 头像：默认校徽，账号框填完整学号后按该生是否换过头像切换 ----
        final ImageView avatarView = UI.circularAvatar(88);
        avatarView.setImage(UI.appIcon());
        // 头像本体轻微上下浮动，光环反向各转一圈，静止画面里也有生气
        FxEffects.floatIdle(avatarView);
        StackPane avatarBox = new StackPane(ring(46.5, 2.5, Duration.seconds(9), true, 150, 42),
                ring(41, 1.4, Duration.seconds(14), false, 58, 34),
                avatarView);
        avatarBox.setAlignment(Pos.CENTER);

        Label title = new Label("南昌航空大学");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        UI.style(title, "login-title");

        Label subTitle = new Label("学生宿舍管理系统 · Dormitory Management System");
        subTitle.setAlignment(Pos.CENTER);
        subTitle.setMaxWidth(Double.MAX_VALUE);
        UI.style(subTitle, "login-sub");

        // 标题与表单之间的渐变分隔线：比一根灰线更像「设计过」
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        UI.style(divider, "login-divider");
        VBox.setMargin(divider, new Insets(4, 30, 8, 30));

        card.getChildren().addAll(avatarBox, title, subTitle, divider);

        // ---- 账号 ----
        Label userLabel = new Label("账号");
        UI.style(userLabel, "login-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("请输入账号");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(usernameField, Priority.NEVER);
        // 边输边判断：凑成完整合法学号就去看该账号有没有换过头像
        usernameField.textProperty().addListener(
                (obs, oldVal, newVal) -> refreshAvatar(avatarView, newVal));

        // ---- 密码 ----
        Label passLabel = new Label("密码");
        UI.style(passLabel, "login-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        VBox userBox = new VBox(6, userLabel, usernameField);
        VBox passBox = new VBox(6, passLabel, passwordField);
        card.getChildren().addAll(userBox, passBox);

        // ---- 登录 / 退出系统 ----
        Button loginButton = new Button("登  录");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        UI.style(loginButton, "login-btn");
        FxEffects.pressFx(loginButton);

        Button exitButton = new Button("退出系统");
        exitButton.setMaxWidth(Double.MAX_VALUE);
        UI.style(exitButton, "login-exit");
        FxEffects.pressFx(exitButton);
        exitButton.setOnAction(e -> {
            if (AlertUtil.confirm("确定退出系统吗？")) {
                Platform.exit();
            }
        });

        // 一主一次成对，用比卡片间距更小的 8px 紧挨在一起（登录失败提示排在它们下方）
        VBox actionBox = new VBox(8, loginButton, exitButton);

        // ---- 错误提示 ----
        // 无错误时既不显示也不占位，卡片底部才不会拖出一块空白；报错时再让它参与布局
        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        UI.style(messageLabel, "login-error");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        card.getChildren().addAll(actionBox, messageLabel);

        // ---- 背景层：极光动效 + 玻璃卡 ----
        // 桌面底部被任务栏占掉一块，卡片若在窗口里上下均分留白，视觉重心会偏低；
        // 故卡片改为贴顶摆放，把多出来的一截全留给下方（见 GAP_TOP / GAP_BOTTOM）。
        // 极光层必须排在最底部，且自身鼠标穿透，不参与命中测试。
        StackPane bg = new StackPane(AuroraBackground.login(), card);
        UI.style(bg, "login-bg");
        StackPane.setAlignment(card, Pos.TOP_CENTER);
        StackPane.setMargin(card, new Insets(GAP_TOP, 24, 24, 24));

        // 小屏幕上按可视区收窄，避免窗口比屏幕还高（此时下方留白先被压缩）
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(bg, 560, Math.min(SCENE_HEIGHT, screen.getHeight() - 24));
        UI.apply(scene);
        UI.applyIcon(stage);
        stage.setTitle("南昌航空大学 · 学生宿舍管理系统");
        stage.setScene(scene);

        // 进场动效：卡片放大浮现，随后内部控件自上而下依次错峰淡入。
        // 注意：若窗口已处于显示中（从主界面「退出登录」返回本页），
        // setOnShown 不会再触发，卡片会一直停在 opacity=0 的透明态，界面看似卡死。
        // 因此在窗口已显示时需直接播放入场动画。
        card.setOpacity(0);
        card.setTranslateY(24);
        Runnable enterFx = () -> {
            FxEffects.popIn(card, Duration.millis(420), 0.94, Duration.ZERO);
            // 卡片先亮起来，里面再一层层铺开；起始延时压过卡片缩放最快的一段，避免糊成一团
            FxEffects.staggerIn(card, card.getChildren(),
                    Duration.millis(300), Duration.millis(42), Duration.millis(120));
        };
        if (stage.isShowing()) {
            enterFx.run();
        } else {
            stage.setOnShown(e -> enterFx.run());
        }
        stage.show();
        // 居中必须放在 show() 之后：窗口未显示时尺寸与装饰尚未确定，早调用等于没居中。
        UI.centerOnScreen(stage);

        // ---- 事件 ----
        Runnable tryLogin = () -> doLogin(usernameField.getText(), passwordField.getText(), messageLabel);
        loginButton.setOnAction(e -> tryLogin.run());
        passwordField.setOnAction(e -> tryLogin.run());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        usernameField.requestFocus();
    }

    /**
     * 头像外的流光圆环：一段渐隐的描边 + 断续的 dash，转起来像跑马灯。
     *
     * @param radius    圆环半径（像素）
     * @param width     描边宽度
     * @param period    转一圈的时长
     * @param clockwise 是否顺时针
     * @param dash      实线段长度（配合 {@code gap} 把圆环切成几段弧，转起来才有流动感）
     * @param gap       空档长度
     */
    private static Circle ring(double radius, double width, Duration period, boolean clockwise,
                               double dash, double gap) {
        Circle c = new Circle(radius);
        c.setFill(null);
        c.setStroke(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#8ec5ff")),
                new Stop(0.45, Color.web("#ffffff", 0.10)),
                new Stop(0.78, Color.web("#c4b5fd")),
                new Stop(1.00, Color.web("#8ec5ff", 0.0))));
        c.setStrokeWidth(width);
        c.setStrokeLineCap(StrokeLineCap.ROUND);
        c.getStrokeDashArray().setAll(dash, gap);
        c.setMouseTransparent(true);
        FxEffects.spin(c, period, clockwise);
        return c;
    }

    /**
     * 按账号框当前内容刷新头像。
     * 只有「完整且合法的学号」且该学号确实存在、其账号又更换过头像时才显示自定义头像，
     * 其余情况（含辅导员/宿管等非学号账号、学号打错）一律退回默认校徽。
     */
    private void refreshAvatar(ImageView avatarView, String username) {
        String id = username == null ? "" : username.trim();
        Image custom = null;
        if (StudentId.isValid(id) && DataCenter.instance().findStudentById(id) != null) {
            custom = UI.avatarImage(avatarService.avatarFileOfUsername(id));
        }
        avatarView.setImage(custom != null ? custom : UI.appIcon());
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
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
        FxEffects.shake(messageLabel);
    }
}

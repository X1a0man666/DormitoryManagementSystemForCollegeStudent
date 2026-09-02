package com.nchu.dorm.ui;

import com.nchu.dorm.model.Admin;
import com.nchu.dorm.model.Counselor;
import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.model.Person;
import com.nchu.dorm.model.RoleKey;
import com.nchu.dorm.model.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * 主框架：登录后按角色显示左侧功能菜单与右侧内容区。
 * 菜单与内容分发的核心依据是 {@link Person#getRoleKey()}（多态的体现）。
 */
public class MainFrame {

    private final Stage stage;
    private final Person user;
    private final BorderPane root = new BorderPane();

    public MainFrame(Stage stage, Person user) {
        this.stage = stage;
        this.user = user;
    }

    public void show() {
        root.setTop(buildTopBar());
        root.setLeft(buildMenu());
        root.setCenter(buildHome());

        Scene scene = new Scene(root, 980, 640);
        stage.setScene(scene);
        stage.setTitle("南昌航空大学学生宿舍管理系统 - " + user.getRoleName());
        stage.centerOnScreen();
        stage.show();
    }

    /** 顶部用户信息栏 */
    private Node buildTopBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.setStyle("-fx-background-color: #2c3e50;");

        Label welcome = new Label("欢迎，" + user.getName() + "（" + user.getRoleName() + "）");
        welcome.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        Label duty = new Label(user.getDutyDescription());
        duty.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutButton = new Button("退出登录");
        logoutButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        logoutButton.setOnAction(e -> new LoginView(stage).show());

        bar.getChildren().addAll(welcome, spacer, duty, logoutButton);
        return bar;
    }

    /** 左侧功能菜单（按角色动态生成） */
    private Node buildMenu() {
        ListView<String> menu = new ListView<>();
        menu.setPrefWidth(170);
        menu.setStyle("-fx-background-color: #ecf0f1;");

        ObservableList<String> items = FXCollections.observableArrayList();
        switch (user.getRoleKey()) {
            case RoleKey.STUDENT:
                items.addAll("我的宿舍", "宿舍申请", "我的申请");
                break;
            case RoleKey.COUNSELOR:
                items.addAll("宿舍审批");
                break;
            case RoleKey.DORM_STAFF:
                items.addAll("日常管理工作台");
                break;
            case RoleKey.ADMIN:
                items.addAll("宿管科工作台");
                break;
            default:
                break;
        }
        menu.setItems(items);

        menu.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                root.setCenter(buildView(newVal));
            }
        });
        // 不要 selectFirst()：初始居中显示的是欢迎首页（buildHome）。
        // 若预选第一项，会提前触发监听把中心设为该视图，随后 show() 里 buildHome() 又会覆盖它，
        // 造成“菜单已选中但内容仍是首页”的不一致——此时再点该项不会触发 selection 变化事件而“没反应”。
        return menu;
    }

    /** 菜单项 -> 具体内容视图 */
    private Node buildView(String menuItem) {
        switch (menuItem) {
            case "我的宿舍":
                return new StudentView((Student) user).buildMyDorm();
            case "宿舍申请":
                return new StudentView((Student) user).buildApply();
            case "我的申请":
                return new StudentView((Student) user).buildApplications();
            case "宿舍审批":
                return new CounselorView((Counselor) user).build();
            case "日常管理工作台":
                return new DormStaffView((DormStaff) user).build();
            case "宿管科工作台":
                return new AdminView((Admin) user).build();
            default:
                return new Label("功能建设中…");
        }
    }

    /** 默认首页（功能菜单概览） */
    private Node buildHome() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));

        Label welcome = new Label("欢迎使用南昌航空大学学生宿舍管理系统");
        welcome.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label tip = new Label("请在左侧选择功能菜单开始使用。");
        tip.setStyle("-fx-font-size: 14; -fx-text-fill: #7f8c8d;");

        box.getChildren().addAll(welcome, tip);
        return box;
    }
}

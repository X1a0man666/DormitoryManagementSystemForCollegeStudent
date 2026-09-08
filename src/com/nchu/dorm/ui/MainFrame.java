package com.nchu.dorm.ui;

import com.nchu.dorm.model.Admin;
import com.nchu.dorm.model.Counselor;
import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.model.Person;
import com.nchu.dorm.model.RoleKey;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.ui.component.AlertUtil;
import com.nchu.dorm.ui.component.FxEffects;
import com.nchu.dorm.ui.component.UI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主框架：登录后按角色显示顶部品牌栏、左侧功能菜单与右侧内容区。
 * <p>
 * 菜单项用 {@link ToggleButton}(ToggleGroup) 实现互斥选中并播放内容切换动效；
 * 首页以「功能概览卡片」呈现并可点击直达对应功能。菜单字符串映射仍集中在 {@link #buildView(String)}。
 */
public class MainFrame {

    /** 默认首页菜单文案。 */
    private static final String HOME = "首页";

    /** 各功能菜单的一句话引导（首页概览卡副标题）。 */
    private static final Map<String, String> TAGLINE = new HashMap<>();

    static {
        TAGLINE.put("我的宿舍", "查看个人档案与当前入住信息");
        TAGLINE.put("宿舍申请", "入住 / 转宿 / 退宿 / 转专业换宿");
        TAGLINE.put("我的申请", "追踪申请进度，待审批项可撤销");
        TAGLINE.put("购电", "为当前房间电费充值");
        TAGLINE.put("维修报修", "提交宿舍维修工单");
        TAGLINE.put("宿舍审批", "审批本学院学生的入住 / 换宿申请");
        TAGLINE.put("日常管理工作台", "总览本人负责楼栋概况");
        TAGLINE.put("夜归登记", "登记学生夜归情况");
        TAGLINE.put("卫生检查", "对分管楼栋房间检查评分");
        TAGLINE.put("贵重物品登记", "记录贵重物品进出楼栋");
        TAGLINE.put("宿管科工作台", "系统总览与待办入口");
        TAGLINE.put("楼栋分配", "调整楼栋与学院的归属");
        TAGLINE.put("宿舍管理人员管理", "增删改查楼栋管理员");
        TAGLINE.put("修理管理", "受理并办结维修工单");
        TAGLINE.put("售电", "处理学生购电缴费");
    }

    private final Stage stage;
    private final Person user;
    private final BorderPane root = new BorderPane();
    private final ToggleGroup navGroup = new ToggleGroup();
    /** 菜单文案 → 导航按钮，供首页卡片与外部跳转使用。 */
    private final Map<String, ToggleButton> navButtons = new LinkedHashMap<>();
    /** 当前内容页对应的菜单文案。 */
    private String currentItem;

    public MainFrame(Stage stage, Person user) {
        this.stage = stage;
        this.user = user;
    }

    public void show() {
        UI.style(root, "app");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        // 默认选中「首页」触发内容区渲染（含进场动效）
        navGroup.selectToggle(navButtons.get(HOME));

        Scene scene = new Scene(root, 1020, 680);
        UI.apply(scene);
        stage.setScene(scene);
        stage.setTitle("南昌航空大学学生宿舍管理系统 - " + user.getRoleName());
        stage.centerOnScreen();
        stage.show();
    }

    /** 顶部品牌栏。 */
    private Node buildTopBar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        UI.style(bar, "topbar");

        Label logo = new Label("寝");
        UI.style(logo, "logo-mark");

        VBox brand = new VBox(2);
        brand.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("南昌航空大学 · 学生宿舍管理系统");
        UI.style(title, "app-name");
        Label sub = new Label("Nanchang Hangkong University · Dormitory Management System");
        UI.style(sub, "app-sub");
        brand.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label chip = new Label(user.getRoleName());
        UI.style(chip, "role-chip");
        Label uname = new Label(user.getName() + "，你好");
        UI.style(uname, "topbar-user");

        Button logout = new Button("退出登录");
        UI.style(logout, "btn-logout");
        logout.setOnAction(e -> {
            if (AlertUtil.confirm("确定退出登录吗？")) {
                new LoginView(stage).show();
            }
        });

        bar.getChildren().addAll(logo, brand, spacer, chip, uname, logout);
        return bar;
    }

    /** 左侧功能菜单。 */
    private Node buildSidebar() {
        VBox side = new VBox(16);
        side.setPrefWidth(238);
        UI.style(side, "sidebar");

        Label head = new Label("功能菜单");
        UI.style(head, "side-head");

        VBox menu = new VBox(6);
        for (String item : menuItems()) {
            menu.getChildren().add(navItem(item));
        }
        side.getChildren().addAll(head, menu);
        return side;
    }

    private ToggleButton navItem(String text) {
        ToggleButton tb = new ToggleButton(text);
        tb.setToggleGroup(navGroup);
        tb.setUserData(text);
        tb.setMaxWidth(Double.MAX_VALUE);
        UI.style(tb, "nav-item");

        tb.selectedProperty().addListener((obs, oldVal, selected) -> {
            if (selected) {
                applyCenter(text);
            }
        });
        // 点击「当前选中项」时不将其取消选中（避免菜单空白）
        tb.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (tb.isSelected() && text.equals(currentItem)) {
                e.consume();
            }
        });
        navButtons.put(text, tb);
        return tb;
    }

    /** 导航文案（含「首页」）。 */
    private List<String> menuItems() {
        List<String> items = new ArrayList<>();
        items.add(HOME);
        items.addAll(roleItems());
        return items;
    }

    /** 当前角色的功能菜单文案（不含「首页」）。 */
    private List<String> roleItems() {
        List<String> items = new ArrayList<>();
        switch (user.getRoleKey()) {
            case RoleKey.STUDENT:
                items.add("我的宿舍");
                items.add("宿舍申请");
                items.add("我的申请");
                items.add("购电");
                items.add("维修报修");
                break;
            case RoleKey.COUNSELOR:
                items.add("宿舍审批");
                break;
            case RoleKey.DORM_STAFF:
                items.add("日常管理工作台");
                items.add("夜归登记");
                items.add("卫生检查");
                items.add("贵重物品登记");
                break;
            case RoleKey.ADMIN:
                items.add("宿管科工作台");
                items.add("楼栋分配");
                items.add("宿舍管理人员管理");
                items.add("修理管理");
                items.add("售电");
                break;
            default:
                break;
        }
        return items;
    }

    /** 应用菜单选中结果：更新高亮语义并在中心区做切换动画。 */
    private void applyCenter(String item) {
        if (item == null || item.equals(currentItem)) {
            return;
        }
        currentItem = item;
        FxEffects.swap(root, buildView(item));
    }

    /** 由首页概览卡等外部跳转指定功能页（驱动对应导航按钮选中，保证动画与高亮一致）。 */
    private void goTo(String item) {
        ToggleButton tb = navButtons.get(item);
        if (tb == null || tb.isSelected()) {
            return;
        }
        navGroup.selectToggle(tb);
    }

    /** 菜单项 → 具体内容视图。 */
    private Node buildView(String menuItem) {
        if (HOME.equals(menuItem)) {
            return buildHome();
        }
        switch (menuItem) {
            case "我的宿舍":
                return new StudentView((Student) user).buildMyDorm();
            case "宿舍申请":
                return new StudentView((Student) user).buildApply();
            case "我的申请":
                return new StudentView((Student) user).buildApplications();
            case "购电":
                return new StudentView((Student) user).buildElectricity();
            case "维修报修":
                return new StudentView((Student) user).buildRepair();
            case "宿舍审批":
                return new CounselorView((Counselor) user).build();
            case "日常管理工作台":
                return new DormStaffView((DormStaff) user).build();
            case "夜归登记":
                return new DormStaffView((DormStaff) user).buildNightReturn();
            case "卫生检查":
                return new DormStaffView((DormStaff) user).buildHygiene();
            case "贵重物品登记":
                return new DormStaffView((DormStaff) user).buildValuables();
            case "宿管科工作台":
                return new AdminView((Admin) user).build();
            case "楼栋分配":
                return new AdminView((Admin) user).buildBuildingAllocation();
            case "宿舍管理人员管理":
                return new AdminView((Admin) user).buildDormStaffManage();
            case "修理管理":
                return new AdminView((Admin) user).buildRepairManage();
            case "售电":
                return new AdminView((Admin) user).buildSellElectricity();
            default:
                Label stub = new Label("功能建设中…");
                UI.style(stub, UI.MUTED);
                return stub;
        }
    }

    /** 默认首页：欢迎语 + 功能概览卡片（点击直达对应功能）。 */
    private Node buildHome() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(36, 44, 44, 44));
        box.setAlignment(Pos.TOP_LEFT);

        Label greeting = new Label("欢迎使用学生宿舍管理系统，" + user.getName());
        UI.style(greeting, "home-greeting");
        Label sub = new Label(user.getRoleName() + " · " + user.getDutyDescription());
        UI.style(sub, "home-sub");
        box.getChildren().addAll(greeting, sub);

        Label prompt = new Label("功能概览（点击卡片进入）：");
        UI.style(prompt, UI.SECTION);
        box.getChildren().add(prompt);

        FlowPane cards = new FlowPane(16, 16);
        cards.setPrefWrapLength(760);
        for (String item : roleItems()) {
            cards.getChildren().add(homeCard(item));
        }
        box.getChildren().add(cards);
        return box;
    }

    private Node homeCard(String item) {
        VBox card = new VBox(8);
        card.setPrefWidth(224);
        card.setPrefHeight(104);
        card.setPadding(new Insets(14, 16, 14, 16));
        UI.style(card, "stat-card");
        FxEffects.hoverLift(card);

        Label title = new Label(item);
        UI.style(title, "stat-title");
        Label desc = new Label(TAGLINE.getOrDefault(item, "点击进入该功能"));
        UI.style(desc, "stat-desc");
        desc.setWrapText(true);

        card.getChildren().addAll(title, desc);
        card.setOnMouseClicked(e -> goTo(item));
        return card;
    }
}

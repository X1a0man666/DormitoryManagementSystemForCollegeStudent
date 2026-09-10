package com.nchu.dorm.ui;

import com.nchu.dorm.model.Admin;
import com.nchu.dorm.model.Counselor;
import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.model.Person;
import com.nchu.dorm.model.RoleKey;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.service.AvatarService;
import com.nchu.dorm.service.CounselorService;
import com.nchu.dorm.ui.component.AlertUtil;
import com.nchu.dorm.ui.component.AuroraBackground;
import com.nchu.dorm.ui.component.FxEffects;
import com.nchu.dorm.ui.component.UI;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主框架：登录后按角色显示顶部品牌栏与内容区。
 * <p>
 * 功能菜单不再常驻侧栏，而是收起为左上角的「≡」标识：鼠标移入展开、移出收起（见 {@link #buildMenuLayer()}）；
 * 菜单项用 {@link ToggleButton}(ToggleGroup) 实现互斥选中并播放内容切换动效，
 * 非首页在内容区顶部显示「返回主页」按钮（见 {@link #buildPageBar()}）。
 * 菜单字符串映射仍集中在 {@link #buildView(String)}。
 */
public class MainFrame {

    /** 默认首页菜单文案。 */
    private static final String HOME = "首页";

    /** 左上角标识与展开面板之间的间隔（8 + 44 + 8 = 60，与顶栏同高，面板正好自顶栏下沿展开）。 */
    private static final double MENU_GAP = 8;

    /** 顶栏右侧「退出」触发器与展开面板之间的间隔（15 + 30 + 15 = 60，面板同样自顶栏下沿展开）。 */
    private static final double EXIT_GAP = 15;

    /** 顶栏右侧为悬浮「退出」触发器预留的宽度（触发器是叠加层，不参与 HBox 布局，故用空位占出同样宽度）。 */
    private static final double EXIT_SLOT = 70;

    /** 顶栏流光带宽度。 */
    private static final double SHEEN_WIDTH = 220;

    /** 流光扫过一趟的时长。 */
    private static final Duration SHEEN_SWEEP = Duration.seconds(2.6);

    /** 流光一个完整周期（扫过 + 停顿），周期远大于扫过时长，所以是「偶尔扫一下」而不是一直闪。 */
    private static final Duration SHEEN_PERIOD = Duration.seconds(9.5);

    /** 首页卡片渐变徽标的配色数量（与 theme.css 的 .badge-0 ~ .badge-3 一一对应）。 */
    private static final int BADGE_VARIANTS = 4;

    /** 各功能菜单的一句话引导（首页概览卡副标题）。 */
    private static final Map<String, String> TAGLINE = new HashMap<>();

    static {
        TAGLINE.put("我的宿舍", "查看个人档案与当前入住信息");
        TAGLINE.put("宿舍申请", "提交入住 / 转宿 / 退宿 / 转专业换宿，并追踪进度");
        TAGLINE.put("购电", "为当前房间电费充值");
        TAGLINE.put("维修报修", "提交宿舍维修工单");
        TAGLINE.put("夜归确认", "确认宿管登记的夜归记录");
        TAGLINE.put("我的卫生检查", "查看本寝室评分评语与历史");
        TAGLINE.put("贵重物品确认", "确认未丢失，遗失可报宿管");
        TAGLINE.put("公告", "查看宿管科发布的全校公告");
        TAGLINE.put("设置", "更换头像、修改姓名与登录密码");
        TAGLINE.put("宿舍审批", "审批本学院学生的入住 / 换宿申请");
        TAGLINE.put("购电记录", "查看本专业学生的购电与售电进度");
        TAGLINE.put("报修记录", "查看本专业学生的报修工单与处理进度");
        TAGLINE.put("夜归记录", "查看本专业学生的夜归登记与确认情况");
        TAGLINE.put("卫生检查记录", "查看本专业学生寝室的卫生评分");
        TAGLINE.put("贵重物品记录", "查看本专业学生的贵重物品出入登记");
        TAGLINE.put("日常管理工作台", "总览本人负责楼栋概况");
        TAGLINE.put("夜归登记", "登记学生夜归情况");
        TAGLINE.put("卫生检查", "对分管楼栋房间检查评分");
        TAGLINE.put("贵重物品登记", "记录贵重物品进出楼栋");
        TAGLINE.put("遗失上报", "把学生报损汇总上报宿管科");
        TAGLINE.put("报修处理", "接收学生报修并上报宿管科");
        TAGLINE.put("宿管科工作台", "系统总览与待办入口");
        TAGLINE.put("楼栋分配", "调整楼栋与学院的归属");
        TAGLINE.put("宿舍管理人员管理", "增删改查楼栋管理员");
        TAGLINE.put("学生账号管理", "按学号/姓名重置学生登录密码");
        TAGLINE.put("维修管理", "受理并办结宿管上报的维修工单");
        TAGLINE.put("售电", "处理学生购电缴费");
        TAGLINE.put("遗失公告", "审核上报并发布全校公告");
    }

    private final Stage stage;
    private final Person user;
    private final AvatarService avatarService = new AvatarService();
    private final CounselorService counselorService = new CounselorService();
    /** 顶层容器：内容 shell 之上叠加左上角悬浮菜单。 */
    private final StackPane root = new StackPane();
    /** 内容区（供 {@link FxEffects#swap} 做页面切换动效）。 */
    private final BorderPane content = new BorderPane();
    private final ToggleGroup navGroup = new ToggleGroup();
    /** 菜单文案 → 导航按钮，供首页卡片与外部跳转使用。 */
    private final Map<String, ToggleButton> navButtons = new LinkedHashMap<>();
    /** 当前内容页对应的菜单文案。 */
    private String currentItem;

    /** 悬浮展开的功能菜单面板（默认隐藏，移入标识才出现）。 */
    private VBox menuPanel;
    /** 菜单是否处于展开态。 */
    private boolean menuOpen;
    /** 正在播放的展开/收起动画（连续悬停时先打断旧的）。 */
    private Animation menuAnim;

    /** 顶栏右侧悬浮展开的「退出」菜单面板（默认隐藏，移入触发器才出现）。 */
    private VBox exitPanel;
    /** 退出菜单是否处于展开态。 */
    private boolean exitOpen;
    /** 正在播放的退出菜单展开/收起动画。 */
    private Animation exitAnim;
    /** 内容区顶部条（承载「返回主页」，首页整条隐藏）。 */
    private HBox pageBar;
    /** 顶栏问候语标签（改名后需单独刷新，故持有引用）。 */
    private Label topBarUser;
    /** 顶栏头像视图（换头像后需单独刷新，故持有引用）。 */
    private ImageView topBarAvatar;
    /** 顶栏流光动画（无限循环，退出登录丢弃本界面时须显式停掉，否则连人带界面一起被引用住）。 */
    private Timeline topBarSheenAnim;

    public MainFrame(Stage stage, Person user) {
        this.stage = stage;
        this.user = user;
    }

    public void show() {
        UI.style(root, "app");

        BorderPane shell = new BorderPane();
        shell.setTop(buildTopBar());
        shell.setCenter(content);
        pageBar = buildPageBar();
        content.setTop(pageBar);
        // 柔光背景垫在最底层：各功能页的滚动面板都是透明的，这层底色才透得出来
        root.getChildren().addAll(AuroraBackground.app(), shell, buildMenuLayer(), buildExitLayer());

        // 默认选中「首页」触发内容区渲染（含进场动效）
        navGroup.selectToggle(navButtons.get(HOME));

        // 默认 1280x760 够放下各页「左侧表格 + 右侧审批/表单面板」，面板不再被挤扁；
        // 小屏幕上按可用区域收窄，避免窗口比屏幕还大。
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root,
                Math.min(1280, screen.getWidth() - 24),
                Math.min(760, screen.getHeight() - 24));
        UI.apply(scene);
        UI.applyIcon(stage);
        stage.setScene(scene);
        stage.setTitle("南昌航空大学学生宿舍管理系统 - " + user.getRoleName());
        stage.show();
        // 居中必须放在 show() 之后：窗口未显示时尺寸与装饰尚未确定，早调用等于没居中。
        UI.centerOnScreen(stage);
    }

    /** 顶部品牌栏。 */
    private Node buildTopBar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        UI.style(bar, "topbar");

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
        topBarUser = new Label(greetingText());
        UI.style(topBarUser, "topbar-user");

        // 「退出」触发器是叠加层（见 buildExitLayer），这里只空出同样宽度的位置，避免压住右侧问候语
        Region exitSlot = new Region();
        exitSlot.setMinWidth(EXIT_SLOT);
        exitSlot.setPrefWidth(EXIT_SLOT);

        bar.getChildren().addAll(buildLogo(), brand, spacer, chip, topBarUser, exitSlot);

        // 顶栏外包一层 StackPane：流光带作为叠加层压在 bar 之上，
        // 直接塞进 HBox 会被当成一个普通格子参与布局，把品牌区挤歪。
        return new StackPane(bar, buildTopBarSheen(bar));
    }

    /**
     * 顶栏流光：一条竖向柔光带不定期自左向右扫过，给静态渐变栏一点「活着」的感觉。
     * <p>
     * 位移量按顶栏宽度实时算（光带居中摆放，左右各需扫出半宽 + 半个带宽），
     * 故窗口缩放后扫过的行程依然完整，不会扫到一半就没了。
     */
    private Region buildTopBarSheen(HBox bar) {
        Region sheen = new Region();
        sheen.setPrefWidth(SHEEN_WIDTH);
        sheen.setMinWidth(SHEEN_WIDTH);
        sheen.setMaxWidth(SHEEN_WIDTH);
        sheen.setMouseTransparent(true);
        UI.style(sheen, "topbar-sheen");

        DoubleProperty progress = new SimpleDoubleProperty(0);
        // progress=0 → 光带整体停在最左画外；progress=1 → 停在最右画外
        sheen.translateXProperty().bind(
                progress.subtract(0.5).multiply(bar.widthProperty().add(SHEEN_WIDTH)));

        topBarSheenAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progress, 0, Interpolator.EASE_BOTH)),
                new KeyFrame(SHEEN_SWEEP, new KeyValue(progress, 1, Interpolator.EASE_BOTH)),
                // 结尾到下一个周期起点都停在画外，中间这段空白就是「扫完歇一会儿」
                new KeyFrame(SHEEN_PERIOD, new KeyValue(progress, 1)));
        topBarSheenAnim.setCycleCount(Animation.INDEFINITE);
        topBarSheenAnim.play();
        return sheen;
    }

    /**
     * 学生与辅导员都可在「设置」里自行改名与更换头像，改完同步顶栏的问候语与头像。
     * （首页欢迎语与各页姓名均按人名即时取值，切页时自动更新，无需处理。）
     */
    private void refreshProfile() {
        if (topBarUser != null) {
            topBarUser.setText(greetingText());
        }
        if (topBarAvatar != null) {
            Image custom = avatarImage();
            topBarAvatar.setImage(custom != null ? custom : UI.appIcon());
        }
    }

    /** 顶栏问候语，如「软件工程专业辅导员，你好」「张三，你好」。 */
    private String greetingText() {
        return personName() + "，你好";
    }

    /**
     * 人名：辅导员未自行改名（姓名仍等于工号）时显示「xx专业辅导员」，改过名则显示本人姓名；
     * 其余角色一律显示姓名。
     */
    private String personName() {
        if (RoleKey.COUNSELOR.equals(user.getRoleKey())) {
            return counselorService.displayName((Counselor) user);
        }
        return user.getName();
    }

    /**
     * 顶栏品牌标识（位于左上角「≡」菜单右侧）：本人已更换的头像，否则校徽；
     * 校徽资源也缺失时退回「寝」字，避免顶栏留白。
     */
    private Node buildLogo() {
        Image icon = avatarImage();
        if (icon == null) {
            icon = UI.appIcon();
        }
        if (icon == null) {
            Label fallback = new Label("寝");
            UI.style(fallback, "logo-mark");
            return fallback;
        }
        topBarAvatar = UI.circularAvatar(38);
        topBarAvatar.setImage(icon);
        UI.style(topBarAvatar, "logo-image");
        return topBarAvatar;
    }

    /** 本人头像图片；未更换或文件读不出时返回 null。 */
    private Image avatarImage() {
        return UI.avatarImage(avatarService.avatarFileOf(user));
    }

    /**
     * 左上角悬浮菜单：默认只露出「≡」标识，鼠标移入展开面板、移出收起。
     * <p>
     * 标识与面板放在同一个 VBox 内并打开 {@code pickOnBounds}，使鼠标从标识滑向面板时
     * 中间的空隙仍属于同一 hover 区域，不会误触收起。
     */
    private Node buildMenuLayer() {
        Button trigger = new Button("≡");
        trigger.setFocusTraversable(false);
        UI.style(trigger, "menu-trigger");
        FxEffects.pressFx(trigger);

        menuPanel = new VBox(6);
        UI.style(menuPanel, "menu-panel");
        Label head = new Label("功能菜单");
        UI.style(head, "menu-head");
        menuPanel.getChildren().add(head);
        for (String item : menuItems()) {
            menuPanel.getChildren().add(navItem(item));
        }
        hideMenuPanel();

        VBox layer = new VBox(MENU_GAP, trigger, menuPanel);
        layer.setAlignment(Pos.TOP_LEFT);
        // 只占内容尺寸：撑满 StackPane 会把整页都变成可 hover 的菜单区域
        layer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        layer.setPickOnBounds(true);
        UI.style(layer, "menu-layer");
        layer.hoverProperty().addListener((obs, was, isHover) -> {
            if (isHover) {
                expandMenu();
            } else {
                collapseMenu();
            }
        });
        StackPane.setAlignment(layer, Pos.TOP_LEFT);
        return layer;
    }

    private void expandMenu() {
        if (menuOpen) {
            return;
        }
        menuOpen = true;
        menuPanel.setVisible(true);
        menuPanel.setManaged(true);
        stopMenuAnim();
        menuAnim = FxEffects.slideInLeft(menuPanel);
        // 面板滑入的同时，菜单项自上而下依次浮现，展开过程就不再是「一整块啪地出现」
        FxEffects.staggerIn(menuPanel, menuPanel.getChildren(),
                Duration.millis(220), Duration.millis(28), Duration.millis(40));
    }

    private void collapseMenu() {
        if (!menuOpen) {
            return;
        }
        menuOpen = false;
        // 收起的回调可能晚于下一次展开（快速移入移出），故以 menuOpen 兜底，避免把已展开的面板藏掉
        stopMenuAnim();
        menuAnim = FxEffects.slideOutLeft(menuPanel, () -> {
            if (!menuOpen) {
                hideMenuPanel();
            }
        });
    }

    /** 打断上一段展开/收起动画，避免新旧动画同时改 opacity 造成残影。 */
    private void stopMenuAnim() {
        if (menuAnim != null) {
            menuAnim.stop();
        }
    }

    /** 收起后隐藏并让出布局：未展开时面板不占位，否则那块空白会一直可 hover。 */
    private void hideMenuPanel() {
        menuPanel.setVisible(false);
        menuPanel.setManaged(false);
    }

    /**
     * 顶栏右侧悬浮的「退出」菜单：默认只露出红色「退出」触发器，鼠标移入展开、移出收起，
     * 交互与左上角功能菜单一致，展开后提供「退出登录」「退出系统」两项。
     */
    private Node buildExitLayer() {
        Button trigger = new Button("退出");
        trigger.setFocusTraversable(false);
        UI.style(trigger, "exit-trigger");
        FxEffects.pressFx(trigger);

        exitPanel = new VBox(4);
        UI.style(exitPanel, "exit-panel");
        exitPanel.setAlignment(Pos.TOP_RIGHT);
        exitPanel.getChildren().addAll(
                exitItem("退出登录", this::logout),
                exitItem("退出系统", this::exitSystem));
        hideExitPanel();

        VBox layer = new VBox(EXIT_GAP, trigger, exitPanel);
        layer.setAlignment(Pos.TOP_RIGHT);
        // 只占内容尺寸：撑满 StackPane 会把整页都变成可 hover 的退出菜单区域
        layer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        layer.setPickOnBounds(true);
        UI.style(layer, "exit-layer");
        layer.hoverProperty().addListener((obs, was, isHover) -> {
            if (isHover) {
                expandExit();
            } else {
                collapseExit();
            }
        });
        StackPane.setAlignment(layer, Pos.TOP_RIGHT);
        return layer;
    }

    /** 退出菜单项：整行可点，点击后先收起菜单再执行动作。 */
    private Button exitItem(String text, Runnable action) {
        Button item = new Button(text);
        item.setFocusTraversable(false);
        item.setMaxWidth(Double.MAX_VALUE);
        UI.style(item, "exit-item");
        FxEffects.pressFx(item);
        item.setOnAction(e -> {
            collapseExit();
            action.run();
        });
        return item;
    }

    private void expandExit() {
        if (exitOpen) {
            return;
        }
        exitOpen = true;
        exitPanel.setVisible(true);
        exitPanel.setManaged(true);
        stopExitAnim();
        exitAnim = FxEffects.slideInLeft(exitPanel);
    }

    private void collapseExit() {
        if (!exitOpen) {
            return;
        }
        exitOpen = false;
        // 与功能菜单同理：收起的回调可能晚于下一次展开，故以 exitOpen 兜底
        stopExitAnim();
        exitAnim = FxEffects.slideOutLeft(exitPanel, () -> {
            if (!exitOpen) {
                hideExitPanel();
            }
        });
    }

    /** 打断上一段展开/收起动画，避免新旧动画同时改 opacity 造成残影。 */
    private void stopExitAnim() {
        if (exitAnim != null) {
            exitAnim.stop();
        }
    }

    /** 收起后隐藏并让出布局：未展开时面板不占位，否则那块空白会一直可 hover。 */
    private void hideExitPanel() {
        exitPanel.setVisible(false);
        exitPanel.setManaged(false);
    }

    /** 退出登录：确认后回到登录页（复用同一窗口）。 */
    private void logout() {
        if (AlertUtil.confirm("确定退出登录吗？")) {
            // 本界面即将被丢弃：先停掉无限循环的流光动画。
            // Timeline 经 KeyValue 反向持有节点，不停就会连整个主界面一起留在内存里继续跑。
            // 背景柔光动画无需在此处理，新的登录页背景会自行把上一套停掉。
            if (topBarSheenAnim != null) {
                topBarSheenAnim.stop();
            }
            new LoginView(stage).show();
        }
    }

    /** 退出系统：确认后关闭窗口并结束进程。 */
    private void exitSystem() {
        if (AlertUtil.confirm("确定退出系统吗？")) {
            Platform.exit();
        }
    }

    /** 内容区顶部条：非首页显示「返回主页」，首页整条隐藏不占位。 */
    private HBox buildPageBar() {
        Button back = new Button("← 返回主页");
        back.setFocusTraversable(false);
        UI.style(back, "btn-back");
        FxEffects.pressFx(back);
        back.setOnAction(e -> goTo(HOME));

        HBox bar = new HBox(back);
        bar.setAlignment(Pos.CENTER_LEFT);
        UI.style(bar, "page-bar");
        bar.setVisible(false);
        bar.setManaged(false);
        return bar;
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
                items.add("购电");
                items.add("维修报修");
                items.add("夜归确认");
                items.add("我的卫生检查");
                items.add("贵重物品确认");
                items.add("公告");
                items.add("设置");
                break;
            case RoleKey.COUNSELOR:
                items.add("宿舍审批");
                // 本专业学生各项生活记录：只读查看，不含修改入口
                items.add("购电记录");
                items.add("报修记录");
                items.add("夜归记录");
                items.add("卫生检查记录");
                items.add("贵重物品记录");
                items.add("公告");
                items.add("设置");
                break;
            case RoleKey.DORM_STAFF:
                items.add("日常管理工作台");
                items.add("夜归登记");
                items.add("卫生检查");
                items.add("贵重物品登记");
                items.add("遗失上报");
                items.add("报修处理");
                break;
            case RoleKey.ADMIN:
                items.add("宿管科工作台");
                items.add("楼栋分配");
                items.add("宿舍管理人员管理");
                items.add("学生账号管理");
                items.add("维修管理");
                items.add("售电");
                items.add("遗失公告");
                break;
            default:
                break;
        }
        return items;
    }

    /** 应用菜单选中结果：收起悬浮菜单、同步返回条显隐，并在内容区做切换动画。 */
    private void applyCenter(String item) {
        if (item == null || item.equals(currentItem)) {
            return;
        }
        currentItem = item;
        collapseMenu();
        boolean home = HOME.equals(item);
        pageBar.setVisible(!home);
        pageBar.setManaged(!home);
        Node view = buildView(item);
        FxEffects.swap(content, view, home ? () -> staggerHome(view) : () -> FxEffects.pageEnter(view));
    }

    /** 由首页概览卡 / 返回按钮等外部跳转指定功能页（驱动对应导航按钮选中，保证动画与高亮一致）。 */
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
            case "购电":
                return new StudentView((Student) user).buildElectricity();
            case "维修报修":
                return new StudentView((Student) user).buildRepair();
            case "夜归确认":
                return new StudentView((Student) user).buildNightReturnConfirm();
            case "我的卫生检查":
                return new StudentView((Student) user).buildHygiene();
            case "贵重物品确认":
                return new StudentView((Student) user).buildValuablesConfirm();
            case "公告":
                // 公告面向全校，学生与辅导员都能看；仅取数据的方式不同，故按角色分派
                return RoleKey.COUNSELOR.equals(user.getRoleKey())
                        ? new CounselorView((Counselor) user).buildAnnouncements()
                        : new StudentView((Student) user).buildAnnouncements();
            case "设置":
                return RoleKey.COUNSELOR.equals(user.getRoleKey())
                        ? new CounselorView((Counselor) user).buildSettings(this::refreshProfile)
                        : new StudentView((Student) user).buildSettings(this::refreshProfile);
            case "宿舍审批":
                return new CounselorView((Counselor) user).build();
            case "购电记录":
                return new CounselorView((Counselor) user).buildElectricity();
            case "报修记录":
                return new CounselorView((Counselor) user).buildRepairs();
            case "夜归记录":
                return new CounselorView((Counselor) user).buildNightReturns();
            case "卫生检查记录":
                return new CounselorView((Counselor) user).buildHygiene();
            case "贵重物品记录":
                return new CounselorView((Counselor) user).buildValuables();
            case "日常管理工作台":
                return new DormStaffView((DormStaff) user).build();
            case "夜归登记":
                return new DormStaffView((DormStaff) user).buildNightReturn();
            case "卫生检查":
                return new DormStaffView((DormStaff) user).buildHygiene();
            case "贵重物品登记":
                return new DormStaffView((DormStaff) user).buildValuables();
            case "遗失上报":
                return new DormStaffView((DormStaff) user).buildLostEscalation();
            case "报修处理":
                return new DormStaffView((DormStaff) user).buildRepairHandling();
            case "宿管科工作台":
                return new AdminView((Admin) user).build();
            case "楼栋分配":
                return new AdminView((Admin) user).buildBuildingAllocation();
            case "宿舍管理人员管理":
                return new AdminView((Admin) user).buildDormStaffManage();
            case "学生账号管理":
                return new AdminView((Admin) user).buildStudentAccountManage();
            case "维修管理":
                return new AdminView((Admin) user).buildRepairManage();
            case "售电":
                return new AdminView((Admin) user).buildSellElectricity();
            case "遗失公告":
                return new AdminView((Admin) user).buildAnnouncementManage();
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

        Label greeting = new Label("欢迎使用学生宿舍管理系统，" + personName());
        UI.style(greeting, "home-greeting");
        Label sub = new Label(user.getRoleName() + " · " + user.getDutyDescription());
        UI.style(sub, "home-sub");
        box.getChildren().addAll(greeting, sub);

        Label prompt = new Label("功能概览（点击卡片进入）：");
        UI.style(prompt, UI.SECTION);
        box.getChildren().add(prompt);

        FlowPane cards = new FlowPane(16, 16);
        cards.setPrefWrapLength(760);
        int index = 0;
        for (String item : roleItems()) {
            cards.getChildren().add(homeCard(item, index++));
        }
        box.getChildren().add(cards);
        return box;
    }

    /**
     * 首页功能卡：左侧渐变徽标 + 标题描述，悬停上浮并扫过一道高光。
     * <p>外面再套一层 StackPane 是为了让高光带能盖在卡片之上而又不参与卡片的内部布局。
     */
    private Node homeCard(String item, int index) {
        VBox card = new VBox(4);
        card.setPrefWidth(224);
        card.setPrefHeight(114);
        UI.style(card, "stat-card");
        FxEffects.hoverFloat(card, 5);

        Label badgeText = new Label(item.substring(0, 1));
        badgeText.setMouseTransparent(true);
        UI.style(badgeText, "card-badge-text");
        StackPane badge = new StackPane(badgeText);
        UI.style(badge, "card-badge", "badge-" + (index % BADGE_VARIANTS));

        Label title = new Label(item);
        UI.style(title, "stat-title");
        Label desc = new Label(TAGLINE.getOrDefault(item, "点击进入该功能"));
        UI.style(desc, "stat-desc");
        desc.setWrapText(true);

        VBox texts = new VBox(4, title, desc);
        HBox.setHgrow(texts, Priority.ALWAYS);
        HBox head = new HBox(12, badge, texts);
        head.setAlignment(Pos.TOP_LEFT);
        card.getChildren().add(head);

        Region sheen = new Region();
        sheen.setPrefWidth(90);
        sheen.setMinWidth(90);
        sheen.setMaxWidth(90);
        UI.style(sheen, "card-sheen");
        // 高光必须裁在卡片圆角内，否则会从卡片四角漏出去
        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.heightProperty());
        sheen.setClip(clip);
        FxEffects.shimmerOnHover(card, sheen);

        StackPane wrapper = new StackPane(card, sheen);
        wrapper.setOnMouseClicked(e -> goTo(item));
        return wrapper;
    }

    /** 首页入场：标题自上而下错峰淡入，底下那排卡片再一片片跟上。 */
    private void staggerHome(Node view) {
        if (!(view instanceof Pane)) {
            return;
        }
        Pane box = (Pane) view;
        FxEffects.staggerIn(box, box.getChildren(),
                Duration.millis(320), Duration.millis(70), Duration.millis(60));
        if (box.getChildren().isEmpty()) {
            return;
        }
        Node last = box.getChildren().get(box.getChildren().size() - 1);
        if (last instanceof Pane) {
            Pane cards = (Pane) last;
            // fromY 传 0：卡片自身挂着 hoverFloat 的位移动效，入场只做淡入，两套动画别抢 translateY
            FxEffects.staggerIn(cards, cards.getChildren(),
                    Duration.millis(340), Duration.millis(45), Duration.millis(230), 0);
        }
    }
}

package com.nchu.dorm.ui.component;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

/**
 * 界面主题统一入口：全局样式表 / 程序图标加载、样式类名常量、色值 → 按钮变体类映射。
 * 样式集中在 {@code ui/theme.css}，Java 代码不再散落内联 setStyle。
 */
public final class UI {

    private UI() {
    }

    /** 样式表在 classpath 中的路径（theme.css 与 ui 包同目录，随编译拷入 out）。 */
    private static final String CSS_PATH = "/com/nchu/dorm/ui/theme.css";

    /** 程序图标（校徽）在 classpath 中的路径，与 theme.css 同目录。 */
    private static final String ICON_PATH = "/com/nchu/dorm/ui/app-icon.png";

    /** 程序图标缓存（懒加载）。 */
    private static Image appIcon;

    /** 将全局主题挂到某 Scene（登录页 / 主框架 / 弹窗的 Scene 各自调用一次）。 */
    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().add(css());
    }

    /** 将全局主题挂到某个对话框（DialogPane 自带 Scene 子树，按钮在 button-bar 内）。 */
    public static void applyToDialog(DialogPane pane) {
        if (pane != null) {
            pane.getStylesheets().add(css());
        }
    }

    /** 样式表资源的 URL（toExternalForm 供 getStylesheets 使用）。 */
    public static String css() {
        return resource(CSS_PATH, "theme.css");
    }

    /** classpath 资源 URL；IDE 未标记资源根时，按与 class 同包的相对路径再试一次。 */
    private static String resource(String path, String samePackageName) {
        URL url = UI.class.getResource(path);
        if (url == null) {
            url = UI.class.getResource(samePackageName);
        }
        return url == null ? "" : url.toExternalForm();
    }

    /** 程序图标（校徽）：窗口图标与顶栏品牌标识共用，只加载一次。 */
    public static Image appIcon() {
        if (appIcon == null) {
            String url = resource(ICON_PATH, "app-icon.png");
            if (!url.isEmpty()) {
                appIcon = new Image(url);
            }
        }
        return appIcon;
    }

    /**
     * 把窗口在屏幕可视区内居中（可视区已扣掉任务栏，故窗口不会贴到任务栏上）。
     * <p>必须在 {@code show()} 之后调用：窗口未显示时尺寸与装饰都还没确定，量出来的高度是错的。
     * <p>不用 {@link Stage#centerOnScreen()}：实测在本机上它不生效——窗口会停在偏上约 50px 的位置，
     * 连续调用两次也一样，因此改为显式设置坐标。
     */
    public static void centerOnScreen(Stage stage) {
        if (stage == null) {
            return;
        }
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setX(screen.getMinX() + (screen.getWidth() - stage.getWidth()) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - stage.getHeight()) / 2);
    }

    /** 给窗口挂上程序图标（标题栏 / 任务栏 / Alt+Tab 共用）。重复调用不会重复挂载（退出登录复用同一窗口）。 */
    public static void applyIcon(Stage stage) {
        Image icon = appIcon();
        if (stage != null && icon != null && !stage.getIcons().contains(icon)) {
            stage.getIcons().add(icon);
        }
    }

    /** 批量添加样式类（不覆盖已有类）。 */
    public static void style(Node node, String... classes) {
        if (node != null) {
            node.getStyleClass().addAll(classes);
        }
    }

    // ---------- 头像 ----------

    /**
     * 圆形头像视图：把方形图片裁成正圆，登录页 / 顶栏 / 设置页共用。
     * 图片由调用方 {@code setImage}；传 null 时显示为空白。
     */
    public static ImageView circularAvatar(double size) {
        ImageView view = new ImageView();
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setClip(new Circle(size / 2, size / 2, size / 2));
        return view;
    }

    /**
     * 加载头像图片文件；未选文件、文件不存在或不是可解析的图片时返回 null，
     * 由调用方退回默认头像（校徽），不抛异常打断界面。
     */
    public static Image avatarImage(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            Image image = new Image(file.toURI().toString());
            return image.isError() ? null : image;
        } catch (RuntimeException e) {
            // 图片损坏/格式不支持：退回默认头像，不让一张坏图把界面带崩
            System.err.println("[UI] 头像加载失败 " + file.getName() + "：" + e.getMessage());
            return null;
        }
    }

    // ---------- 样式类名常量 ----------

    public static final String PAGE_TITLE = "page-title";
    public static final String SECTION = "section-title";
    public static final String CARD = "card";
    public static final String PAGE_SCROLL = "page-scroll";
    /** 卡片外观的滚动面板（右侧审批面板等：内容超高时内部滚动）。 */
    public static final String PANEL_SCROLL = "panel-scroll";
    public static final String FIELD = "field-label";
    public static final String KEY = "kv-key";
    public static final String MUTED = "muted";
    public static final String FAINT = "faint";
    public static final String INK = "text-ink";
    public static final String DETAIL = "detail-line";
    public static final String OK = "text-success";
    public static final String WARN = "text-warning";
    public static final String WARN_DEEP = "text-warning-deep";
    public static final String DANGER = "text-danger";

    public static final String BTN = "btn";
    public static final String BTN_PRIMARY = "btn-primary";
    public static final String BTN_SUCCESS = "btn-success";
    public static final String BTN_WARNING = "btn-warning";
    public static final String BTN_DANGER = "btn-danger";
    public static final String BTN_DARK = "btn-dark";
    public static final String BTN_GHOST = "btn-ghost";

    // ---------- 旧色值 → 按钮变体类 映射 ----------

    /** 历史代码 button(text, "#xxxxxx", handler) 传来的色值，统一转成按钮变体类。 */
    public static String variantClass(String hex) {
        if (hex == null) {
            return BTN_DARK;
        }
        switch (hex.trim().toLowerCase()) {
            case "#3498db":
                return BTN_PRIMARY;
            case "#27ae60":
                return BTN_SUCCESS;
            case "#e67e22":
                return BTN_WARNING;
            case "#e74c3c":
                return BTN_DANGER;
            case "#2c3e50":
                return BTN_DARK;
            default:
                // 兜底：避免未知色把白字白底弄到不可读
                System.err.println("[UI] 未识别的按钮色值 " + hex + "，已使用深蓝变体");
                return BTN_DARK;
        }
    }
}

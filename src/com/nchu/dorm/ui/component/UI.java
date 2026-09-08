package com.nchu.dorm.ui.component;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;

/**
 * 界面主题统一入口：全局样式表加载、样式类名常量、色值 → 按钮变体类映射。
 * 样式集中在 {@code ui/theme.css}，Java 代码不再散落内联 setStyle。
 */
public final class UI {

    private UI() {
    }

    /** 样式表在 classpath 中的路径（theme.css 与 ui 包同目录，随编译拷入 out）。 */
    private static final String CSS_PATH = "/com/nchu/dorm/ui/theme.css";

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
        URL url = UI.class.getResource(CSS_PATH);
        if (url == null) {
            // 容错：以相对方式再试一次（IDE 未标记资源根时 css 与 class 同包输出）
            url = UI.class.getResource("theme.css");
        }
        return url == null ? "" : url.toExternalForm();
    }

    /** 批量添加样式类（不覆盖已有类）。 */
    public static void style(Node node, String... classes) {
        if (node != null) {
            node.getStyleClass().addAll(classes);
        }
    }

    // ---------- 样式类名常量 ----------

    public static final String PAGE_TITLE = "page-title";
    public static final String SECTION = "section-title";
    public static final String CARD = "card";
    public static final String PAGE_SCROLL = "page-scroll";
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

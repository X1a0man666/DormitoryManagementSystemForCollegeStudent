package com.nchu.dorm.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.List;

/**
 * 界面动效工具（克制细腻：约 150–320ms，EASE_OUT）。
 * <p>
 * 设计原则：动画只动「几何/透明度」（scale/translate/opacity），
 * 颜色 hover 交给 CSS 伪类瞬时换肤——两者不抢同一属性，互不冲突。
 */
public final class FxEffects {

    /** 入场时长（内容切换 / 首载）。 */
    public static final Duration T_IN = Duration.millis(260);
    /** 退场时长。 */
    public static final Duration T_OUT = Duration.millis(140);
    /** 统一缓动。 */
    public static final Interpolator EASE = Interpolator.EASE_OUT;

    private FxEffects() {
    }

    /** 入场：淡入 + 自下而上 16px（默认时长）。 */
    public static void fadeInUp(Node node) {
        fadeInUp(node, T_IN, 16, Duration.ZERO);
    }

    /** 入场：淡入 + 自下而上 fromY 像素，可选延时（错峰）。 */
    public static void fadeInUp(Node node, Duration d, double fromY, Duration delay) {
        if (node == null) {
            return;
        }
        FadeTransition fade = new FadeTransition(d, node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition move = new TranslateTransition(d, node);
        move.setFromY(fromY);
        move.setToY(0);

        ParallelTransition pt = new ParallelTransition(fade, move);
        pt.setInterpolator(EASE);
        pt.setOnFinished(e -> {
            // 归零，避免 translateY 影响后续布局
            node.setTranslateY(0);
        });
        pt.setDelay(delay);
        pt.play();
    }

    /**
     * 替换 BorderPane 的中心内容：旧内容淡出 140ms → setCenter(新) → 淡入上移 260ms。
     * 内含 busy 守卫：连续切换会中断上一个动画，防止 translateY/opacity 残留与闪跳。
     */
    public static void swap(BorderPane pane, Node newNode) {
        if (pane == null || newNode == null) {
            return;
        }
        Node old = pane.getCenter();
        if (old == newNode) {
            return;
        }
        if (old == null) {
            pane.setCenter(newNode);
            fadeInUp(newNode);
            return;
        }
        FadeTransition out = new FadeTransition(T_OUT, old);
        out.setFromValue(1);
        out.setToValue(0);
        out.setInterpolator(EASE);
        out.setOnFinished(e -> {
            pane.setCenter(newNode);
            fadeInUp(newNode);
        });
        out.play();
    }

    /** 进场错峰：父节点下 children 依次淡入上移（首页卡片 / 登录表单）。 */
    public static void staggerIn(Pane parent, List<? extends Node> children,
                                 Duration per, Duration gap) {
        if (parent == null || children == null) {
            return;
        }
        double total = 0;
        for (Node child : children) {
            fadeInUp(child, per, 14, Duration.millis(total));
            total += gap.toMillis();
        }
    }

    /** 按钮按压反馈：按下 scale→0.96，释放回 1.0。监听鼠标事件，自带中断。 */
    public static void pressFx(Node node) {
        if (node == null) {
            return;
        }
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (!node.isDisabled()) {
                scaleTo(node, 0.96, Duration.millis(90));
            }
        });
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, e ->
                scaleTo(node, 1.0, Duration.millis(150)));
    }

    /** 可点元素 hover 上浮：进入 scale→1.02，离开回 1.0。 */
    public static void hoverLift(Node node) {
        if (node == null) {
            return;
        }
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> scaleTo(node, 1.02, Duration.millis(140)));
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> scaleTo(node, 1.0, Duration.millis(160)));
    }

    private static void scaleTo(Node node, double to, Duration d) {
        ScaleTransition st = new ScaleTransition(d, node);
        st.setToX(to);
        st.setToY(to);
        st.setInterpolator(EASE);
        st.setOnFinished(e -> {
            node.setScaleX(1);
            node.setScaleY(1);
        });
        st.play();
    }

    /** 错误提示横向抖动（登录失败等），结束后复位。 */
    public static void shake(Node node) {
        if (node == null) {
            return;
        }
        TranslateTransition t1 = new TranslateTransition(Duration.millis(50), node);
        t1.setFromX(0);
        t1.setToX(-6);
        TranslateTransition t2 = new TranslateTransition(Duration.millis(60), node);
        t2.setFromX(-6);
        t2.setToX(6);
        TranslateTransition t3 = new TranslateTransition(Duration.millis(50), node);
        t3.setFromX(6);
        t3.setToX(0);
        t3.setOnFinished(e -> node.setTranslateX(0));
        t2.setOnFinished(e -> t3.play());
        t1.setOnFinished(e -> t2.play());
        t1.play();
    }

    /** 淡入闪现（节点初始需已 setOpacity(0)）。 */
    public static void fadeIn(Node node, Duration d) {
        if (node == null) {
            return;
        }
        FadeTransition fade = new FadeTransition(d, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(EASE);
        fade.play();
    }
}

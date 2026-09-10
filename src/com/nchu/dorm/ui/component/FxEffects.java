package com.nchu.dorm.ui.component;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.List;

/**
 * 界面动效工具（克制细腻：约 140–460ms，EASE_OUT）。
 * <p>
 * 设计原则：动画只动「几何/透明度」（scale/translate/rotate/opacity），
 * 颜色 hover 交给 CSS 伪类瞬时换肤——两者不抢同一属性，互不冲突。
 * <p>
 * 循环类的常驻动画（{@link #floatIdle} / {@link #spin}）只在登录页等少量节点上使用，
 * 不要挂到列表单元格上：单元格会被复用，动画既跟着乱跑又白白吃帧。
 */
public final class FxEffects {

    /** 入场时长（内容切换 / 首载）。 */
    public static final Duration T_IN = Duration.millis(260);
    /** 退场时长。 */
    public static final Duration T_OUT = Duration.millis(140);
    /** 统一缓动。 */
    public static final Interpolator EASE = Interpolator.EASE_OUT;
    /** 悬浮菜单展开时长（比页面切换轻快，跟手）。 */
    private static final Duration T_MENU = Duration.millis(180);
    /** 悬浮菜单收起/滑入的横向位移。 */
    private static final double MENU_SHIFT = 10;

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
        // 带动画延时（错峰）时，动画开始前节点会保持原态「先亮一下」再跳回透明，
        // 看上去是一次闪跳；故先把初始态直接落到位。fromY 为 0 时不碰 translateY，
        // 免得把元素身上正跑着的 hover 位移动效一并抹掉。
        if (delay != null && delay.greaterThan(Duration.ZERO)) {
            node.setOpacity(0);
            if (fromY != 0) {
                node.setTranslateY(fromY);
            }
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
     * 入场：淡入 + 由 fromScale 轻微放大到 1（卡片级入场，比 {@link #fadeInUp} 更有「浮现」感）。
     * 缩放不会影响布局，故结束后无需复位到 1 以外的值。
     */
    public static void popIn(Node node, Duration d, double fromScale, Duration delay) {
        if (node == null) {
            return;
        }
        node.setOpacity(0);
        node.setScaleX(fromScale);
        node.setScaleY(fromScale);

        FadeTransition fade = new FadeTransition(d, node);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(d, node);
        scale.setFromX(fromScale);
        scale.setFromY(fromScale);
        scale.setToX(1);
        scale.setToY(1);

        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.setInterpolator(EASE);
        pt.setOnFinished(e -> {
            node.setOpacity(1);
            node.setScaleX(1);
            node.setScaleY(1);
        });
        pt.setDelay(delay);
        pt.play();
    }

    /**
     * 替换 BorderPane 的中心内容：旧内容淡出 140ms → setCenter(新) → 入场。
     * 内含 busy 守卫：连续切换会中断上一个动画，防止 translateY/opacity 残留与闪跳。
     */
    public static void swap(BorderPane pane, Node newNode) {
        swap(pane, newNode, null);
    }

    /**
     * 见 {@link #swap(BorderPane, Node)}。
     *
     * @param afterIn 新内容入场动效的接管回调，可为 null。
     *                <p>传 null：容器整体淡入上移。
     *                <p>传了回调：容器保持不透明，由回调自行安排（通常是对子元素错峰）。
     *                两种情况必须二选一——若容器淡入的同时子元素也在错峰，两层透明度相乘，
     *                开头一段会又灰又闷，看着像没渲染完。
     */
    public static void swap(BorderPane pane, Node newNode, Runnable afterIn) {
        if (pane == null || newNode == null) {
            return;
        }
        Node old = pane.getCenter();
        if (old == newNode) {
            return;
        }
        if (old == null) {
            pane.setCenter(newNode);
            enter(newNode, afterIn);
            return;
        }
        FadeTransition out = new FadeTransition(T_OUT, old);
        out.setFromValue(1);
        out.setToValue(0);
        out.setInterpolator(EASE);
        out.setOnFinished(e -> {
            pane.setCenter(newNode);
            enter(newNode, afterIn);
        });
        out.play();
    }

    private static void enter(Node newNode, Runnable afterIn) {
        if (afterIn == null) {
            fadeInUp(newNode);
            return;
        }
        // 子元素错峰自己会把节点从透明带到不透明，容器只需保证不是半透明残留态
        newNode.setOpacity(1);
        newNode.setTranslateY(0);
        afterIn.run();
    }

    /**
     * 取页面内容的根容器：大多数功能页外面包了一层 {@link ScrollPane}，
     * 真正装着各个板块的是它的内容 VBox，错峰动效要作用在这一层上。
     * 不是容器（或空页）时返回 null，调用方跳过动画即可。
     */
    public static Pane contentRoot(Node view) {
        if (view instanceof ScrollPane sp) {
            return sp.getContent() instanceof Pane p ? p : null;
        }
        return view instanceof Pane p ? p : null;
    }

    /**
     * 页面入场：内容根下的各个板块自上而下依次淡入上移。
     * <p>二级界面（表格 + 右侧面板 / 表单 + 列表）由多个独立板块拼成，
     * 整块淡入会显得很平；按板块错峰才看得出层次。
     */
    public static void pageEnter(Node view) {
        Pane root = contentRoot(view);
        if (root == null) {
            return;
        }
        staggerIn(root, root.getChildren(),
                Duration.millis(320), Duration.millis(65), Duration.millis(40));
    }

    /** 进场错峰：父节点下 children 依次淡入上移（首页卡片 / 登录表单 / 展开的菜单）。 */
    public static void staggerIn(Pane parent, List<? extends Node> children,
                                 Duration per, Duration gap) {
        staggerIn(parent, children, per, gap, Duration.ZERO, 14);
    }

    /** 见 {@link #staggerIn(Pane, List, Duration, Duration)}；start 为整体起始延时。 */
    public static void staggerIn(Pane parent, List<? extends Node> children,
                                 Duration per, Duration gap, Duration start) {
        staggerIn(parent, children, per, gap, start, 14);
    }

    /**
     * 见 {@link #staggerIn(Pane, List, Duration, Duration)}。
     *
     * @param start 整体起始延时
     * @param fromY 位移量；传 0 表示只淡入不位移——元素自身还挂着 hover 位移动效（如首页卡片）时
     *              必须用 0，否则两套动画会抢 translateY，入场期间鼠标一进一出就会被拽回去
     */
    public static void staggerIn(Pane parent, List<? extends Node> children,
                                 Duration per, Duration gap, Duration start, double fromY) {
        if (parent == null || children == null) {
            return;
        }
        double total = start == null ? 0 : start.toMillis();
        for (Node child : children) {
            fadeInUp(child, per, fromY, Duration.millis(total));
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

    /**
     * 可点元素 hover 上浮（位移版）：进入 translateY→-px，离开回 0。
     * 与 {@link #hoverLift} 的缩放版分开提供：缩放会连阴影一起放大，
     * 卡片类大块元素用位移更稳，也不会和按压缩放抢 scale 通道。
     */
    public static void hoverFloat(Node node, double px) {
        if (node == null) {
            return;
        }
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> translateYTo(node, -px, Duration.millis(170)));
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> translateYTo(node, 0, Duration.millis(220)));
    }

    /**
     * 悬停高光：鼠标移入宿主后，sheen 光带自左向右扫过一遍。
     * <p>行程按宿主宽度现算，故宿主必须是有宽度的 {@link Region}（卡片、按钮等），
     * 且 sheen 已由调用方摆在宿主之上（通常和宿主同为某个 StackPane 的子节点）。
     */
    public static void shimmerOnHover(Region host, Region sheen) {
        if (host == null || sheen == null) {
            return;
        }
        sheen.setMouseTransparent(true);
        sheen.setOpacity(0);
        host.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            double w = host.getWidth();
            if (w <= 0) {
                return;
            }
            // 光带在 StackPane 里是居中的，故左右各扫出半个卡宽即可完整离场
            Timeline sweep = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(sheen.translateXProperty(), -w * 0.75),
                            new KeyValue(sheen.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(150),
                            new KeyValue(sheen.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(760),
                            new KeyValue(sheen.translateXProperty(), w * 0.75),
                            new KeyValue(sheen.opacityProperty(), 0.0)));
            sweep.play();
        });
    }

    /** 常驻循环：原地缓慢上下浮动（登录页头像）。 */
    public static void floatIdle(Node node) {
        if (node == null) {
            return;
        }
        TranslateTransition floatUp = new TranslateTransition(Duration.seconds(3.2), node);
        floatUp.setToY(-5);
        floatUp.setInterpolator(Interpolator.EASE_BOTH);
        floatUp.setAutoReverse(true);
        floatUp.setCycleCount(Animation.INDEFINITE);
        floatUp.play();
    }

    /** 常驻循环：绕自身中心匀速旋转（头像光环），clockwise 为 false 时逆时针。 */
    public static void spin(Node node, Duration period, boolean clockwise) {
        if (node == null) {
            return;
        }
        RotateTransition r = new RotateTransition(period, node);
        r.setByAngle(clockwise ? 360 : -360);
        // 匀速：加缓动会让转速忽快忽慢，看着像卡顿
        r.setInterpolator(Interpolator.LINEAR);
        r.setCycleCount(Animation.INDEFINITE);
        r.play();
    }

    /**
     * 缩放到位后「停在那儿」，不回弹。
     * <p>
     * hover 上浮要的是「移入时保持放大、移出才复原」，所以这里不能在动画结束时复位到 1——
     * 那样放大刚生效就被抹掉，整个 hoverLift 等于没写。需要回弹的场景由调用方显式给终点值。
     */
    private static void scaleTo(Node node, double to, Duration d) {
        ScaleTransition st = new ScaleTransition(d, node);
        st.setToX(to);
        st.setToY(to);
        st.setInterpolator(EASE);
        st.play();
    }

    private static void translateYTo(Node node, double to, Duration d) {
        TranslateTransition tt = new TranslateTransition(d, node);
        tt.setToY(to);
        tt.setInterpolator(EASE);
        tt.play();
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

    /** 左上角悬浮菜单展开：淡入 + 自左向右滑入。返回的动画可由调用方 stop（连续悬停时打断）。 */
    public static Animation slideInLeft(Node node) {
        if (node == null) {
            return null;
        }
        FadeTransition fade = new FadeTransition(T_MENU, node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition move = new TranslateTransition(T_MENU, node);
        move.setFromX(-MENU_SHIFT);
        move.setToX(0);

        ParallelTransition pt = new ParallelTransition(fade, move);
        pt.setInterpolator(EASE);
        pt.setOnFinished(e -> node.setTranslateX(0));
        pt.play();
        return pt;
    }

    /** 左上角悬浮菜单收起：淡出 + 左移，结束后执行 onFinished（调用方据此隐藏面板）。 */
    public static Animation slideOutLeft(Node node, Runnable onFinished) {
        if (node == null) {
            return null;
        }
        FadeTransition fade = new FadeTransition(T_OUT, node);
        fade.setFromValue(1);
        fade.setToValue(0);

        TranslateTransition move = new TranslateTransition(T_OUT, node);
        move.setFromX(0);
        move.setToX(-MENU_SHIFT);

        ParallelTransition pt = new ParallelTransition(fade, move);
        pt.setInterpolator(EASE);
        pt.setOnFinished(e -> {
            node.setTranslateX(0);
            if (onFinished != null) {
                onFinished.run();
            }
        });
        pt.play();
        return pt;
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

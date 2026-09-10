package com.nchu.dorm.ui.component;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 页面背景层：品牌渐变底 + 缓慢飘移的极光光斑（登录页再叠一层星尘粒子）。
 * <p>
 * 作为容器（StackPane）的第一个子节点铺满整页，鼠标事件完全穿透，不影响任何交互。
 *
 * <p>实现要点：
 * <ul>
 *   <li>光斑用 {@link RadialGradient}（中心实、边缘渐隐）代替高斯模糊：模糊效果每帧都要重算，
 *       整屏尺寸下很卡；径向渐变交给 GPU 填充，几乎零成本，观感同样柔和。</li>
 *   <li>深色底用 {@link BlendMode#SCREEN} 让光斑相互叠加提亮，避免互相覆盖后发灰发脏。</li>
 *   <li>粒子只用一个 {@link Canvas} 绘制，比几十个 Circle 节点轻得多。</li>
 *   <li>粒子坐标全部用「黄金比低差异序列」的伪随机数生成，不用 {@code Math.random()}：
 *       每次启动星图一致，截图比对时不会因为随机分布而对不上。</li>
 * </ul>
 *
 * <p>生命周期：同一时刻只应存在一套背景（登录页 或 主界面）。本类用静态 {@link #current}
 * 记住最近一次创建的实例，新建时先停掉上一套的时间轴与粒子计时器——
 * 否则「退出登录 → 回登录页」会不断叠加永不回收的动画，同时拖慢帧率。
 */
public final class AuroraBackground {

    private AuroraBackground() {
    }

    /** 最近创建的背景实例；新实例诞生时负责把它停掉（见类注释的「生命周期」）。 */
    private static Aurora current;

    /** 登录页背景：深空蓝底 + 高饱和极光 + 星尘粒子。 */
    public static Pane login() {
        return new Aurora(true);
    }

    /** 主界面背景：浅色柔光，压在内容卡片之下，只做若有若无的底纹。 */
    public static Pane app() {
        return new Aurora(false);
    }

    // ================================================================

    /** 背景实现：一个铺满容器的 Pane，内部只有光斑、暗角、（可选）粒子三层。 */
    private static final class Aurora extends Pane {

        /** 本实例启动的全部循环动画，{@link #stop()} 时统一收尾。 */
        private final List<Animation> anims = new ArrayList<>();

        /** 粒子绘制计时器（仅登录页有）。 */
        private AnimationTimer timer;

        Aurora(boolean dark) {
            // 先停掉上一套背景，避免两套动画同时跑
            if (current != null) {
                current.stop();
            }
            current = this;

            setMinSize(0, 0);
            setMouseTransparent(true);
            UI.style(this, dark ? "aurora-dark" : "aurora-light");

            if (dark) {
                buildAurora();
                buildVignette();
                buildParticles();
            } else {
                buildSoftGlow();
            }
        }

        /** 停掉本背景的全部循环动画（背景被替换时调用）。 */
        private void stop() {
            for (Animation a : anims) {
                a.stop();
            }
            anims.clear();
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        }

        // ---------- 登录页：极光 + 暗角 + 星尘 ----------

        /**
         * 五片大光斑，按不同周期在画面里缓慢游走，叠出极光的流动感。
         * 参数顺序：中心 x/y（占容器比例）、横/纵半径（占容器比例）、颜色、中心不透明度、
         * 横向/纵向漂移像素、漂移一个来回的秒数。
         */
        private void buildAurora() {
            blob(0.18, 0.14, 0.62, 0.52, "#2563eb", 0.62, 130, 90, 21);
            blob(0.86, 0.22, 0.58, 0.46, "#7c3aed", 0.50, -110, 80, 27);
            blob(0.52, 0.92, 0.70, 0.44, "#06b6d4", 0.42, 90, -70, 24);
            blob(0.10, 0.78, 0.46, 0.40, "#1d4ed8", 0.52, 120, -60, 31);
            blob(0.94, 0.86, 0.50, 0.42, "#9333ea", 0.38, -90, -90, 19);
        }

        /**
         * 加一片光斑。
         *
         * @param cx    中心横坐标（占容器宽度的比例）
         * @param cy    中心纵坐标（占容器高度的比例）
         * @param rx    横半径（占容器宽度的比例）
         * @param ry    纵半径（占容器高度的比例）
         * @param hex   光斑颜色
         * @param alpha 中心不透明度（0–1）
         * @param dx    横向漂移距离（像素，负值向左）
         * @param dy    纵向漂移距离（像素，负值向上）
         * @param secs  漂移一个来回的时长（秒，各片取不同值，合起来就不会整齐划一地呼吸）
         */
        private void blob(double cx, double cy, double rx, double ry, String hex,
                          double alpha, double dx, double dy, double secs) {
            Ellipse e = new Ellipse();
            e.setFill(soft(hex, alpha));
            // 斜一点，更像极光带而不是一个个圆斑
            e.setRotate(-18);
            // 光斑相互叠加时提亮而不是互相盖住
            e.setBlendMode(BlendMode.SCREEN);
            e.setMouseTransparent(true);

            // 随容器尺寸缩放：半径按比例跟随，因此窗口拉伸/缩小都不会让光斑跑偏
            e.radiusXProperty().bind(widthProperty().multiply(rx));
            e.radiusYProperty().bind(heightProperty().multiply(ry));
            e.centerXProperty().bind(widthProperty().multiply(cx));
            e.centerYProperty().bind(heightProperty().multiply(cy));

            Timeline t = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(e.translateXProperty(), 0, Interpolator.EASE_BOTH),
                            new KeyValue(e.translateYProperty(), 0, Interpolator.EASE_BOTH),
                            new KeyValue(e.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                            new KeyValue(e.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(secs),
                            new KeyValue(e.translateXProperty(), dx, Interpolator.EASE_BOTH),
                            new KeyValue(e.translateYProperty(), dy, Interpolator.EASE_BOTH),
                            new KeyValue(e.scaleXProperty(), 1.18, Interpolator.EASE_BOTH),
                            new KeyValue(e.scaleYProperty(), 0.86, Interpolator.EASE_BOTH)));
            // 来回往复：光斑永远走不到头，也就看不出循环点
            t.setAutoReverse(true);
            t.setCycleCount(Animation.INDEFINITE);
            t.play();
            anims.add(t);

            getChildren().add(e);
        }

        /** 四角压暗的暗角，把视线收拢到中间的登录卡片上。 */
        private void buildVignette() {
            Rectangle vignette = new Rectangle();
            vignette.widthProperty().bind(widthProperty());
            vignette.heightProperty().bind(heightProperty());
            vignette.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.78, true, CycleMethod.NO_CYCLE,
                    new Stop(0.40, Color.TRANSPARENT),
                    new Stop(0.78, Color.web("#040a14", 0.30)),
                    new Stop(1.0, Color.web("#01040a", 0.66))));
            vignette.setMouseTransparent(true);
            getChildren().add(vignette);
        }

        /** 缓慢上浮的星尘：一个 Canvas 画完所有粒子。 */
        private void buildParticles() {
            final Canvas canvas = new Canvas();
            canvas.widthProperty().bind(widthProperty());
            canvas.heightProperty().bind(heightProperty());
            canvas.setMouseTransparent(true);
            getChildren().add(canvas);

            final int count = 64;
            final double[] nx = new double[count];
            final double[] ny = new double[count];
            final double[] radius = new double[count];
            final double[] speed = new double[count];
            final double[] phase = new double[count];
            final double[] sway = new double[count];
            for (int i = 0; i < count; i++) {
                // 黄金比 / 无理数序列：取值分散且互不同步，看起来才像随机星图
                nx[i] = frac(i * 0.6180339887);
                ny[i] = frac(i * 0.7548776662);
                radius[i] = 0.8 + frac(i * 0.4142135624) * 1.9;
                speed[i] = 0.012 + frac(i * 0.7320508076) * 0.030;
                phase[i] = frac(i * 0.2360679775) * 2 * Math.PI;
                sway[i] = 0.4 + frac(i * 0.3166247904) * 1.6;
            }

            timer = new AnimationTimer() {
                private long t0;

                @Override
                public void handle(long now) {
                    if (t0 == 0) {
                        t0 = now;
                        return;
                    }
                    double t = (now - t0) / 1e9;
                    double w = canvas.getWidth();
                    double h = canvas.getHeight();
                    if (w <= 0 || h <= 0) {
                        return;
                    }
                    GraphicsContext g = canvas.getGraphicsContext2D();
                    g.clearRect(0, 0, w, h);
                    for (int i = 0; i < count; i++) {
                        // 纵坐标对 1 取小数部分，粒子飘出顶部后自动从底部绕回
                        double y = frac(ny[i] - speed[i] * t) * (h + 40) - 20;
                        double x = (nx[i] + Math.sin(t * sway[i] + phase[i]) * 0.012) * w;
                        // 明暗呼吸，避免整片星星一起闪
                        double a = 0.10 + 0.45 * (0.5 + 0.5 * Math.sin(t * 1.7 + phase[i] * 3));
                        double r = radius[i];

                        g.setFill(Color.web("#8ec5ff"));
                        g.setGlobalAlpha(a * 0.30);
                        g.fillOval(x - r * 2.4, y - r * 2.4, r * 4.8, r * 4.8);
                        g.setFill(Color.WHITE);
                        g.setGlobalAlpha(a);
                        g.fillOval(x - r, y - r, r * 2, r * 2);
                    }
                    g.setGlobalAlpha(1);
                }
            };
            timer.start();
        }

        // ---------- 主界面：浅色柔光 ----------

        /**
         * 主界面不要粒子也不要暗角——内容区全是白卡片和表格，底色只做「有质感但绝不抢戏」。
         * 三片低不透明度的冷色柔光，周期拉长到一分钟以上，几乎察觉不到在动。
         */
        private void buildSoftGlow() {
            blob(0.12, 0.08, 0.52, 0.60, "#a8ccff", 0.85, 90, 40, 63);
            blob(0.92, 0.30, 0.46, 0.52, "#cec0ff", 0.72, -80, 50, 71);
            blob(0.55, 0.98, 0.60, 0.48, "#a9e2ff", 0.78, 70, -40, 57);
        }

        // ---------- 工具 ----------

        /** 中心实、边缘渐隐的柔和光斑；用径向渐变代替高斯模糊（见类注释）。 */
        private static Paint soft(String hex, double alpha) {
            Color c = Color.web(hex);
            return new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                    new Stop(0.00, withAlpha(c, alpha)),
                    new Stop(0.38, withAlpha(c, alpha * 0.46)),
                    new Stop(0.68, withAlpha(c, alpha * 0.15)),
                    new Stop(1.00, Color.TRANSPARENT));
        }

        private static Color withAlpha(Color c, double alpha) {
            return Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(1, alpha)));
        }

        /** 取小数部分（负数也返回 [0,1) 区间）。 */
        private static double frac(double v) {
            return v - Math.floor(v);
        }
    }
}

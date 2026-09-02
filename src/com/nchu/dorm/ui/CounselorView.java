package com.nchu.dorm.ui;

import com.nchu.dorm.model.Counselor;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.application.DormApplication;
import com.nchu.dorm.service.DormApplicationService;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.ui.component.AlertUtil;
import com.nchu.dorm.util.BusinessException;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 辅导员端视图：宿舍审批（待处理 + 历史）。
 * 左侧按范围筛选（待处理/已通过/已驳回/已撤销/全部）列出申请；
 * 右侧面板按申请类型与所处阶段自适应：
 * <ul>
 *   <li>入住/转宿（PENDING）：目标楼栋空床房间下拉 + 通过分配 / 驳回；</li>
 *   <li>退宿（PENDING）：红字风险提示，无房下拉，通过退宿 / 驳回；</li>
 *   <li>转专业换宿（PENDING，本专业辅导员）：同意迁出 / 驳回；</li>
 *   <li>转专业换宿（AWAITING_TARGET，目标辅导员）：跨学院选房接收 / 同学院直接接收 / 驳回；</li>
 *   <li>历史：只读查看，含两级审批痕迹。</li>
 * </ul>
 */
public class CounselorView {

    private final Counselor counselor;
    private final DormApplicationService applicationService = new DormApplicationService();

    private TableView<DormApplication> table;
    private ComboBox<String> filterCombo;
    private VBox rightBox;
    private TextArea commentArea;
    private DormApplication current;

    public CounselorView(Counselor counselor) {
        this.counselor = counselor;
    }

    public Node build() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));

        Label title = new Label("宿舍审批（" + DataCenter.instance().collegeName(counselor.getCollegeCode()) + "）");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().add(title);

        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("待处理", "已通过", "已驳回", "已撤销", "全部");
        filterCombo.getSelectionModel().selectFirst();
        filterCombo.setOnAction(e -> refresh());
        HBox filterRow = new HBox(10, new Label("范围："), filterCombo);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(filterRow);

        // ---- 左侧：申请列表 ----
        table = new TableView<>();
        table.getColumns().add(col("编号", 80, DormApplication::getId));
        table.getColumns().add(col("申请人", 105, a -> studentName(a.getStudentId())));
        table.getColumns().add(col("类型", 100, DormApplication::getTypeName));
        table.getColumns().add(col("目标班级", 120, a -> DormApplication.TYPE_MAJOR_TRANSFER.equals(a.getType())
                ? DormApplicationService.classLabel(a.getTargetClass()) : ""));
        table.getColumns().add(col("目标楼栋", 80, a -> safe(a.getTargetBuilding())));
        table.getColumns().add(col("原宿舍", 100, this::originText));
        table.getColumns().add(col("状态", 80, DormApplication::getStatusName));
        table.getColumns().add(col("提交时间", 135, DormApplication::getCreateTime));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> showDetail(newVal));
        VBox.setVgrow(table, Priority.ALWAYS);

        // ---- 右侧：审批面板 ----
        rightBox = new VBox(10);
        rightBox.setPadding(new Insets(12));
        rightBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dfe6e9; -fx-border-radius: 6;");
        rightBox.setPrefWidth(340);
        rightBox.setFillWidth(true);
        commentArea = new TextArea();
        commentArea.setPromptText("审批意见（可选）");
        commentArea.setPrefRowCount(3);

        HBox content = new HBox(14, table, rightBox);
        HBox.setHgrow(table, Priority.ALWAYS);

        box.getChildren().add(content);
        refresh();
        return wrap(box);
    }

    private void refresh() {
        String scope = filterCombo.getValue();
        List<DormApplication> items = new ArrayList<>();
        if ("待处理".equals(scope)) {
            items = applicationService.findActionableOf(counselor);
        } else {
            items = applicationService.findHistoryOf(counselor);
            List<DormApplication> kept = new ArrayList<>();
            for (DormApplication app : items) {
                if ("全部".equals(scope) || matchesStatusScope(scope, app)) {
                    kept.add(app);
                }
            }
            items = kept;
        }
        table.setItems(FXCollections.observableArrayList(items));
        showDetail(null);
    }

    private boolean matchesStatusScope(String scope, DormApplication app) {
        if ("已通过".equals(scope)) {
            return DormApplication.STATUS_APPROVED.equals(app.getStatus());
        }
        if ("已驳回".equals(scope)) {
            return DormApplication.STATUS_REJECTED.equals(app.getStatus());
        }
        if ("已撤销".equals(scope)) {
            return DormApplication.STATUS_CANCELLED.equals(app.getStatus());
        }
        return true;
    }

    private void showDetail(DormApplication app) {
        this.current = app;
        rightBox.getChildren().clear();
        if (app == null) {
            Label l = new Label("请在左侧选择一条申请。");
            l.setStyle("-fx-text-fill: #95a5a6;");
            rightBox.getChildren().add(l);
            return;
        }

        Label detail = new Label(detailText(app));
        detail.setWrapText(true);
        detail.setStyle("-fx-text-fill: #2c3e50;");
        rightBox.getChildren().add(detail);

        boolean editable = "待处理".equals(filterCombo.getValue());
        if (editable) {
            addActionControls(app);
        } else {
            Label readonly = new Label("（历史记录，仅供查看）");
            readonly.setStyle("-fx-text-fill: #95a5a6;");
            rightBox.getChildren().add(readonly);
        }

        rightBox.getChildren().add(new Label("审批意见："));
        rightBox.getChildren().add(commentArea);
    }

    /** 依据类型与阶段为待处理申请渲染操作控件。 */
    private void addActionControls(DormApplication app) {
        String type = app.getType();
        String status = app.getStatus();

        if (DormApplication.TYPE_MAJOR_TRANSFER.equals(type)) {
            if (DormApplication.STATUS_PENDING.equals(status)) {
                Label l = new Label("作为本专业辅导员：同意后该申请将流转至目标专业辅导员接收。");
                l.setStyle("-fx-text-fill: #e67e22;");
                l.setWrapText(true);
                rightBox.getChildren().add(l);
                rightBox.getChildren().add(buttonRow(button("同意迁出", "#27ae60", e -> doMajorMoveOut(app)),
                        button("驳回申请", "#e74c3c", e -> doReject(app))));
            } else if (DormApplication.STATUS_AWAITING_TARGET.equals(status)) {
                Label l = new Label("本专业辅导员已同意迁出，现由您（目标专业辅导员）接收。");
                l.setStyle("-fx-text-fill: #2c3e50;");
                l.setWrapText(true);
                rightBox.getChildren().add(l);

                String targetBuilding = app.getTargetBuilding();
                ComboBox<String> roomCombo = new ComboBox<>();
                roomCombo.setPrefWidth(280);
                boolean needRoom = targetBuilding != null && !targetBuilding.isEmpty();
                if (needRoom) {
                    for (Room r : DataCenter.instance().findAvailableRooms(targetBuilding)) {
                        roomCombo.getItems().add(roomItem(r));
                    }
                    if (!roomCombo.getItems().isEmpty()) {
                        roomCombo.getSelectionModel().selectFirst();
                    }
                    rightBox.getChildren().add(new Label("接收房间（目标楼栋 " + targetBuilding + "）："));
                    rightBox.getChildren().add(roomCombo);
                } else {
                    Label noMove = new Label("目标学院与当前学院相同，不更换宿舍，仅更新专业/班级档案。");
                    noMove.setWrapText(true);
                    noMove.setStyle("-fx-text-fill: #7f8c8d;");
                    rightBox.getChildren().add(noMove);
                }
                rightBox.getChildren().add(buttonRow(
                        button("通过并接收", "#27ae60", e -> doMajorAccept(app, needRoom ? roomCombo.getValue() : null)),
                        button("驳回申请", "#e74c3c", e -> doReject(app))));
            }
            return;
        }

        if (DormApplication.TYPE_EXIT.equals(type)) {
            Label risk = new Label("通过后将释放该生当前床位并置为未入住。");
            risk.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            risk.setWrapText(true);
            rightBox.getChildren().add(risk);
            rightBox.getChildren().add(buttonRow(button("通过并退宿", "#27ae60", e -> doApproveExit(app)),
                    button("驳回申请", "#e74c3c", e -> doReject(app))));
            return;
        }

        // 入住 / 转宿
        String building = app.getTargetBuilding();
        ComboBox<String> roomCombo = new ComboBox<>();
        roomCombo.setPrefWidth(280);
        if (building != null && !building.isEmpty()) {
            for (Room r : DataCenter.instance().findAvailableRooms(building)) {
                roomCombo.getItems().add(roomItem(r));
            }
            if (!roomCombo.getItems().isEmpty()) {
                roomCombo.getSelectionModel().selectFirst();
            }
        }
        rightBox.getChildren().add(new Label("分配房间（目标楼栋 " + safe(building) + "）："));
        rightBox.getChildren().add(roomCombo);
        rightBox.getChildren().add(buttonRow(button("通过并分配宿舍", "#27ae60", e -> doApprove(app, roomCombo.getValue())),
                button("驳回申请", "#e74c3c", e -> doReject(app))));
    }

    // ---------- 操作 ----------

    private void doApprove(DormApplication app, String selection) {
        try {
            String[] loc = parseRoom(selection);
            applicationService.approve(app, counselor, loc[0], loc[1], commentArea.getText());
            AlertUtil.info("已通过并分配宿舍：" + loc[0] + "-" + loc[1]);
            commentArea.clear();
            refresh();
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    private void doApproveExit(DormApplication app) {
        try {
            applicationService.approveExit(app, counselor, commentArea.getText());
            AlertUtil.info("已通过退宿，该生床位已释放。");
            commentArea.clear();
            refresh();
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    private void doMajorMoveOut(DormApplication app) {
        try {
            applicationService.approveMajorMoveOut(app, counselor, commentArea.getText());
            AlertUtil.info("已同意迁出，申请已流转至目标专业辅导员。");
            commentArea.clear();
            refresh();
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    private void doMajorAccept(DormApplication app, String selection) {
        try {
            if (selection == null) {
                applicationService.approveMajorAccept(app, counselor, null, null, commentArea.getText());
            } else {
                String[] loc = parseRoom(selection);
                applicationService.approveMajorAccept(app, counselor, loc[0], loc[1], commentArea.getText());
            }
            AlertUtil.info("已通过接收，转专业换宿完成。");
            commentArea.clear();
            refresh();
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    private void doReject(DormApplication app) {
        try {
            applicationService.reject(app, counselor, commentArea.getText());
            AlertUtil.info("已驳回该申请。");
            commentArea.clear();
            refresh();
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    // ---------- 展示辅助 ----------

    private String detailText(DormApplication app) {
        StringBuilder sb = new StringBuilder();
        sb.append("编号：").append(app.getId()).append("\n");
        Student s = DataCenter.instance().findStudentById(app.getStudentId());
        sb.append("申请人：").append(s == null ? app.getStudentId() : s.getName() + "（" + s.getId() + "）").append("\n");
        sb.append("类型：").append(app.getTypeName()).append("\n");
        sb.append("状态：").append(app.getStatusName()).append("\n");
        if (DormApplication.TYPE_MAJOR_TRANSFER.equals(app.getType())) {
            sb.append("目标班级：").append(DormApplicationService.classLabel(app.getTargetClass())).append("\n");
            if (app.getOriginClass() != null) {
                sb.append("原班级：").append(DormApplicationService.classLabel(app.getOriginClass())).append("\n");
            }
        }
        if (app.getTargetBuilding() != null && !app.getTargetBuilding().isEmpty()) {
            sb.append("目标楼栋：").append(app.getTargetBuilding());
            if (app.getTargetRoom() != null && !app.getTargetRoom().isEmpty()) {
                sb.append(" - ").append(app.getTargetRoom());
            }
            sb.append("\n");
        }
        if (originText(app) != null && !originText(app).isEmpty()) {
            sb.append("原宿舍：").append(originText(app)).append("\n");
        }
        sb.append("申请原因：").append(app.getReason() == null ? "" : app.getReason()).append("\n");
        sb.append("提交时间：").append(app.getCreateTime()).append("\n");
        if (app.getStep1ReviewerId() != null) {
            sb.append("▸ 同意迁出：").append(reviewerText(app.getStep1ReviewerId())).append("  ")
                    .append(safe(app.getStep1ReviewTime())).append("\n");
            if (app.getStep1ReviewComment() != null && !app.getStep1ReviewComment().isEmpty()) {
                sb.append("  意见：").append(app.getStep1ReviewComment()).append("\n");
            }
        }
        if (app.getReviewerId() != null) {
            sb.append("▸ 处理人：").append(reviewerText(app.getReviewerId())).append("  ")
                    .append(safe(app.getReviewTime())).append("\n");
            if (app.getReviewComment() != null && !app.getReviewComment().isEmpty()) {
                sb.append("  意见：").append(app.getReviewComment()).append("\n");
            }
        }
        return sb.toString();
    }

    private String reviewerText(String id) {
        Student s = DataCenter.instance().findStudentById(id);
        if (s != null) {
            return s.getName();
        }
        Counselor c = DataCenter.instance().findCounselorById(id);
        return c == null ? id : "辅导员 " + id;
    }

    private String originText(DormApplication app) {
        if (app.getOriginBuilding() == null && app.getOriginRoom() == null) {
            return "";
        }
        return safe(app.getOriginBuilding()) + "-" + safe(app.getOriginRoom());
    }

    private String roomItem(Room r) {
        return r.displayKey() + "（余" + r.availableBedCount() + "床）";
    }

    /** 从房间下拉条目解析出 [楼栋, 房号]；条目形如 "20B栋-205（余3床）"。 */
    private String[] parseRoom(String item) {
        if (item == null || item.isEmpty()) {
            throw new BusinessException("请选择分配房间");
        }
        int dash = item.indexOf('-');
        if (dash < 0) {
            throw new BusinessException("房间格式异常");
        }
        String building = item.substring(0, dash);
        String tail = item.substring(dash + 1);
        int open = tail.indexOf('（');
        String roomNo = open >= 0 ? tail.substring(0, open) : tail;
        return new String[]{building, roomNo};
    }

    private String studentName(String studentId) {
        Student s = DataCenter.instance().findStudentById(studentId);
        return s == null ? studentId : s.getName();
    }

    // ---------- 工具 ----------

    private HBox buttonRow(Node... nodes) {
        HBox row = new HBox(10, nodes);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button button(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white;");
        b.setOnAction(handler);
        return b;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private <T> TableColumn<T, String> col(String header, double width, Function<T, String> mapper) {
        TableColumn<T, String> c = new TableColumn<>(header);
        c.setPrefWidth(width);
        c.setCellValueFactory(data -> new ReadOnlyStringWrapper(mapper.apply(data.getValue())));
        return c;
    }

    private Node wrap(VBox box) {
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #f8f9fa;");
        return sp;
    }
}

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

import java.util.List;
import java.util.function.Function;

/**
 * 辅导员端视图：审批本学院学生的宿舍入住申请，并分配房间床位。
 */
public class CounselorView {

    private final Counselor counselor;
    private final DormApplicationService applicationService = new DormApplicationService();

    private TableView<DormApplication> table;
    private Label detailLabel;
    private Label buildingLabel;
    private ComboBox<String> roomCombo;
    private TextArea commentArea;
    private DormApplication current;

    public CounselorView(Counselor counselor) {
        this.counselor = counselor;
    }

    public Node build() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));

        Label title = new Label("待审批申请（" + DataCenter.instance().collegeName(counselor.getCollegeCode()) + "）");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().add(title);

        // ---- 左侧：申请列表 ----
        table = new TableView<>();
        table.getColumns().add(col("编号", 90, DormApplication::getId));
        table.getColumns().add(col("申请人", 110, a -> studentName(a.getStudentId())));
        table.getColumns().add(col("类型", 90, DormApplication::getTypeName));
        table.getColumns().add(col("目标楼栋", 90, DormApplication::getTargetBuilding));
        table.getColumns().add(col("申请原因", 220, a -> a.getReason() == null ? "" : a.getReason()));
        table.getColumns().add(col("提交时间", 150, DormApplication::getCreateTime));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> onSelect(newVal));
        refresh();
        VBox.setVgrow(table, Priority.ALWAYS);

        // ---- 右侧：审批面板 ----
        VBox reviewPanel = new VBox(10);
        reviewPanel.setPadding(new Insets(12));
        reviewPanel.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dfe6e9; -fx-border-radius: 6;");

        detailLabel = new Label("请在左侧选择一条申请。");
        detailLabel.setWrapText(true);
        buildingLabel = new Label("目标楼栋：");

        roomCombo = new ComboBox<>();
        roomCombo.setPrefWidth(240);
        roomCombo.setPromptText("请选择分配房间");

        commentArea = new TextArea();
        commentArea.setPromptText("审批意见（可选）");
        commentArea.setPrefRowCount(3);

        Button approveButton = new Button("通过并分配宿舍");
        approveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        approveButton.setOnAction(e -> doApprove());

        Button rejectButton = new Button("驳回申请");
        rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        rejectButton.setOnAction(e -> doReject());

        HBox buttons = new HBox(10, approveButton, rejectButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        reviewPanel.getChildren().addAll(detailLabel, buildingLabel, roomCombo,
                new Label("审批意见："), commentArea, buttons);
        reviewPanel.setPrefWidth(300);

        HBox content = new HBox(14, table, reviewPanel);
        HBox.setHgrow(table, Priority.ALWAYS);

        box.getChildren().add(content);
        return wrap(box);
    }

    private void refresh() {
        table.setItems(FXCollections.observableArrayList(
                applicationService.findPendingOfCollege(counselor.getCollegeCode())));
    }

    private void onSelect(DormApplication app) {
        this.current = app;
        roomCombo.getItems().clear();
        if (app == null) {
            detailLabel.setText("请在左侧选择一条申请。");
            buildingLabel.setText("目标楼栋：");
            return;
        }
        Student s = DataCenter.instance().findStudentById(app.getStudentId());
        String applicant = s == null ? app.getStudentId() : s.getName() + "（" + s.getId() + "）";
        detailLabel.setText("申请人：" + applicant + "\n类型：" + app.getTypeName() + "\n原因："
                + (app.getReason() == null ? "" : app.getReason()));
        buildingLabel.setText("目标楼栋：" + app.getTargetBuilding());

        List<Room> available = DataCenter.instance().findAvailableRooms(app.getTargetBuilding());
        for (Room r : available) {
            roomCombo.getItems().add(r.displayKey() + "（余" + r.availableBedCount() + "床）");
        }
        if (!roomCombo.getItems().isEmpty()) {
            roomCombo.getSelectionModel().selectFirst();
        }
    }

    private void doApprove() {
        if (current == null) {
            AlertUtil.warn("请先选择一条申请");
            return;
        }
        String selection = roomCombo.getValue();
        if (selection == null) {
            AlertUtil.warn("该楼栋暂无空床房间，无法分配");
            return;
        }
        int idx = selection.indexOf('-');
        String building = selection.substring(0, idx);
        String roomNo = selection.substring(idx + 1, selection.indexOf('（'));
        try {
            applicationService.approve(current, counselor, building, roomNo, commentArea.getText());
            AlertUtil.info("已通过并分配宿舍：" + building + "-" + roomNo);
            commentArea.clear();
            refresh();
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    private void doReject() {
        if (current == null) {
            AlertUtil.warn("请先选择一条申请");
            return;
        }
        try {
            applicationService.reject(current, counselor, commentArea.getText());
            AlertUtil.info("已驳回该申请");
            commentArea.clear();
            refresh();
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    private String studentName(String studentId) {
        Student s = DataCenter.instance().findStudentById(studentId);
        return s == null ? studentId : s.getName();
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

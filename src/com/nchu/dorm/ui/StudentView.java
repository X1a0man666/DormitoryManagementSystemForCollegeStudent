package com.nchu.dorm.ui;

import com.nchu.dorm.model.Bed;
import com.nchu.dorm.model.College;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.StudentId;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

/**
 * 学生端视图：我的宿舍 / 宿舍申请 / 我的申请。
 */
public class StudentView {

    private final Student student;
    private final DormApplicationService applicationService = new DormApplicationService();

    public StudentView(Student student) {
        this.student = student;
    }

    // ---------- 我的宿舍 ----------

    public Node buildMyDorm() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setMaxWidth(720);

        Label title = new Label("我的宿舍");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().add(title);

        GridPane info = new GridPane();
        info.setHgap(20);
        info.setVgap(8);
        info.addRow(0, label("姓名："), value(student.getName()));
        info.addRow(1, label("学号："), value(student.getId()));

        // 学号编码的是入学时信息（学号一经生成不变；转专业后当前学院/专业可与学号不同）
        StudentId sid = StudentId.parseOrNull(student.getId());
        if (sid != null) {
            info.addRow(2, label("入学年份："), value(sid.getFullAdmissionYear() + " 年"));
            info.addRow(3, label("入学学院："), value(DataCenter.instance().collegeName(sid.getCollegeCode())
                    + "（代码 " + sid.getCollegeCode() + "）"));
            info.addRow(4, label("专业代码："), value(String.valueOf(sid.getMajorCode())));
            info.addRow(5, label("班级代码："), value(String.valueOf(sid.getClassCode())));
            info.addRow(6, label("学号序号："), value(String.format("%02d", sid.getStudentNo())));
        } else {
            info.addRow(2, label("学号格式："), value("非标准 8 位学号"));
        }

        info.addRow(7, label("当前学院："), value(DataCenter.instance().collegeName(student.getCollegeCode())));
        info.addRow(8, label("当前专业："), value(student.getMajor()));
        info.addRow(9, label("当前班级："), value(student.getClassName()));
        box.getChildren().add(info);

        if (student.isCheckedIn()) {
            Label dormLabel = new Label("当前入住：" + student.getCurrentBuilding() + " - " + student.getCurrentRoom());
            dormLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            box.getChildren().add(dormLabel);

            Room room = DataCenter.instance().findRoom(student.getCurrentBuilding(), student.getCurrentRoom());
            if (room != null) {
                box.getChildren().add(new Label("室友信息："));
                for (Bed bed : room.getBeds()) {
                    if (!bed.isEmpty()) {
                        Student occupant = DataCenter.instance().findStudentById(bed.getOccupantId());
                        String text = "床位" + bed.getBedNo() + "：" + (occupant == null
                                ? bed.getOccupantId()
                                : occupant.getName() + "（" + occupant.getId() + "）");
                        box.getChildren().add(new Label(text));
                    }
                }
            }
        } else {
            box.getChildren().add(new Label("尚未入住宿舍，请到【宿舍申请】提交入住申请。"));
        }
        return wrap(box);
    }

    // ---------- 宿舍申请 ----------

    public Node buildApply() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setMaxWidth(720);

        Label title = new Label("宿舍申请");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().add(title);

        if (student.isCheckedIn()) {
            box.getChildren().add(new Label("您已入住宿舍，如需调整请使用【转移宿舍】功能（后续版本开放）。"));
            return wrap(box);
        }

        DataCenter dc = DataCenter.instance();
        College college = dc.findCollegeByCode(student.getCollegeCode());
        List<String> buildingNames = college == null ? java.util.Collections.<String>emptyList() : college.getBuildingNames();
        if (buildingNames.isEmpty()) {
            box.getChildren().add(new Label("本学院暂未分配宿舍楼栋，请联系辅导员或宿管科。"));
            return wrap(box);
        }

        ComboBox<String> buildingCombo = new ComboBox<>();
        buildingCombo.getItems().addAll(buildingNames);
        buildingCombo.getSelectionModel().selectFirst();

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("请填写申请原因（必填）");
        reasonArea.setPrefRowCount(4);

        Button submitButton = new Button("提交申请");
        submitButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14;");
        submitButton.setOnAction(e -> {
            try {
                applicationService.submitApply(student, buildingCombo.getValue(), reasonArea.getText());
                AlertUtil.info("申请提交成功，请等待辅导员审批。");
                reasonArea.clear();
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        HBox buttonRow = new HBox(10, submitButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(
                new Label("目标楼栋（本学院分配）："),
                buildingCombo,
                new Label("申请原因："),
                reasonArea,
                buttonRow);
        return wrap(box);
    }

    // ---------- 我的申请 ----------

    public Node buildApplications() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));

        Label title = new Label("我的申请");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().add(title);

        TableView<DormApplication> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(
                applicationService.findApplicationsOfStudent(student.getId())));
        table.getColumns().add(col("申请编号", 110, DormApplication::getId));
        table.getColumns().add(col("类型", 90, DormApplication::getTypeName));
        table.getColumns().add(col("目标楼栋", 100, DormApplication::getTargetBuilding));
        table.getColumns().add(col("分配房间", 100, a -> a.getTargetRoom() == null ? "" : a.getTargetRoom()));
        table.getColumns().add(col("状态", 90, DormApplication::getStatusName));
        table.getColumns().add(col("提交时间", 150, DormApplication::getCreateTime));
        table.getColumns().add(col("审批意见", 200, a -> a.getReviewComment() == null ? "" : a.getReviewComment()));
        table.setPrefHeight(420);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        box.getChildren().add(table);
        return wrap(box);
    }

    // ---------- 工具 ----------

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #7f8c8d;");
        return l;
    }

    private Label value(String text) {
        return new Label(text == null ? "" : text);
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

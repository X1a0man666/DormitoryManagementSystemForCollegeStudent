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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 学生端视图：我的宿舍 / 宿舍申请 / 我的申请。
 * 宿舍申请支持入住、转宿、退宿、转专业换宿四类（按居住状态展示可用类型）。
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
            info.addRow(3, label("学号序号："), value(String.format("%02d", sid.getStudentNo())));
        } else {
            info.addRow(2, label("学号格式："), value("非标准 8 位学号"));
        }

        info.addRow(4, label("当前学院："), value(DataCenter.instance().collegeName(student.getCollegeCode())));
        info.addRow(5, label("当前专业："), value(student.getMajor()));
        info.addRow(6, label("当前班级："), value(student.getClassName()));
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

    /**
     * 宿舍申请页：按居住状态给出可用申请类型（入住 / 转宿 / 退宿 / 转专业换宿），切换类型重排表单。
     */
    public Node buildApply() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setMaxWidth(720);

        Label title = new Label("宿舍申请");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        final VBox page = new VBox(12);
        box.getChildren().addAll(title, page);
        renderApply(page);
        return wrap(box);
    }

    /** 整体重绘宿舍申请页（提交后刷新状态/在办提示）。 */
    private void renderApply(final VBox page) {
        page.getChildren().clear();

        Label statusLabel;
        if (student.isCheckedIn()) {
            statusLabel = new Label("当前状态：已入住 " + student.getCurrentBuilding() + " - " + student.getCurrentRoom());
            statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            statusLabel = new Label("当前状态：未入住（可提交入住申请）");
            statusLabel.setStyle("-fx-text-fill: #7f8c8d;");
        }
        page.getChildren().add(statusLabel);

        DormApplication inflight = findInflight();
        if (inflight != null) {
            Label warn = new Label("您有 1 条待审批申请（编号 " + inflight.getId() + "，类型："
                    + inflight.getTypeName() + "）。请等待处理或先撤销后再提交。");
            warn.setStyle("-fx-text-fill: #d35400; -fx-font-weight: bold;");
            warn.setWrapText(true);
            page.getChildren().add(warn);
        }

        List<String> available = availableTypeDisplays();
        page.getChildren().add(new Label("申请类型："));
        final ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(available);
        typeCombo.setPrefWidth(220);

        final VBox form = new VBox(12);
        page.getChildren().addAll(typeCombo, form);

        final boolean inFlight = inflight != null;
        Runnable refresh = () -> renderApply(page);
        Runnable repopulate = () -> populateApplyForm(form, keyOfDisplay(typeCombo.getValue()), inFlight, refresh);
        typeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> repopulate.run());
        typeCombo.getSelectionModel().selectFirst();
        repopulate.run();
    }

    /** 依据所选类型填充申请表单。 */
    private void populateApplyForm(final VBox form, String typeKey, boolean inFlight, Runnable refresh) {
        form.getChildren().clear();
        if (typeKey == null) {
            return;
        }
        List<Node> nodes = new ArrayList<>();
        String typeName = typeDisplay(typeKey);

        if (DormApplication.TYPE_MAJOR_TRANSFER.equals(typeKey)) {
            nodes.add(new Label("现居宿舍：" + currentDormText()));

            ComboBox<String> classCombo = new ComboBox<>();
            classCombo.setEditable(true);
            classCombo.setPrefWidth(460);
            classCombo.setPromptText("请选择转专业后的班级（可跨学院/跨届）");
            for (String item : targetClassOptions()) {
                classCombo.getItems().add(item);
            }
            Label classInfo = new Label();
            classInfo.setWrapText(true);
            classInfo.setStyle("-fx-text-fill: #7f8c8d;");
            classCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                String code = codeOfClassOption(newVal);
                if (DormApplicationService.isRealClass(code)) {
                    classInfo.setText("目标：学院" + DormApplicationService.collegeOfClass(code)
                            + " · " + DormApplicationService.classLabel(code));
                } else {
                    classInfo.setText("");
                }
            });

            Label flow = new Label("流程说明：需先经本专业辅导员同意迁出，再由目标专业辅导员同意接收；"
                    + "任一级拒绝即失败。跨学院转专业才需更换宿舍楼（目标学院同性别楼）。");
            flow.setWrapText(true);
            flow.setStyle("-fx-text-fill: #e67e22;");

            TextArea reason = reasonArea();
            Button submit = submitButton("提交转专业换宿申请", () -> {
                applicationService.submitMajorTransfer(student, codeOfClassOption(classCombo.getValue()), reason.getText());
                AlertUtil.info("转专业换宿申请已提交，等待本专业辅导员审批。");
                reason.clear();
                refresh.run();
            }, inFlight);
            nodes.add(new Label("转专业后的班级："));
            nodes.add(classCombo);
            nodes.add(classInfo);
            nodes.add(flow);
            nodes.add(new Label("申请原因："));
            nodes.add(reason);
            nodes.add(submit);
        } else if (DormApplication.TYPE_EXIT.equals(typeKey)) {
            nodes.add(new Label("现居宿舍：" + currentDormText()));
            Label risk = new Label("提示：通过后将释放当前床位并将您置为未入住。");
            risk.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            TextArea reason = reasonArea();
            Button submit = submitButton("提交退宿申请", () -> {
                applicationService.submitExit(student, reason.getText());
                AlertUtil.info("退宿申请已提交，等待辅导员审批。");
                reason.clear();
                refresh.run();
            }, inFlight);
            nodes.add(risk);
            nodes.add(new Label("申请原因："));
            nodes.add(reason);
            nodes.add(submit);
        } else {
            // 入住 / 转宿：目标楼栋 + 原因
            if (DormApplication.TYPE_TRANSFER.equals(typeKey)) {
                nodes.add(new Label("现居宿舍：" + currentDormText()));
            }
            List<String> buildings = genderBuildings();
            ComboBox<String> buildingCombo = new ComboBox<>();
            buildingCombo.setPrefWidth(240);
            buildingCombo.getItems().addAll(buildings);
            if (!buildings.isEmpty()) {
                buildingCombo.getSelectionModel().selectFirst();
            }
            Label spareLabel = new Label();
            spareLabel.setStyle("-fx-text-fill: #7f8c8d;");
            Runnable updateSpare = () -> {
                String b = buildingCombo.getValue();
                spareLabel.setText(b == null ? "" : "该楼栋当前空床房间数："
                        + DataCenter.instance().findAvailableRooms(b).size());
            };
            buildingCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateSpare.run());
            updateSpare.run();

            TextArea reason = reasonArea();
            String labelText = DormApplication.TYPE_APPLY.equals(typeKey)
                    ? "提交入住申请" : "提交转宿申请";
            Button submit = submitButton(labelText, () -> {
                if (DormApplication.TYPE_APPLY.equals(typeKey)) {
                    applicationService.submitApply(student, buildingCombo.getValue(), reason.getText());
                } else {
                    applicationService.submitTransfer(student, buildingCombo.getValue(), reason.getText());
                }
                AlertUtil.info(typeName + "已提交，等待辅导员审批。");
                reason.clear();
                refresh.run();
            }, inFlight);
            nodes.add(new Label("目标楼栋（本学院 · 匹配性别）："));
            nodes.add(buildingCombo);
            nodes.add(spareLabel);
            nodes.add(new Label("申请原因："));
            nodes.add(reason);
            nodes.add(submit);
        }
        form.getChildren().addAll(nodes);
    }

    private String currentDormText() {
        return student.isCheckedIn()
                ? student.getCurrentBuilding() + " - " + student.getCurrentRoom()
                : "未入住";
    }

    private TextArea reasonArea() {
        TextArea reason = new TextArea();
        reason.setPromptText("请填写申请原因（必填）");
        reason.setPrefRowCount(4);
        return reason;
    }

    private Button submitButton(String text, Runnable action, boolean disabled) {
        Button submit = new Button(text);
        submit.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14;");
        submit.setDisable(disabled);
        submit.setOnAction(e -> {
            try {
                action.run();
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });
        return submit;
    }

    private List<String> availableTypeDisplays() {
        List<String> list = new ArrayList<>();
        if (student.isCheckedIn()) {
            list.add("转宿申请");
            list.add("退宿申请");
            list.add("转专业换宿");
        } else {
            list.add("入住申请");
        }
        return list;
    }

    private DormApplication findInflight() {
        for (DormApplication app : applicationService.findApplicationsOfStudent(student.getId())) {
            if (app.isPendingLike()) {
                return app;
            }
        }
        return null;
    }

    private List<String> genderBuildings() {
        DataCenter dc = DataCenter.instance();
        College college = dc.findCollegeByCode(student.getCollegeCode());
        List<String> result = new ArrayList<>();
        if (college != null) {
            boolean male = "男".equals(student.getGender());
            for (String name : college.getBuildingNames()) {
                if (male == name.endsWith("A栋")) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    /** 转专业目标班级下拉候选：全校真实班级编码 - 本班，展示 "编码 ｜ 学院·专业·级·班"。 */
    private List<String> targetClassOptions() {
        Set<String> options = new LinkedHashSet<>();
        for (Student s : DataCenter.instance().getStudents()) {
            String cls = s.getClassName();
            if (cls == null || cls.length() < 6) {
                continue;
            }
            String code = cls.substring(0, 6);
            if (code.equals(student.getClassName())) {
                continue;
            }
            options.add(code + " ｜ " + DormApplicationService.classLabel(code));
        }
        return new ArrayList<>(options);
    }

    private String codeOfClassOption(String selection) {
        if (selection == null) {
            return null;
        }
        String trimmed = selection.trim();
        int idx = trimmed.indexOf(" ｜");
        return idx >= 0 ? trimmed.substring(0, idx) : trimmed;
    }

    // ---------- 我的申请 ----------

    public Node buildApplications() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));

        Label title = new Label("我的申请");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("全部", "待审批", "已通过", "已驳回", "已撤销");
        filterCombo.getSelectionModel().selectFirst();

        final TableView<DormApplication> table = new TableView<>();
        table.getColumns().add(col("申请编号", 110, DormApplication::getId));
        table.getColumns().add(col("类型", 100, DormApplication::getTypeName));
        table.getColumns().add(col("目标楼栋", 100, a -> safe(a.getTargetBuilding())));
        table.getColumns().add(col("目标班级", 150, a -> DormApplication.TYPE_MAJOR_TRANSFER.equals(a.getType())
                ? DormApplicationService.classLabel(a.getTargetClass()) : ""));
        table.getColumns().add(col("原宿舍", 130, a -> originText(a)));
        table.getColumns().add(col("分配房间", 90, a -> safe(a.getTargetRoom())));
        table.getColumns().add(col("状态", 90, DormApplication::getStatusName));
        table.getColumns().add(col("提交时间", 150, DormApplication::getCreateTime));
        table.getColumns().add(col("审批意见", 180, a -> a.getReviewComment() == null ? "" : a.getReviewComment()));
        table.setPrefHeight(360);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Runnable refreshTable = () -> {
            List<DormApplication> all = applicationService.findApplicationsOfStudent(student.getId());
            List<DormApplication> shown = new ArrayList<>();
            String filter = filterCombo.getValue();
            for (DormApplication app : all) {
                if (statusMatches(filter, app)) {
                    shown.add(app);
                }
            }
            table.setItems(FXCollections.observableArrayList(shown));
        };

        filterCombo.setOnAction(e -> refreshTable.run());

        Button cancelButton = new Button("撤销选中申请");
        cancelButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        cancelButton.setOnAction(e -> {
            DormApplication selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.warn("请先在表格中选择一条申请");
                return;
            }
            if (!selected.isPendingLike()) {
                AlertUtil.warn("仅待审批（含待目标专业审批）的申请可撤销");
                return;
            }
            if (AlertUtil.confirm("确定撤销申请 " + selected.getId() + "（类型：" + selected.getTypeName() + "）吗？")) {
                try {
                    applicationService.cancel(student, selected);
                    AlertUtil.info("已撤销该申请。");
                    refreshTable.run();
                } catch (BusinessException ex) {
                    AlertUtil.error(ex.getMessage());
                }
            }
        });

        Label hint = new Label("提示：待审批状态可选中后撤销；已通过/驳回/撤销的记录仅供查看。");
        hint.setStyle("-fx-text-fill: #95a5a6;");

        HBox filterRow = new HBox(10, new Label("状态筛选："), filterCombo);
        HBox actionRow = new HBox(10, cancelButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(title, filterRow, table, actionRow, hint);
        refreshTable.run();
        return wrap(box);
    }

    private boolean statusMatches(String filter, DormApplication app) {
        if ("待审批".equals(filter)) {
            return app.isPendingLike();
        }
        if ("已通过".equals(filter)) {
            return DormApplication.STATUS_APPROVED.equals(app.getStatus());
        }
        if ("已驳回".equals(filter)) {
            return DormApplication.STATUS_REJECTED.equals(app.getStatus());
        }
        if ("已撤销".equals(filter)) {
            return DormApplication.STATUS_CANCELLED.equals(app.getStatus());
        }
        return true; // 全部
    }

    private String originText(DormApplication app) {
        if (app.getOriginBuilding() == null && app.getOriginRoom() == null) {
            return "";
        }
        return safe(app.getOriginBuilding()) + "-" + safe(app.getOriginRoom());
    }

    // ---------- 工具 ----------

    private String keyOfDisplay(String display) {
        if ("入住申请".equals(display)) {
            return DormApplication.TYPE_APPLY;
        }
        if ("转宿申请".equals(display)) {
            return DormApplication.TYPE_TRANSFER;
        }
        if ("退宿申请".equals(display)) {
            return DormApplication.TYPE_EXIT;
        }
        return DormApplication.TYPE_MAJOR_TRANSFER;
    }

    private String typeDisplay(String typeKey) {
        if (DormApplication.TYPE_APPLY.equals(typeKey)) {
            return "入住申请";
        }
        if (DormApplication.TYPE_TRANSFER.equals(typeKey)) {
            return "转宿申请";
        }
        if (DormApplication.TYPE_EXIT.equals(typeKey)) {
            return "退宿申请";
        }
        return "转专业换宿";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

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

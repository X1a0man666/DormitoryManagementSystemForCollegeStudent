package com.nchu.dorm.ui;

import com.nchu.dorm.model.Bed;
import com.nchu.dorm.model.Building;
import com.nchu.dorm.model.College;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Person;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.StudentId;
import com.nchu.dorm.model.announcement.Announcement;
import com.nchu.dorm.model.application.DormApplication;
import com.nchu.dorm.model.application.ElectricityPurchase;
import com.nchu.dorm.model.application.RepairTicket;
import com.nchu.dorm.model.record.HygieneRecord;
import com.nchu.dorm.model.record.NightReturnRecord;
import com.nchu.dorm.model.record.ValuablesRecord;
import com.nchu.dorm.service.DormApplicationService;
import com.nchu.dorm.service.ElectricityService;
import com.nchu.dorm.service.RepairService;
import com.nchu.dorm.service.StudentLifeService;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.ui.component.AlertUtil;
import com.nchu.dorm.ui.component.UI;
import com.nchu.dorm.util.BusinessException;
import com.nchu.dorm.util.FormatUtil;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * 学生端视图：我的宿舍 / 宿舍申请 / 购电 / 维修报修 / 夜归确认 / 卫生检查 /
 * 贵重物品确认 / 公告 / 设置。
 * 宿舍申请页把"提交申请"与"我的申请记录"合为一页（同属更换/调整宿舍一件事），
 * 支持入住、转宿、退宿、转专业换宿四类（按居住状态展示可用类型），并在页内追踪进度、撤销在办申请；
 * 购电、维修报修为"学生提交 → 宿管科处理"的业务闭环（迭代五）；
 * 夜归确认、卫生检查、贵重物品确认为"宿管登记 → 学生确认/反馈"的对应闭环（迭代六），
 * 其中贵重物品报遗失后经宿管上报宿管科、由宿管科发布全校公告（见 {@link StudentLifeService}）；
 * 设置为学生自助页：更换头像、修改本人姓名（默认与学号一致）与登录密码，页面本身由 {@link SettingsView} 提供。
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
        UI.style(title, UI.PAGE_TITLE);
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
            UI.style(dormLabel, UI.OK);
            dormLabel.setStyle("-fx-font-size: 14px;");
            box.getChildren().add(dormLabel);

            Room room = DataCenter.instance().findRoom(student.getCurrentBuilding(), student.getCurrentRoom());
            if (room != null) {
                box.getChildren().add(new Label("房间电表剩余电量："
                        + FormatUtil.num(room.getElectricityBalance()) + " 度（如需购电请到【购电】页）"));
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

    // ---------- 宿舍申请（提交 + 我的申请记录） ----------

    /**
     * 宿舍申请页：既提交申请也追踪进度，二者同属"更换/调整宿舍"一件事，故合并为一页——
     * 上半区按居住状态给出可用申请类型（入住 / 转宿 / 退宿 / 转专业换宿），切换类型重排表单；
     * 下半区为「我的申请记录」（状态筛选、原宿舍/目标班级、待审批项撤销）。
     */
    public Node buildApply() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setMaxWidth(1100);

        Label title = new Label("宿舍申请");
        UI.style(title, UI.PAGE_TITLE);

        // 表单区（提交后整体重绘）
        final VBox formArea = new VBox(12);

        // 申请记录：状态筛选 + 表格 + 撤销
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
        table.setPrefHeight(280);
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
        UI.style(cancelButton, UI.BTN, UI.BTN_WARNING);
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
                    // 撤销后恢复为"无在办申请"，表单区需一并重绘以解除提交禁用
                    renderApply(formArea, refreshTable);
                    refreshTable.run();
                } catch (BusinessException ex) {
                    AlertUtil.error(ex.getMessage());
                }
            }
        });

        Label hint = new Label("提示：待审批状态可选中后撤销；已通过/驳回/撤销的记录仅供查看。");
        UI.style(hint, UI.FAINT);

        HBox filterRow = new HBox(10, new Label("状态筛选："), filterCombo);
        HBox actionRow = new HBox(10, cancelButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Label submitTitle = new Label("提交申请");
        UI.style(submitTitle, UI.SECTION);
        Label recordsTitle = new Label("我的申请记录");
        UI.style(recordsTitle, UI.SECTION);

        box.getChildren().addAll(title, submitTitle, formArea, recordsTitle, filterRow, table, actionRow, hint);
        renderApply(formArea, refreshTable);
        refreshTable.run();
        return wrap(box);
    }

    /** 重绘申请表单区（提交后刷新状态/在办提示），并同步刷新下方申请记录表。 */
    private void renderApply(final VBox formArea, final Runnable refreshTable) {
        formArea.getChildren().clear();

        Label statusLabel;
        if (student.isCheckedIn()) {
            statusLabel = new Label("当前状态：已入住 " + student.getCurrentBuilding() + " - " + student.getCurrentRoom());
            UI.style(statusLabel, UI.OK);
        } else {
            statusLabel = new Label("当前状态：未入住（可提交入住申请）");
            UI.style(statusLabel, UI.MUTED);
        }
        formArea.getChildren().add(statusLabel);

        DormApplication inflight = findInflight();
        if (inflight != null) {
            Label warn = new Label("您有 1 条待审批申请（编号 " + inflight.getId() + "，类型："
                    + inflight.getTypeName() + "）。请等待处理或先撤销后再提交。");
            UI.style(warn, UI.WARN_DEEP);
            warn.setWrapText(true);
            formArea.getChildren().add(warn);
        }

        List<String> available = availableTypeDisplays();
        formArea.getChildren().add(new Label("申请类型："));
        final ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(available);
        typeCombo.setPrefWidth(220);

        final VBox form = new VBox(12);
        formArea.getChildren().addAll(typeCombo, form);

        final boolean inFlight = inflight != null;
        // 提交成功后：重绘表单（状态/在办提示）并刷新下方的申请记录表
        Runnable refresh = () -> {
            renderApply(formArea, refreshTable);
            refreshTable.run();
        };
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
            UI.style(classInfo, UI.MUTED);
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
            UI.style(flow, UI.WARN);

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
            UI.style(risk, UI.DANGER);
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
            UI.style(spareLabel, UI.MUTED);
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
        UI.style(submit, UI.BTN, UI.BTN_PRIMARY);
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
                Building b = dc.findBuilding(name);
                boolean maleBuilding = b != null ? b.isMale() : name.endsWith("A栋");
                if (male == maleBuilding) {
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

    // ---------- 申请记录辅助 ----------

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

    // ---------- 购电 ----------

    /** 购电页：为本人当前房间购电，宿管科售电后到账。 */
    public Node buildElectricity() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("购电（电费充值）");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        final VBox body = new VBox(12);
        box.getChildren().add(body);
        renderElectricity(body);
        return wrap(box);
    }

    private void renderElectricity(final VBox body) {
        body.getChildren().clear();
        final ElectricityService service = new ElectricityService();

        if (!student.isCheckedIn()) {
            Label note = new Label("您尚未入住宿舍，无法购电。请先在【宿舍申请】办理入住。");
            UI.style(note, UI.DANGER);
            body.getChildren().add(note);
            return;
        }

        DataCenter dc = DataCenter.instance();
        Room room = dc.findRoom(student.getCurrentBuilding(), student.getCurrentRoom());
        Label status = new Label("购电房间：" + student.getCurrentBuilding() + " - " + student.getCurrentRoom()
                + "　　· 房间剩余电量：" + FormatUtil.num(room == null ? 0 : room.getElectricityBalance()) + " 度"
                + "　　· 单价：" + FormatUtil.money(ElectricityService.UNIT_PRICE) + " 元/度");
        UI.style(status, UI.SECTION);
        body.getChildren().add(status);

        // ---- 购电表单 ----
        final TextField degreeField = new TextField();
        degreeField.setPromptText("请输入购电度数，如 200");
        degreeField.setPrefWidth(220);
        final Label amountLabel = new Label("预计金额：-- 元");
        UI.style(amountLabel, UI.MUTED);
        degreeField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double d = Double.parseDouble(newVal.trim());
                amountLabel.setText("预计金额：" + FormatUtil.money(Math.round(d * ElectricityService.UNIT_PRICE * 100) / 100.0) + " 元");
            } catch (NumberFormatException ex) {
                amountLabel.setText("预计金额：-- 元");
            }
        });

        Button submit = new Button("提交购电申请");
        UI.style(submit, UI.BTN, UI.BTN_PRIMARY);
        submit.setOnAction(e -> {
            try {
                double degree = Double.parseDouble(degreeField.getText().trim());
                service.submitPurchase(student, degree);
                AlertUtil.info("购电申请已提交，请到宿管科售电窗口缴费，到账后将计入房间电表。");
                degreeField.clear();
                renderElectricity(body);
            } catch (NumberFormatException ex) {
                AlertUtil.error("请输入有效的购电度数（数字）");
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        HBox formRow = new HBox(10, new Label("购电度数："), degreeField, submit);
        formRow.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label("提示：提交后状态为「待售电」，宿管科售电后方计入房间剩余电量。");
        UI.style(hint, UI.FAINT);
        body.getChildren().addAll(formRow, amountLabel, hint);

        // ---- 我的购电记录 ----
        Label recTitle = new Label("我的购电记录");
        UI.style(recTitle, UI.SECTION);
        body.getChildren().add(recTitle);

        TableView<ElectricityPurchase> table = new TableView<>();
        table.getColumns().add(col("单号", 90, ElectricityPurchase::getId));
        table.getColumns().add(col("房间", 110, ElectricityPurchase::getRoomKey));
        table.getColumns().add(col("购电(度)", 90, p -> FormatUtil.num(p.getDegree())));
        table.getColumns().add(col("应付(元)", 90, p -> FormatUtil.money(p.getAmount())));
        table.getColumns().add(col("状态", 90, ElectricityPurchase::getStatusName));
        table.getColumns().add(col("提交时间", 150, ElectricityPurchase::getCreateTime));
        table.getColumns().add(col("售电时间", 150, p -> safe(p.getHandleTime())));
        table.getColumns().add(col("经办", 90, p -> safe(p.getHandlerId())));
        table.setPrefHeight(300);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(service.ofStudent(student.getId())));
        body.getChildren().add(table);
    }

    // ---------- 维修报修 ----------

    /** 维修报修页：为本人当前房间报修；先由分管宿管接收上报，再由宿管科受理/办结。 */
    public Node buildRepair() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("维修报修");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        final VBox body = new VBox(12);
        box.getChildren().add(body);
        renderRepair(body);
        return wrap(box);
    }

    private void renderRepair(final VBox body) {
        body.getChildren().clear();
        final RepairService service = new RepairService();

        if (!student.isCheckedIn()) {
            Label note = new Label("您尚未入住宿舍，无法报修。请先在【宿舍申请】办理入住。");
            UI.style(note, UI.DANGER);
            body.getChildren().add(note);
            return;
        }

        Label status = new Label("报修房间：" + student.getCurrentBuilding() + " - " + student.getCurrentRoom());
        UI.style(status, UI.SECTION);
        body.getChildren().add(status);

        final TextArea desc = new TextArea();
        desc.setPromptText("请描述需要维修的问题（如：灯管不亮、空调不制冷、门锁损坏…）");
        desc.setPrefRowCount(4);
        Button submit = new Button("提交报修单");
        UI.style(submit, UI.BTN, UI.BTN_WARNING);
        submit.setOnAction(e -> {
            try {
                service.submitRepair(student, desc.getText());
                AlertUtil.info("报修单已提交，等待宿管核实后上报宿管科。");
                desc.clear();
                renderRepair(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });
        body.getChildren().addAll(new Label("维修内容："), desc, submit);

        Label recTitle = new Label("我的报修记录");
        UI.style(recTitle, UI.SECTION);
        body.getChildren().add(recTitle);
        body.getChildren().add(note("报修单先由分管您楼栋的宿管核实，再由宿管上报宿管科安排维修；"
                + "状态依次为「待宿管上报 → 待处理 → 处理中 → 已完成」。"));

        TableView<RepairTicket> table = new TableView<>();
        table.getColumns().add(col("单号", 90, RepairTicket::getId));
        table.getColumns().add(col("房间", 100, RepairTicket::getRoomKey));
        table.getColumns().add(col("维修内容", 220, RepairTicket::getDescription));
        table.getColumns().add(col("状态", 110, RepairTicket::getStatusName));
        table.getColumns().add(col("提交时间", 150, RepairTicket::getCreateTime));
        table.getColumns().add(col("上报时间", 150, t -> safe(t.getEscalateTime())));
        table.getColumns().add(col("处理人", 100, t -> safe(t.getHandlerId())));
        table.getColumns().add(col("处理时间", 150, t -> safe(t.getHandleTime())));
        table.setPrefHeight(280);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(service.ofStudent(student.getId())));
        body.getChildren().add(table);
    }

    // ---------- 夜归确认 ----------

    /**
     * 夜归确认页：查看宿管登记的本人夜归记录，逐条确认「已到寝」或提异议。
     */
    public Node buildNightReturnConfirm() {
        VBox box = page("夜归确认");
        final VBox body = new VBox(12);
        box.getChildren().add(body);
        renderNightReturnConfirm(body);
        return wrap(box);
    }

    private void renderNightReturnConfirm(final VBox body) {
        body.getChildren().clear();
        final StudentLifeService service = new StudentLifeService();
        List<NightReturnRecord> records = service.nightReturnsOf(student);
        if (records.isEmpty()) {
            body.getChildren().add(note("暂无夜归登记记录。若宿管已登记您的夜归情况，此处会显示待确认记录。"));
            return;
        }
        int pending = service.pendingNightReturnCount(student);
        body.getChildren().add(pending > 0
                ? warn("您有 " + pending + " 条夜归记录待确认，请在下方表格中选中后确认。")
                : note("您已处理完全部夜归记录。如对登记内容有异议，仍可提出。"));
        body.getChildren().add(note("说明：夜归记录由宿舍管理人员登记；请核对夜归时间与原因，确认无误后点「确认已到寝」。"
                + "若登记内容与实际情况不符，请点「有异议」并填写说明，宿管会据此核实。"
                + "宿管复核后结论为最终结果，不可再修改，复核意见显示在「确认状态与说明」列。"));

        final TableView<NightReturnRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, NightReturnRecord::getId));
        table.getColumns().add(col("日期", 110, NightReturnRecord::getDate));
        table.getColumns().add(col("夜归时间", 100, NightReturnRecord::getReturnTime));
        table.getColumns().add(col("登记原因", 160, r -> safe(r.getReason())));
        table.getColumns().add(col("确认状态与说明", 300, NightReturnRecord::getConfirmSummary));
        table.setPrefHeight(320);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(records));
        body.getChildren().add(table);

        Button confirm = new Button("确认已到寝");
        UI.style(confirm, UI.BTN, UI.BTN_SUCCESS);
        confirm.setOnAction(e -> {
            NightReturnRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.warn("请先在表格中选择一条夜归记录");
                return;
            }
            try {
                service.confirmNightReturn(student, selected);
                AlertUtil.info("已确认到寝：" + selected.getDate() + " " + selected.getReturnTime() + "。");
                renderNightReturnConfirm(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        Button dispute = new Button("有异议");
        UI.style(dispute, UI.BTN, UI.BTN_WARNING);
        dispute.setOnAction(e -> {
            NightReturnRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.warn("请先在表格中选择一条夜归记录");
                return;
            }
            String remark = askRemark("对夜归记录 " + selected.getId() + " 提出异议", "异议说明",
                    "请说明哪里与实际不符（如夜归时间、原因）");
            if (remark == null) {
                return;
            }
            try {
                service.disputeNightReturn(student, selected, remark);
                AlertUtil.info("已提交异议，宿管将核实处理。");
                renderNightReturnConfirm(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        HBox actions = new HBox(10, confirm, dispute);
        actions.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().add(actions);
    }

    // ---------- 卫生检查（查看与反馈） ----------

    /**
     * 卫生检查页：查看宿管对本寝室的评分与评语，历史记录可查，并可反馈「无异议 / 有问题」。
     */
    public Node buildHygiene() {
        VBox box = page("卫生检查");
        final VBox body = new VBox(12);
        box.getChildren().add(body);
        renderHygiene(body);
        return wrap(box);
    }

    private void renderHygiene(final VBox body) {
        body.getChildren().clear();
        final StudentLifeService service = new StudentLifeService();

        if (!student.isCheckedIn()) {
            body.getChildren().add(warn("您尚未入住宿舍，暂无可查看的卫生检查记录。"));
            return;
        }
        String roomText = student.getCurrentBuilding() + " - " + student.getCurrentRoom();
        List<HygieneRecord> records = service.hygieneRecordsOf(student);
        double average = service.averageScoreOf(student);

        Label summary = new Label("本寝室：" + roomText
                + "　·　检查次数：" + records.size() + " 次"
                + (average < 0 ? "" : "　·　平均分：" + FormatUtil.num(average) + " 分"));
        UI.style(summary, UI.SECTION);
        body.getChildren().add(summary);

        if (records.isEmpty()) {
            body.getChildren().add(note("宿管尚未对本寝室做过卫生检查。检查完成后，评分与评语会显示在这里，历史记录长期可查。"));
            return;
        }
        body.getChildren().add(note("以下为宿管对本寝室的历次卫生检查评分与评语（含历史记录）。"
                + "如对某次检查的评分或评语有疑问，请选中该行后点「反馈问题」。"
                + "宿管复核后结论为最终结果，不可再反馈，复核意见显示在「我的反馈」列。"));

        final TableView<HygieneRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, HygieneRecord::getId));
        table.getColumns().add(col("检查日期", 110, HygieneRecord::getDate));
        table.getColumns().add(col("得分", 70, r -> String.valueOf((int) r.getScore())));
        table.getColumns().add(col("检查人", 140, r -> personLabel(r.getInspectorId())));
        table.getColumns().add(col("评语", 220, r -> safe(r.getComment())));
        table.getColumns().add(col("我的反馈", 180, HygieneRecord::getStudentSummary));
        table.setPrefHeight(320);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(records));
        body.getChildren().add(table);

        Button acknowledge = new Button("无异议");
        UI.style(acknowledge, UI.BTN, UI.BTN_SUCCESS);
        acknowledge.setOnAction(e -> {
            HygieneRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.warn("请先在表格中选择一条检查记录");
                return;
            }
            try {
                service.acknowledgeHygiene(student, selected);
                AlertUtil.info("已反馈「无异议」。");
                renderHygiene(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        Button dispute = new Button("反馈问题");
        UI.style(dispute, UI.BTN, UI.BTN_WARNING);
        dispute.setOnAction(e -> {
            HygieneRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.warn("请先在表格中选择一条检查记录");
                return;
            }
            String remark = askRemark("对卫生检查 " + selected.getId() + " 反馈问题", "问题说明",
                    "请说明对本次评分或评语有疑问的地方");
            if (remark == null) {
                return;
            }
            try {
                service.disputeHygiene(student, selected, remark);
                AlertUtil.info("已提交反馈，宿管将核实处理。");
                renderHygiene(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        HBox actions = new HBox(10, acknowledge, dispute);
        actions.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().addAll(actions,
                hint("同一寝室的室友均可查看与反馈，反馈会记录在对应的检查记录上。"));
    }

    // ---------- 贵重物品确认 ----------

    /**
     * 贵重物品确认页：列出本人在宿管处登记的全部贵重物品出入记录（带入 / 带出），
     * 其中带出的逐条确认「未丢失」，遗失则报「已遗失」，由宿管上报宿管科发布公告（进度在本页可见）。
     */
    public Node buildValuablesConfirm() {
        VBox box = page("贵重物品确认");
        final VBox body = new VBox(12);
        box.getChildren().add(body);
        renderValuablesConfirm(body);
        return wrap(box);
    }

    private void renderValuablesConfirm(final VBox body) {
        body.getChildren().clear();
        final StudentLifeService service = new StudentLifeService();
        List<ValuablesRecord> records = service.valuablesOf(student);
        if (records.isEmpty()) {
            body.getChildren().add(note("暂无贵重物品出入登记记录。"
                    + "宿管登记您携带贵重物品进出楼栋后，此处会显示记录。"));
            return;
        }
        int pending = service.pendingValuablesCount(student);
        body.getChildren().add(pending > 0
                ? warn("您有 " + pending + " 件贵重物品待确认是否安全，请及时确认。")
                : note("您已处理完全部贵重物品确认。"));
        body.getChildren().add(note("说明：宿管登记您携带贵重物品进出楼栋后，请逐条确认物品是否安全——"
                + "「带入」的物品确认在楼内未丢失，「带出」的物品确认已安全带回；"
                + "若确已遗失，请点「报告遗失」并填写说明，宿管核实后将上报宿管科发布全校公告协助查找。"));

        final TableView<ValuablesRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, ValuablesRecord::getId));
        table.getColumns().add(col("物品", 160, ValuablesRecord::getItemName));
        table.getColumns().add(col("方向", 70, ValuablesRecord::getDirectionName));
        table.getColumns().add(col("登记时间", 150, ValuablesRecord::getRecordTime));
        table.getColumns().add(col("登记人", 130, r -> personLabel(r.getHandlerId())));
        table.getColumns().add(col("确认状态", 90, ValuablesRecord::getConfirmStatusName));
        table.getColumns().add(col("上报进度", 110, ValuablesRecord::getReportStatusName));
        table.getColumns().add(col("我的说明", 180, r -> safe(r.getStudentRemark())));
        table.setPrefHeight(320);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(records));
        body.getChildren().add(table);

        Label progress = new Label(reportProgressText(records));
        progress.setWrapText(true);
        UI.style(progress, UI.MUTED);
        body.getChildren().add(progress);

        Button intact = new Button("确认未丢失");
        UI.style(intact, UI.BTN, UI.BTN_SUCCESS);
        intact.setOnAction(e -> {
            ValuablesRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.warn("请先在表格中选择一条贵重物品记录");
                return;
            }
            try {
                service.confirmValuablesIntact(student, selected);
                AlertUtil.info("已确认「" + selected.getItemName() + "」未丢失。");
                renderValuablesConfirm(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        Button lost = new Button("报告遗失");
        UI.style(lost, UI.BTN, UI.BTN_DANGER);
        lost.setOnAction(e -> {
            ValuablesRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.warn("请先在表格中选择一条贵重物品记录");
                return;
            }
            String remark = askRemark("报告遗失：「" + selected.getItemName() + "」", "遗失说明",
                    "请描述物品特征、遗失时间与地点等，便于宿管科通报查找");
            if (remark == null) {
                return;
            }
            try {
                service.reportValuablesLost(student, selected, remark);
                AlertUtil.info("已报告遗失。宿管核实后将上报宿管科，由宿管科发布全校公告。");
                renderValuablesConfirm(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        HBox actions = new HBox(10, intact, lost);
        actions.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().add(actions);
        body.getChildren().add(hint("若确认后物品又发生遗失，请联系宿管重新登记后报损。"));
    }

    /** 汇总本人贵重物品的上报进度提示（有遗失记录时才给提示行）。 */
    private String reportProgressText(List<ValuablesRecord> records) {
        int lost = 0;
        int escalated = 0;
        int published = 0;
        for (ValuablesRecord r : records) {
            if (ValuablesRecord.REPORT_STUDENT_REPORTED.equals(r.getReportStatus())) {
                lost++;
            } else if (ValuablesRecord.REPORT_ESCALATED.equals(r.getReportStatus())) {
                escalated++;
            } else if (ValuablesRecord.REPORT_PUBLISHED.equals(r.getReportStatus())) {
                published++;
            }
        }
        List<String> parts = new ArrayList<>();
        if (lost > 0) {
            parts.add(lost + " 件已报遗失，待宿管核实上报");
        }
        if (escalated > 0) {
            parts.add(escalated + " 件宿管已上报宿管科，待发布公告");
        }
        if (published > 0) {
            parts.add(published + " 件宿管科已发布公告");
        }
        return parts.isEmpty() ? "" : "遗失进度：" + String.join("；", parts) + "。";
    }

    // ---------- 公告 ----------

    /** 公告页：查看宿管科发布的全校公告（当前为物品遗失通报）。 */
    public Node buildAnnouncements() {
        VBox box = page("公告");
        final VBox body = new VBox(12);
        box.getChildren().add(body);
        body.setMaxWidth(760);

        List<Announcement> list = new StudentLifeService().announcements();
        if (list.isEmpty()) {
            body.getChildren().add(note("暂无公告。"));
            return wrap(box);
        }
        for (Announcement a : list) {
            body.getChildren().add(announcementCard(a));
        }
        return wrap(box);
    }

    /** 单条公告卡片：标题 + 正文 + 发布人/时间（正文自动换行）。 */
    private Node announcementCard(Announcement a) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 16, 14, 16));
        UI.style(card, UI.CARD);

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(a.getTitle());
        UI.style(title, UI.SECTION);
        Label typeChip = new Label(a.getTypeName());
        UI.style(typeChip, UI.MUTED);
        head.getChildren().addAll(title, typeChip);

        Label content = new Label(a.getContent());
        content.setWrapText(true);

        Label meta = new Label("发布人：" + personLabel(a.getPublisherId())
                + "　·　发布时间：" + safe(a.getPublishTime())
                + (safe(a.getRelatedId()).isEmpty() ? "" : "　·　来源记录：" + a.getRelatedId()));
        UI.style(meta, UI.FAINT);

        card.getChildren().addAll(head, content, meta);
        return card;
    }

    // ---------- 设置 ----------

    /**
     * 设置页：更换头像、修改本人姓名与登录密码（与辅导员共用 {@link SettingsView}）。
     * 头像默认校徽，可换成自己的图片；姓名默认为学号、可自行修改（学号不可更改）；
     * 改密码须先验证当前账号的密码，且两次输入的新密码一致才会生效。
     *
     * @param onProfileChanged 头像/姓名修改成功后的回调（主框架据此刷新顶栏头像与问候语），可为 null
     */
    public Node buildSettings(Runnable onProfileChanged) {
        return new SettingsView(student, "学号",
                "提示：姓名默认与学号一致，可自行修改；学号是账号与档案的唯一标识，不可更改。")
                .build(onProfileChanged);
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

    /** 新建一个带页面标题的纵向容器（新页面统一用它的排版）。 */
    private VBox page(String titleText) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setMaxWidth(760);
        Label title = new Label(titleText);
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);
        return box;
    }

    /** 说明性文字（灰色、自动换行）。 */
    private Label note(String text) {
        Label l = new Label(text);
        UI.style(l, UI.MUTED);
        l.setWrapText(true);
        return l;
    }

    /** 需要学生注意的提醒（橙色、自动换行）。 */
    private Label warn(String text) {
        Label l = new Label(text);
        UI.style(l, UI.WARN_DEEP);
        l.setWrapText(true);
        return l;
    }

    /** 次要提示（浅灰、自动换行）。 */
    private Label hint(String text) {
        Label l = new Label(text);
        UI.style(l, UI.FAINT);
        l.setWrapText(true);
        return l;
    }

    /** 人员标签：姓名 + 工号/学号；查不到时退回编号原文。 */
    private String personLabel(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        Person p = DataCenter.instance().findPersonById(id);
        return p == null ? id : p.getName() + "（" + id + "）";
    }

    /**
     * 弹窗收集一段说明文字。取消或留空返回 null（调用方据此中止操作）；
     * 说明内容较长，故用多行 {@link TextArea} 而非单行输入框。
     */
    private String askRemark(String header, String fieldLabel, String prompt) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("填写说明");
        dialog.setHeaderText(header);
        ButtonType okType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(4);
        area.setPrefColumnCount(34);
        VBox content = new VBox(8, new Label(fieldLabel), area);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        UI.applyToDialog(dialog.getDialogPane());

        dialog.setResultConverter(button -> button == okType ? area.getText() : null);
        // 弹窗显示后把焦点放进输入框，打开即可直接输入
        Platform.runLater(area::requestFocus);

        Optional<String> result = dialog.showAndWait();
        return result.isPresent() ? result.get() : null;
    }

    private Label label(String text) {
        Label l = new Label(text);
        UI.style(l, UI.KEY);
        return l;
    }

    /** 表单字段名（比 key-value 的 label 更醒目）。 */
    private Label fieldLabel(String text) {
        Label l = new Label(text);
        UI.style(l, UI.FIELD);
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
        UI.style(sp, UI.PAGE_SCROLL);
        return sp;
    }
}

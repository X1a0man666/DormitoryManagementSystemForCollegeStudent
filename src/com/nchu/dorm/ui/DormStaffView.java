package com.nchu.dorm.ui;

import com.nchu.dorm.model.Bed;
import com.nchu.dorm.model.Building;
import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.application.RepairTicket;
import com.nchu.dorm.model.record.HygieneRecord;
import com.nchu.dorm.model.record.NightReturnRecord;
import com.nchu.dorm.model.record.ValuablesRecord;
import com.nchu.dorm.service.DormStaffService;
import com.nchu.dorm.service.RepairService;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.ui.component.AlertUtil;
import com.nchu.dorm.ui.component.DatePickers;
import com.nchu.dorm.ui.component.UI;
import com.nchu.dorm.util.BusinessException;
import com.nchu.dorm.util.TimeUtil;
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
import javafx.scene.control.DatePicker;
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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 宿舍管理人员视图：日常管理工作台 / 夜归登记 / 卫生检查 / 贵重物品出入登记 / 遗失上报 / 报修处理。
 * 登记、复核与上报规则收口在 {@link DormStaffService} 与 {@link RepairService}：
 * 只能处理本人分管楼栋内入住的学生 / 房间。
 * 学生的确认与反馈见 {@link StudentView}（夜归确认 / 卫生检查 / 贵重物品确认）。
 */
public class DormStaffView {

    private final DormStaff staff;
    private final DormStaffService dormService = new DormStaffService();
    private final RepairService repairService = new RepairService();

    public DormStaffView(DormStaff staff) {
        this.staff = staff;
    }

    // ==================== 工作台 ====================

    public Node build() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setMaxWidth(760);

        Label title = new Label("日常管理工作台");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        addInfoRows(box, new String[][]{
                {"姓名", staff.getName()},
                {"工号", staff.getId()},
                {"职务", staff.getJobTitle()},
                {"负责楼栋", String.join("、", staff.getManageBuildingNames())},
        });

        DataCenter dc = DataCenter.instance();
        Label bTitle = new Label("负责楼栋简况");
        UI.style(bTitle, UI.SECTION);
        box.getChildren().add(bTitle);
        for (String name : staff.getManageBuildingNames()) {
            Building b = dc.findBuilding(name);
            if (b != null) {
                Label l = new Label("· " + name + "（" + (b.isMale() ? "男" : b.isFemale() ? "女" : "未定") + "）"
                        + "　入住 " + dc.occupiedCountOfBuilding(name) + " 人 / "
                        + dc.findRoomsOfBuilding(name).size() + " 间房间");
                box.getChildren().add(l);
            }
        }
        Label hint = new Label("请在左侧功能菜单选择：夜归登记 · 卫生检查 · 贵重物品登记 · 遗失上报 · 报修处理。");
        UI.style(hint, UI.MUTED);
        box.getChildren().add(hint);
        return wrap(box);
    }

    // ==================== 夜归登记 ====================

    public Node buildNightReturn() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("夜归登记");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        final VBox body = new VBox(12);
        box.getChildren().add(body);
        renderNightReturn(body);
        return wrap(box);
    }

    private void renderNightReturn(final VBox body) {
        body.getChildren().clear();
        body.getChildren().add(note("登记学生夜归（仅限本人分管楼栋入住的在校学生）。"));

        final TextField studentField = new TextField();
        studentField.setPromptText("输入学生学号，如 25201101");
        studentField.setPrefWidth(220);
        final Label preview = new Label();
        UI.style(preview, UI.OK);
        Button query = button("查询学生", "#3498db", e -> {
            try {
                Student s = dormService.findManagedStudent(staff, studentField.getText());
                preview.setText("已确认：" + s.getName() + "（" + s.getId() + "），现居 "
                        + s.getCurrentBuilding() + " - " + s.getCurrentRoom());
            } catch (BusinessException ex) {
                preview.setText("");
                AlertUtil.error(ex.getMessage());
            }
        });

        final DatePicker datePicker = DatePickers.create(TimeUtil.today());
        final TextField timeField = new TextField();
        timeField.setPromptText("夜归时间，如 23:30");
        timeField.setPrefWidth(140);
        final TextField reasonField = new TextField();
        reasonField.setPromptText("夜归原因（可选）");
        reasonField.setPrefWidth(300);

        Button register = button("登记夜归", "#27ae60", e -> {
            try {
                dormService.registerNightReturn(staff, studentField.getText(),
                        DatePickers.textOf(datePicker), timeField.getText(), reasonField.getText());
                AlertUtil.info("已登记夜归记录。");
                renderNightReturn(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        HBox studentRow = new HBox(10, new Label("学生："), studentField, query, preview);
        studentRow.setAlignment(Pos.CENTER_LEFT);
        HBox timeRow = new HBox(10, new Label("日期："), datePicker,
                new Label("时间："), timeField);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        HBox reasonRow = new HBox(10, new Label("原因："), reasonField, register);
        reasonRow.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().addAll(studentRow, timeRow, reasonRow);

        body.getChildren().add(sectionTitle("夜归记录"));
        TableView<NightReturnRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, NightReturnRecord::getId));
        table.getColumns().add(col("学号", 90, NightReturnRecord::getStudentId));
        table.getColumns().add(col("姓名·宿舍", 200, r -> occupantLabel(r.getStudentId())));
        table.getColumns().add(col("日期", 110, NightReturnRecord::getDate));
        table.getColumns().add(col("夜归时间", 100, NightReturnRecord::getReturnTime));
        table.getColumns().add(col("原因", 160, r -> safe(r.getReason())));
        table.getColumns().add(col("学生确认", 180, NightReturnRecord::getConfirmSummary));
        table.setPrefHeight(260);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(dormService.nightReturnsOfStaff(staff)));
        body.getChildren().add(table);

        body.getChildren().addAll(divider(), buildNightReviewSection(body));
    }

    /**
     * 夜归异议复核区块：列出分管楼栋内学生已提异议的记录，
     * 复核结论二选一——「维持原登记」或「修改后通过」（更正日期 / 时间 / 原因后通过）。
     * 复核后记录终结，学生不可再提异议。
     */
    private Node buildNightReviewSection(final VBox body) {
        VBox section = new VBox(12);
        List<NightReturnRecord> pending = dormService.pendingNightReviewOfStaff(staff);
        section.getChildren().add(sectionTitle("待复核异议（" + pending.size() + " 条）"));
        if (pending.isEmpty()) {
            section.getChildren().add(note("暂无待复核的夜归异议。学生在「夜归确认」中提出异议后，记录会出现在这里。"));
            return section;
        }
        section.getChildren().add(note("请核对学生的异议说明，选择「维持原登记」或更正登记内容后通过。"
                + "复核结论为最终结果，学生不可再提异议。"));

        final TableView<NightReturnRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, NightReturnRecord::getId));
        table.getColumns().add(col("学号", 90, NightReturnRecord::getStudentId));
        table.getColumns().add(col("姓名·宿舍", 200, r -> occupantLabel(r.getStudentId())));
        table.getColumns().add(col("日期", 110, NightReturnRecord::getDate));
        table.getColumns().add(col("夜归时间", 100, NightReturnRecord::getReturnTime));
        table.getColumns().add(col("原因", 150, r -> safe(r.getReason())));
        table.getColumns().add(col("学生异议", 200, r -> safe(r.getStudentRemark())));
        table.getColumns().add(col("异议时间", 150, r -> safe(r.getConfirmTime())));
        table.setPrefHeight(240);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(pending));
        section.getChildren().add(table);

        Button keep = button("维持原登记", "#2c3e50", e -> {
            NightReturnRecord selected = selectedNightReturn(table);
            if (selected == null) {
                return;
            }
            String comment = askReviewComment("维持夜归登记 " + selected.getId(),
                    "复核意见（可留空）：说明核实过程与维持原登记的理由");
            if (comment == null) {
                return;
            }
            if (!AlertUtil.confirm("确定对夜归记录 " + selected.getId() + " 维持原登记吗？"
                    + "复核后学生不可再提异议。")) {
                return;
            }
            try {
                dormService.reviewNightReturn(selected, staff, true, null, null, null, comment);
                AlertUtil.info("已复核：维持原登记。");
                renderNightReturn(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        Button adjust = button("修改后通过", "#27ae60", e -> {
            NightReturnRecord selected = selectedNightReturn(table);
            if (selected == null) {
                return;
            }
            NightAdjust adjustInput = askNightAdjust(selected);
            if (adjustInput == null) {
                return;
            }
            try {
                dormService.reviewNightReturn(selected, staff, false, adjustInput.date,
                        adjustInput.time, adjustInput.reason, adjustInput.comment);
                AlertUtil.info("已复核：登记内容已更正，学生不可再提异议。");
                renderNightReturn(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        Button keepAll = button("一键维持全部待复核", "#2c3e50", e -> {
            int count = dormService.pendingNightReviewOfStaff(staff).size();
            if (count == 0) {
                AlertUtil.info("当前没有待复核的夜归异议。");
                return;
            }
            if (!AlertUtil.confirm("确定对全部 " + count + " 条夜归异议维持原登记吗？"
                    + "（限本人分管楼栋范围，复核后学生不可再提异议）")) {
                return;
            }
            int done = dormService.reviewAllNightReturnsKeep(staff);
            AlertUtil.info("已复核 " + done + " 条夜归异议：维持原登记。");
            renderNightReturn(body);
        });

        HBox actions = new HBox(10, keep, adjust, keepAll);
        actions.setAlignment(Pos.CENTER_LEFT);
        section.getChildren().add(actions);
        return section;
    }

    private NightReturnRecord selectedNightReturn(TableView<NightReturnRecord> table) {
        NightReturnRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("请先在表格中选择一条待复核的夜归异议记录");
        }
        return selected;
    }

    // ==================== 卫生检查 ====================

    public Node buildHygiene() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("卫生检查");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        final VBox body = new VBox(12);
        box.getChildren().add(body);
        renderHygiene(body);
        return wrap(box);
    }

    private void renderHygiene(final VBox body) {
        body.getChildren().clear();
        if (staff.getManageBuildingNames().isEmpty()) {
            body.getChildren().add(note("您尚未被分配负责楼栋，请联系宿管科在「宿舍管理人员管理」中分配。"));
            return;
        }
        body.getChildren().add(note("对本人分管楼栋内房间进行卫生检查评分（百分制）。"));

        final ComboBox<String> buildingCombo = new ComboBox<>();
        buildingCombo.getItems().addAll(staff.getManageBuildingNames());
        buildingCombo.getSelectionModel().selectFirst();

        final TextField roomField = new TextField();
        roomField.setPromptText("房间号，如 101、1205");
        roomField.setPrefWidth(120);
        final Label roomPreview = new Label();
        UI.style(roomPreview, UI.OK);
        Button queryRoom = button("查询房间", "#3498db", e -> {
            String building = buildingCombo.getValue();
            String roomNo = roomField.getText() == null ? "" : roomField.getText().trim();
            if (building == null || roomNo.isEmpty()) {
                AlertUtil.warn("请先选择楼栋并填写房间号");
                return;
            }
            Room room = DataCenter.instance().findRoom(building, roomNo);
            if (room == null) {
                roomPreview.setText("");
                AlertUtil.error("「" + building + "」中不存在房间 " + roomNo);
                return;
            }
            roomPreview.setText(room.displayKey() + "（4 人间，当前入住 " + (room.getCapacity() - room.availableBedCount())
                    + " 人）" + occupantsText(room));
        });

        final TextField dateField = new TextField(TimeUtil.today());
        dateField.setPromptText("yyyy-MM-dd");
        dateField.setPrefWidth(130);
        final TextField scoreField = new TextField();
        scoreField.setPromptText("得分 0~100");
        scoreField.setPrefWidth(100);
        final TextField commentField = new TextField();
        commentField.setPromptText("评语（可选）");
        commentField.setPrefWidth(260);

        Button register = button("保存检查记录", "#27ae60", e -> {
            try {
                double score = Double.parseDouble(scoreField.getText().trim());
                dormService.registerHygiene(staff, buildingCombo.getValue(), roomField.getText(),
                        dateField.getText(), score, commentField.getText());
                AlertUtil.info("已保存卫生检查记录。");
                renderHygiene(body);
            } catch (NumberFormatException ex) {
                AlertUtil.error("卫生得分需为 0~100 之间的数字");
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        HBox roomRow = new HBox(10, new Label("楼栋："), buildingCombo,
                new Label("房间："), roomField, queryRoom);
        roomRow.setAlignment(Pos.CENTER_LEFT);
        HBox dateRow = new HBox(10, new Label("日期："), dateField,
                new Label("得分："), scoreField);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        HBox commentRow = new HBox(10, new Label("评语："), commentField, register);
        commentRow.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().addAll(roomRow, roomPreview, dateRow, commentRow);

        body.getChildren().add(sectionTitle("本楼栋卫生检查记录"));
        TableView<HygieneRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, HygieneRecord::getId));
        table.getColumns().add(col("房间", 130, HygieneRecord::getRoomKey));
        table.getColumns().add(col("日期", 110, HygieneRecord::getDate));
        table.getColumns().add(col("得分", 70, r -> String.valueOf((int) r.getScore())));
        table.getColumns().add(col("检查人", 120, r -> personLabel(r.getInspectorId())));
        table.getColumns().add(col("评语", 220, r -> safe(r.getComment())));
        table.setPrefHeight(240);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(dormService.hygieneRecordsOfStaff(staff)));
        body.getChildren().add(table);

        body.getChildren().addAll(divider(), buildHygieneReviewSection(body));
    }

    /**
     * 卫生检查反馈复核区块：列出分管楼栋内学生已反馈「有问题」的记录，
     * 复核结论二选一——「维持原记录」或「更正后通过」（改正得分 / 评语后通过）。
     * 复核后记录终结，学生不可再反馈。
     */
    private Node buildHygieneReviewSection(final VBox body) {
        VBox section = new VBox(12);
        List<HygieneRecord> pending = dormService.pendingHygieneReviewOfStaff(staff);
        section.getChildren().add(sectionTitle("待复核反馈（" + pending.size() + " 条）"));
        if (pending.isEmpty()) {
            section.getChildren().add(note("暂无待复核的卫生检查反馈。学生在「我的卫生检查」中反馈问题后，记录会出现在这里。"));
            return section;
        }
        section.getChildren().add(note("请核对学生的反馈说明，选择「维持原记录」或更正得分 / 评语后通过。"
                + "复核结论为最终结果，学生不可再反馈。"));

        final TableView<HygieneRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, HygieneRecord::getId));
        table.getColumns().add(col("房间", 130, HygieneRecord::getRoomKey));
        table.getColumns().add(col("日期", 110, HygieneRecord::getDate));
        table.getColumns().add(col("得分", 70, r -> String.valueOf((int) r.getScore())));
        table.getColumns().add(col("评语", 180, r -> safe(r.getComment())));
        table.getColumns().add(col("学生反馈", 200, r -> safe(r.getStudentRemark())));
        table.getColumns().add(col("反馈时间", 150, r -> safe(r.getFeedbackTime())));
        table.setPrefHeight(240);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(pending));
        section.getChildren().add(table);

        Button keep = button("维持原记录", "#2c3e50", e -> {
            HygieneRecord selected = selectedHygiene(table);
            if (selected == null) {
                return;
            }
            String comment = askReviewComment("维持卫生检查 " + selected.getId(),
                    "复核意见（可留空）：说明核实过程与维持原记录的理由");
            if (comment == null) {
                return;
            }
            if (!AlertUtil.confirm("确定对卫生检查 " + selected.getId() + " 维持原记录吗？"
                    + "复核后学生不可再反馈。")) {
                return;
            }
            try {
                dormService.reviewHygiene(selected, staff, true, 0, null, comment);
                AlertUtil.info("已复核：维持原记录。");
                renderHygiene(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        Button adjust = button("更正后通过", "#27ae60", e -> {
            HygieneRecord selected = selectedHygiene(table);
            if (selected == null) {
                return;
            }
            HygieneAdjust adjustInput = askHygieneAdjust(selected);
            if (adjustInput == null) {
                return;
            }
            try {
                dormService.reviewHygiene(selected, staff, false, adjustInput.score,
                        adjustInput.comment, adjustInput.reviewComment);
                AlertUtil.info("已复核：得分 / 评语已更正，学生不可再反馈。");
                renderHygiene(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        Button keepAll = button("一键维持全部待复核", "#2c3e50", e -> {
            int count = dormService.pendingHygieneReviewOfStaff(staff).size();
            if (count == 0) {
                AlertUtil.info("当前没有待复核的卫生检查反馈。");
                return;
            }
            if (!AlertUtil.confirm("确定对全部 " + count + " 条卫生检查反馈维持原记录吗？"
                    + "（限本人分管楼栋范围，复核后学生不可再反馈）")) {
                return;
            }
            int done = dormService.reviewAllHygieneKeep(staff);
            AlertUtil.info("已复核 " + done + " 条卫生检查反馈：维持原记录。");
            renderHygiene(body);
        });

        HBox actions = new HBox(10, keep, adjust, keepAll);
        actions.setAlignment(Pos.CENTER_LEFT);
        section.getChildren().add(actions);
        return section;
    }

    private HygieneRecord selectedHygiene(TableView<HygieneRecord> table) {
        HygieneRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("请先在表格中选择一条待复核的卫生检查反馈");
        }
        return selected;
    }

    private String occupantsText(Room room) {
        List<String> names = new ArrayList<>();
        for (Bed bed : room.getBeds()) {
            if (!bed.isEmpty()) {
                Student s = DataCenter.instance().findStudentById(bed.getOccupantId());
                names.add(s == null ? bed.getOccupantId() : s.getName());
            }
        }
        return names.isEmpty() ? "（暂无入住学生）" : "　室友：" + String.join("、", names);
    }

    // ==================== 贵重物品出入登记 ====================

    public Node buildValuables() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("贵重物品出入登记");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        final VBox body = new VBox(12);
        box.getChildren().add(body);
        renderValuables(body);
        return wrap(box);
    }

    private void renderValuables(final VBox body) {
        body.getChildren().clear();
        body.getChildren().add(note("登记学生携带贵重物品进出楼栋（仅限本人分管楼栋入住的在校学生）。"));

        final TextField studentField = new TextField();
        studentField.setPromptText("学生学号");
        studentField.setPrefWidth(180);
        final Label preview = new Label();
        UI.style(preview, UI.OK);
        Button query = button("查询学生", "#3498db", e -> {
            try {
                Student s = dormService.findManagedStudent(staff, studentField.getText());
                preview.setText("已确认：" + s.getName() + "（" + s.getId() + "），现居 "
                        + s.getCurrentBuilding() + " - " + s.getCurrentRoom());
            } catch (BusinessException ex) {
                preview.setText("");
                AlertUtil.error(ex.getMessage());
            }
        });

        final TextField itemField = new TextField();
        itemField.setPromptText("物品名称，如 笔记本电脑");
        itemField.setPrefWidth(200);
        final ComboBox<String> dirCombo = new ComboBox<>();
        dirCombo.getItems().addAll("带出", "带入");
        dirCombo.getSelectionModel().selectFirst();

        Button register = button("登记", "#27ae60", e -> {
            String direction = "带出".equals(dirCombo.getValue()) ? ValuablesRecord.DIRECTION_OUT : ValuablesRecord.DIRECTION_IN;
            try {
                dormService.registerValuables(staff, studentField.getText(), itemField.getText(), direction);
                AlertUtil.info("已登记贵重物品出入记录。");
                renderValuables(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        HBox studentRow = new HBox(10, new Label("学生："), studentField, query, preview);
        studentRow.setAlignment(Pos.CENTER_LEFT);
        HBox itemRow = new HBox(10, new Label("物品："), itemField,
                new Label("方向："), dirCombo, register);
        itemRow.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().addAll(studentRow, itemRow);

        body.getChildren().add(sectionTitle("本楼栋贵重物品出入记录"));
        TableView<ValuablesRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, ValuablesRecord::getId));
        table.getColumns().add(col("学号", 90, ValuablesRecord::getStudentId));
        table.getColumns().add(col("姓名·宿舍", 200, r -> occupantLabel(r.getStudentId())));
        table.getColumns().add(col("物品", 140, ValuablesRecord::getItemName));
        table.getColumns().add(col("方向", 70, ValuablesRecord::getDirectionName));
        table.getColumns().add(col("登记时间", 150, ValuablesRecord::getRecordTime));
        table.getColumns().add(col("学生确认", 100, ValuablesRecord::getConfirmStatusName));
        table.getColumns().add(col("遗失上报", 110, ValuablesRecord::getReportStatusName));
        table.setPrefHeight(300);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(dormService.valuablesOfStaff(staff)));
        body.getChildren().add(table);
    }

    // ==================== 遗失上报（宿管 → 宿管科） ====================

    /**
     * 遗失上报页：汇总分管楼栋内学生已报遗失的贵重物品，核实后上报宿管科；
     * 宿管科在「遗失公告」页审核发布全校公告。
     */
    public Node buildLostEscalation() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("遗失上报（提交宿管科）");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        final VBox body = new VBox(12);
        box.getChildren().add(body);
        renderLostEscalation(body);
        return wrap(box);
    }

    private void renderLostEscalation(final VBox body) {
        body.getChildren().clear();
        body.getChildren().add(note("学生在「贵重物品确认」中报告遗失后，记录会出现在这里。"
                + "请核实情况后上报宿管科，由宿管科统一发布全校公告。"));

        List<ValuablesRecord> pending = dormService.pendingEscalateOfStaff(staff);
        Label pendingTitle = sectionTitle("待上报（" + pending.size() + " 条）");
        body.getChildren().add(pendingTitle);

        final TableView<ValuablesRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, ValuablesRecord::getId));
        table.getColumns().add(col("学号", 90, ValuablesRecord::getStudentId));
        table.getColumns().add(col("姓名·宿舍", 200, r -> occupantLabel(r.getStudentId())));
        table.getColumns().add(col("物品", 140, ValuablesRecord::getItemName));
        table.getColumns().add(col("登记时间", 150, ValuablesRecord::getRecordTime));
        table.getColumns().add(col("报损时间", 150, ValuablesRecord::getConfirmTime));
        table.getColumns().add(col("学生说明", 220, r -> safe(r.getStudentRemark())));
        table.setPrefHeight(260);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(pending));
        body.getChildren().add(table);

        Button escalateOne = button("上报选中记录", "#27ae60", e -> {
            ValuablesRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.warn("请先在表格中选择一条待上报记录");
                return;
            }
            if (!AlertUtil.confirm("确定把「" + selected.getItemName() + "」（" + selected.getStudentId()
                    + "）的遗失情况上报宿管科吗？")) {
                return;
            }
            try {
                dormService.escalate(selected, staff);
                AlertUtil.info("已上报宿管科，等待宿管科发布公告。");
                renderLostEscalation(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        Button escalateAll = button("一键上报全部待上报", "#2c3e50", e -> {
            int count = dormService.pendingEscalateOfStaff(staff).size();
            if (count == 0) {
                AlertUtil.info("当前没有待上报的遗失记录。");
                return;
            }
            if (!AlertUtil.confirm("确定把全部 " + count + " 条遗失记录上报宿管科吗？"
                    + "（限本人分管楼栋范围）")) {
                return;
            }
            int done = dormService.escalateLostValuables(staff);
            AlertUtil.info("已上报 " + done + " 条遗失记录，等待宿管科发布公告。");
            renderLostEscalation(body);
        });

        HBox actions = new HBox(10, escalateOne, escalateAll);
        actions.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().add(actions);

        body.getChildren().add(sectionTitle("已上报记录"));
        List<ValuablesRecord> escalated = dormService.escalatedOfStaff(staff);
        if (escalated.isEmpty()) {
            body.getChildren().add(note("暂无已上报的遗失记录。"));
            return;
        }
        TableView<ValuablesRecord> history = new TableView<>();
        history.getColumns().add(col("编号", 80, ValuablesRecord::getId));
        history.getColumns().add(col("学号", 90, ValuablesRecord::getStudentId));
        history.getColumns().add(col("物品", 150, ValuablesRecord::getItemName));
        history.getColumns().add(col("上报进度", 110, ValuablesRecord::getReportStatusName));
        history.getColumns().add(col("上报时间", 150, r -> safe(r.getReportTime())));
        history.getColumns().add(col("上报人", 130, r -> personLabel(r.getReporterId())));
        history.setPrefHeight(220);
        history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        history.setItems(FXCollections.observableArrayList(escalated));
        body.getChildren().add(history);
    }

    // ==================== 报修处理（宿管 → 宿管科） ====================

    /**
     * 报修处理页：接收分管楼栋内学生提交的报修单，核实后统一上报宿管科；
     * 宿管科在「维修管理」页受理并办结。学生报修不再直达宿管科。
     */
    public Node buildRepairHandling() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("报修处理（上报宿管科）");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        final VBox body = new VBox(12);
        box.getChildren().add(body);
        renderRepairHandling(body);
        return wrap(box);
    }

    private void renderRepairHandling(final VBox body) {
        body.getChildren().clear();
        body.getChildren().add(note("学生在「维修报修」中提交后，工单会出现在这里。"
                + "请核实情况后上报宿管科，由宿管科安排维修并办结。"));

        List<RepairTicket> pending = repairService.pendingEscalateOfStaff(staff);
        body.getChildren().add(sectionTitle("待上报（" + pending.size() + " 条）"));

        final TableView<RepairTicket> table = new TableView<>();
        table.getColumns().add(col("单号", 80, RepairTicket::getId));
        table.getColumns().add(col("房间", 100, RepairTicket::getRoomKey));
        table.getColumns().add(col("学号", 90, RepairTicket::getReporterId));
        table.getColumns().add(col("姓名·宿舍", 200, r -> occupantLabel(r.getReporterId())));
        table.getColumns().add(col("维修内容", 260, r -> safe(r.getDescription())));
        table.getColumns().add(col("提交时间", 150, RepairTicket::getCreateTime));
        table.setPrefHeight(260);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(pending));
        body.getChildren().add(table);

        Button escalateOne = button("上报选中工单", "#27ae60", e -> {
            RepairTicket selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.warn("请先在表格中选择一条待上报工单");
                return;
            }
            if (!AlertUtil.confirm("确定把工单 " + selected.getId() + "（" + selected.getRoomKey()
                    + "）上报宿管科吗？")) {
                return;
            }
            try {
                repairService.escalate(selected, staff);
                AlertUtil.info("已上报宿管科，等待宿管科受理。");
                renderRepairHandling(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        Button escalateAll = button("一键上报全部待上报", "#2c3e50", e -> {
            int count = repairService.pendingEscalateOfStaff(staff).size();
            if (count == 0) {
                AlertUtil.info("当前没有待上报的报修单。");
                return;
            }
            if (!AlertUtil.confirm("确定把全部 " + count + " 张报修单上报宿管科吗？"
                    + "（限本人分管楼栋范围）")) {
                return;
            }
            int done = repairService.escalateAll(staff);
            AlertUtil.info("已上报 " + done + " 张报修单，等待宿管科受理。");
            renderRepairHandling(body);
        });

        HBox actions = new HBox(10, escalateOne, escalateAll);
        actions.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().add(actions);

        body.getChildren().add(sectionTitle("已上报工单"));
        List<RepairTicket> escalated = new ArrayList<>();
        for (RepairTicket t : repairService.repairTicketsOfStaff(staff)) {
            if (!t.isSubmitted()) {
                escalated.add(t);
            }
        }
        if (escalated.isEmpty()) {
            body.getChildren().add(note("暂无已上报的报修单。"));
            return;
        }
        TableView<RepairTicket> history = new TableView<>();
        history.getColumns().add(col("单号", 80, RepairTicket::getId));
        history.getColumns().add(col("房间", 100, RepairTicket::getRoomKey));
        history.getColumns().add(col("维修内容", 240, r -> safe(r.getDescription())));
        history.getColumns().add(col("进度", 100, RepairTicket::getStatusName));
        history.getColumns().add(col("上报时间", 150, r -> safe(r.getEscalateTime())));
        history.getColumns().add(col("上报人", 130, r -> personLabel(r.getEscalatorId())));
        history.setPrefHeight(220);
        history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        history.setItems(FXCollections.observableArrayList(escalated));
        body.getChildren().add(history);
    }

    // ==================== 弹窗 ====================

    /**
     * 弹窗收集一段复核意见。取消返回 null（调用方据此中止操作），留空返回空串（复核意见可选）。
     */
    private String askReviewComment(String header, String prompt) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("填写复核意见");
        dialog.setHeaderText(header);
        ButtonType okType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(3);
        area.setPrefColumnCount(34);
        VBox content = new VBox(8, new Label("复核意见："), area);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        UI.applyToDialog(dialog.getDialogPane());

        dialog.setResultConverter(button -> button == okType ? area.getText() : null);
        Platform.runLater(area::requestFocus);

        Optional<String> result = dialog.showAndWait();
        return result.isPresent() ? result.get() : null;
    }

    /**
     * 夜归「修改后通过」弹窗：预填原登记内容，宿管改正日期 / 时间 / 原因后复核通过。
     * 取消返回 null。
     */
    private NightAdjust askNightAdjust(NightReturnRecord record) {
        Dialog<NightAdjust> dialog = new Dialog<>();
        dialog.setTitle("更正夜归登记");
        dialog.setHeaderText("更正夜归记录 " + record.getId() + "（学生：" + occupantLabel(record.getStudentId())
                + "）");
        ButtonType okType = new ButtonType("复核通过", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        DatePicker datePicker = DatePickers.create(record.getDate());
        TextField timeField = new TextField(safe(record.getReturnTime()));
        timeField.setPromptText("夜归时间，如 23:30");
        TextField reasonField = new TextField(safe(record.getReason()));
        reasonField.setPromptText("夜归原因（可空）");
        TextArea commentField = new TextArea();
        commentField.setPromptText("复核意见：说明更正了哪些内容及依据");
        commentField.setPrefRowCount(3);
        commentField.setPrefColumnCount(34);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0, new Label("日期："), datePicker);
        grid.addRow(1, new Label("时间："), timeField);
        grid.addRow(2, new Label("原因："), reasonField);
        grid.addRow(3, new Label("复核意见："), commentField);
        VBox content = new VBox(8,
                new Label("学生异议：" + safe(record.getStudentRemark())), grid);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        UI.applyToDialog(dialog.getDialogPane());

        dialog.setResultConverter(button -> button == okType
                ? new NightAdjust(DatePickers.textOf(datePicker), timeField.getText(),
                        reasonField.getText(), commentField.getText())
                : null);
        Platform.runLater(datePicker::requestFocus);

        Optional<NightAdjust> result = dialog.showAndWait();
        return result.isPresent() ? result.get() : null;
    }

    /**
     * 卫生「更正后通过」弹窗：预填原得分 / 评语，宿管改正后复核通过。取消返回 null。
     */
    private HygieneAdjust askHygieneAdjust(HygieneRecord record) {
        Dialog<HygieneAdjust> dialog = new Dialog<>();
        dialog.setTitle("更正卫生检查记录");
        dialog.setHeaderText("更正卫生检查 " + record.getId() + "（房间：" + safe(record.getRoomKey()) + "）");
        ButtonType okType = new ButtonType("复核通过", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextField scoreField = new TextField(String.valueOf((int) record.getScore()));
        scoreField.setPromptText("得分 0~100");
        TextField commentField = new TextField(safe(record.getComment()));
        commentField.setPromptText("评语（可空）");
        commentField.setPrefWidth(240);
        TextArea reviewField = new TextArea();
        reviewField.setPromptText("复核意见：说明更正了哪些内容及依据");
        reviewField.setPrefRowCount(3);
        reviewField.setPrefColumnCount(34);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0, new Label("得分："), scoreField);
        grid.addRow(1, new Label("评语："), commentField);
        grid.addRow(2, new Label("复核意见："), reviewField);
        VBox content = new VBox(8,
                new Label("学生反馈：" + safe(record.getStudentRemark())), grid);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        UI.applyToDialog(dialog.getDialogPane());

        dialog.setResultConverter(button -> {
            if (button != okType) {
                return null;
            }
            double score;
            try {
                score = Double.parseDouble(scoreField.getText().trim());
            } catch (NumberFormatException ex) {
                score = Double.NaN;
            }
            return new HygieneAdjust(score, commentField.getText(), reviewField.getText());
        });
        Platform.runLater(scoreField::requestFocus);

        Optional<HygieneAdjust> result = dialog.showAndWait();
        return result.isPresent() ? result.get() : null;
    }

    /** 夜归「修改后通过」弹窗的输入结果。 */
    private static class NightAdjust {
        private final String date;
        private final String time;
        private final String reason;
        private final String comment;

        NightAdjust(String date, String time, String reason, String comment) {
            this.date = date;
            this.time = time;
            this.reason = reason;
            this.comment = comment;
        }
    }

    /** 卫生「更正后通过」弹窗的输入结果。 */
    private static class HygieneAdjust {
        private final double score;
        private final String comment;
        private final String reviewComment;

        HygieneAdjust(double score, String comment, String reviewComment) {
            this.score = score;
            this.comment = comment;
            this.reviewComment = reviewComment;
        }
    }

    // ==================== 工具 ====================

    /** 区块之间的分隔线。 */
    private Node divider() {
        javafx.scene.control.Separator line = new javafx.scene.control.Separator();
        line.setPadding(new Insets(10, 0, 2, 0));
        return line;
    }

    private String occupantLabel(String studentId) {
        Student s = DataCenter.instance().findStudentById(studentId);
        if (s == null) {
            return studentId;
        }
        return s.getName() + "（" + s.getId() + "）"
                + (s.isCheckedIn() ? "·" + s.getCurrentBuilding() + "-" + s.getCurrentRoom() : "·未入住");
    }

    private String personLabel(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        com.nchu.dorm.model.Person p = DataCenter.instance().findPersonById(id);
        return p == null ? id : p.getName() + "（" + id + "）";
    }

    private void addInfoRows(VBox box, String[][] rows) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);
        for (int i = 0; i < rows.length; i++) {
            Label key = new Label(rows[i][0] + "：");
            UI.style(key, UI.KEY);
            grid.addRow(i, key, new Label(rows[i][1]));
        }
        box.getChildren().add(grid);
    }

    private Label note(String text) {
        Label l = new Label(text);
        UI.style(l, UI.MUTED);
        l.setWrapText(true);
        return l;
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        UI.style(l, UI.SECTION);
        return l;
    }

    private Button button(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        UI.style(b, UI.BTN, UI.variantClass(color));
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
        UI.style(sp, UI.PAGE_SCROLL);
        return sp;
    }
}

package com.nchu.dorm.ui;

import com.nchu.dorm.model.Bed;
import com.nchu.dorm.model.Counselor;
import com.nchu.dorm.model.Person;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.announcement.Announcement;
import com.nchu.dorm.model.application.DormApplication;
import com.nchu.dorm.model.application.ElectricityPurchase;
import com.nchu.dorm.model.application.RepairTicket;
import com.nchu.dorm.model.record.HygieneRecord;
import com.nchu.dorm.model.record.NightReturnRecord;
import com.nchu.dorm.model.record.ValuablesRecord;
import com.nchu.dorm.service.CounselorService;
import com.nchu.dorm.service.DormApplicationService;
import com.nchu.dorm.service.RoomAssignmentService;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.ui.component.AlertUtil;
import com.nchu.dorm.ui.component.UI;
import com.nchu.dorm.util.BusinessException;
import com.nchu.dorm.util.FormatUtil;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 辅导员端视图：宿舍审批（待处理 + 历史）与设置（改名 / 换头像 / 改密码）。
 * 左侧按范围筛选（待处理/已通过/已驳回/已撤销/全部）列出申请；
 * 右侧面板按申请类型与所处阶段自适应：
 * <ul>
 *   <li>入住/转宿（PENDING）：目标楼栋空床房间下拉 + 通过分配 / 驳回；</li>
 *   <li>退宿（PENDING）：红字风险提示，无房下拉，通过退宿 / 驳回；</li>
 *   <li>转专业换宿（PENDING，本专业辅导员）：同意迁出 / 驳回；</li>
 *   <li>转专业换宿（AWAITING_TARGET，目标辅导员）：跨学院选房接收 / 同学院直接接收 / 驳回；</li>
 *   <li>历史：只读查看，含两级审批痕迹。</li>
 * </ul>
 * <p>
 * 除审批外，辅导员对本专业（本年级）学生的购电、维修报修、夜归、卫生检查得分、贵重物品确认，
 * 以及全校公告有<b>查看权</b>（见 {@code buildElectricity} 等只读页面）；这些页面不含任何修改入口，
 * 记录的变更仍由学生本人或宿管 / 宿管科发起（规则与查询范围见 {@link CounselorService}）。
 * </p>
 */
public class CounselorView {

    private final Counselor counselor;
    private final DormApplicationService applicationService = new DormApplicationService();
    private final CounselorService counselorService = new CounselorService();

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
        UI.style(title, UI.PAGE_TITLE);
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
        // 窗口变窄时优先压缩表格，把空间让给右侧审批面板
        table.setMinWidth(240);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> showDetail(newVal));
        VBox.setVgrow(table, Priority.ALWAYS);

        // ---- 右侧：审批面板 ----
        // 面板内容（详情/选房/按钮/意见框）高度不定，套一层滚动容器：
        // 既保证按钮不被压扁截断，也保证面板宽度不被表格挤掉。
        rightBox = new VBox(10);
        rightBox.setPadding(new Insets(16));
        rightBox.setFillWidth(true);

        ScrollPane panelScroll = new ScrollPane(rightBox);
        panelScroll.setFitToWidth(true);
        panelScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panelScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        panelScroll.setPrefWidth(360);
        panelScroll.setMinWidth(320);
        UI.style(panelScroll, UI.PANEL_SCROLL);

        commentArea = new TextArea();
        commentArea.setPromptText("审批意见（可选）");
        commentArea.setPrefRowCount(3);

        HBox content = new HBox(14, table, panelScroll);
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
            UI.style(l, UI.FAINT);
            rightBox.getChildren().add(l);
            return;
        }

        Label detail = new Label(detailText(app));
        detail.setWrapText(true);
        UI.style(detail, UI.DETAIL);
        rightBox.getChildren().add(detail);

        boolean editable = "待处理".equals(filterCombo.getValue());
        if (editable) {
            addActionControls(app);
        } else {
            Label readonly = new Label("（历史记录，仅供查看）");
            UI.style(readonly, UI.FAINT);
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
                UI.style(l, UI.WARN);
                l.setWrapText(true);
                rightBox.getChildren().add(l);
                rightBox.getChildren().add(buttonRow(button("同意迁出", "#27ae60", e -> doMajorMoveOut(app)),
                        button("驳回申请", "#e74c3c", e -> doReject(app))));
            } else if (DormApplication.STATUS_AWAITING_TARGET.equals(status)) {
                Label l = new Label("本专业辅导员已同意迁出，现由您（目标专业辅导员）接收。");
                UI.style(l, UI.INK);
                l.setWrapText(true);
                rightBox.getChildren().add(l);

                String targetBuilding = app.getTargetBuilding();
                boolean needRoom = targetBuilding != null && !targetBuilding.isEmpty();
                if (needRoom) {
                    // 排序须用"目标班级"：学生档案是占床之后才改写为转专业目标的，此时仍是原班级。
                    Student student = DataCenter.instance().findStudentById(app.getStudentId());
                    // 仅当目标楼栋就是现居楼栋时才排除现居房间（房号跨楼栋重复，不可无条件排除）
                    String excludeRoomNo = student != null && targetBuilding.equals(student.getCurrentBuilding())
                            ? student.getCurrentRoom() : null;
                    Label preview = new Label();
                    preview.setWrapText(true);
                    UI.style(preview, UI.OK);
                    ComboBox<String> rooms = roomCombo(targetBuilding, app.getTargetClass(), excludeRoomNo, preview);
                    rightBox.getChildren().add(new Label(
                            "接收房间（目标楼栋 " + targetBuilding + "，按同班/同专业优先排序）："));
                    rightBox.getChildren().add(rooms);
                    rightBox.getChildren().add(preview);
                    rightBox.getChildren().add(buttonRow(
                            button("通过并接收", "#27ae60", e -> doMajorAccept(app, rooms.getValue())),
                            button("驳回申请", "#e74c3c", e -> doReject(app))));
                } else {
                    Label noMove = new Label("目标学院与当前学院相同，不更换宿舍，仅更新专业/班级档案。");
                    noMove.setWrapText(true);
                    UI.style(noMove, UI.MUTED);
                    rightBox.getChildren().add(noMove);
                    rightBox.getChildren().add(buttonRow(
                            button("通过并接收", "#27ae60", e -> doMajorAccept(app, null)),
                            button("驳回申请", "#e74c3c", e -> doReject(app))));
                }
            }
            return;
        }

        if (DormApplication.TYPE_EXIT.equals(type)) {
            Label risk = new Label("通过后将释放该生当前床位并置为未入住。");
            UI.style(risk, UI.DANGER);
            risk.setWrapText(true);
            rightBox.getChildren().add(risk);
            rightBox.getChildren().add(buttonRow(button("通过并退宿", "#27ae60", e -> doApproveExit(app)),
                    button("驳回申请", "#e74c3c", e -> doReject(app))));
            return;
        }

        // 入住 / 转宿：同样按"同班 > 同专业 > 同学院"推荐排序，避免把学生拆散
        String building = app.getTargetBuilding();
        Student student = DataCenter.instance().findStudentById(app.getStudentId());
        String targetClass = student == null ? null : student.getClassName();
        String excludeRoomNo = student != null && building != null && building.equals(student.getCurrentBuilding())
                ? student.getCurrentRoom() : null;
        Label preview = new Label();
        preview.setWrapText(true);
        UI.style(preview, UI.OK);
        ComboBox<String> rooms = roomCombo(building, targetClass, excludeRoomNo, preview);
        rightBox.getChildren().add(new Label("分配房间（目标楼栋 " + safe(building) + "，按同班/同专业优先排序）："));
        rightBox.getChildren().add(rooms);
        rightBox.getChildren().add(preview);
        rightBox.getChildren().add(buttonRow(button("通过并分配宿舍", "#27ae60", e -> doApprove(app, rooms.getValue())),
                button("驳回申请", "#e74c3c", e -> doReject(app))));
    }

    /**
     * 分配房间下拉：按"同班 &gt; 同专业 &gt; 同学院"推荐排序，并默认选中推荐房间（辅导员仍可改选）。
     * 选中项变化时同步刷新 {@code preview} 中的室友信息。
     */
    private ComboBox<String> roomCombo(String buildingName, String targetClass,
                                       String excludeRoomNo, Label preview) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setPrefWidth(280);
        for (RoomAssignmentService.Suggestion s : RoomAssignmentService.suggestRooms(buildingName, targetClass, excludeRoomNo)) {
            combo.getItems().add(roomItem(s));
        }
        if (!combo.getItems().isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
        combo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> preview.setText(previewText(buildingName, newVal)));
        preview.setText(previewText(buildingName, combo.getValue()));
        return combo;
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

    // ---------- 设置 ----------

    /**
     * 设置页：辅导员同样可自助更换头像、修改姓名与登录密码（与学生共用 {@link SettingsView}）。
     * 姓名默认与工号一致，改过名后顶栏问候语直接显示本人姓名，未改名则显示「xx专业辅导员」。
     *
     * @param onProfileChanged 头像/姓名修改成功后的回调（主框架据此刷新顶栏头像与问候语），可为 null
     */
    public Node buildSettings(Runnable onProfileChanged) {
        return new SettingsView(counselor, "工号",
                "提示：姓名默认与工号一致，可自行修改；工号是账号的唯一标识，不可更改。")
                .build(onProfileChanged);
    }

    // ---------- 本专业学生记录（只读） ----------
    // 辅导员对本专业学生的六类内容只有查看权：页面不放任何按钮 / 输入框，
    // 数据变更一律由学生本人或宿管 / 宿管科在各自端发起。

    /** 购电记录页：本专业学生的购电单与售电进度（缴费售电由宿管科办理）。 */
    public Node buildElectricity() {
        VBox box = readOnlyPage("购电记录",
                "以下为本专业学生提交的购电单及其售电进度；收费与售电由宿管科办理。");
        List<ElectricityPurchase> records = counselorService.electricityOfCounselor(counselor);
        int pending = 0;
        for (ElectricityPurchase p : records) {
            if (p.isPending()) {
                pending++;
            }
        }
        box.getChildren().add(summaryLabel("共 " + records.size() + " 条购电记录，其中待售电 " + pending + " 单。"));

        TableView<ElectricityPurchase> table = new TableView<>();
        table.getColumns().add(col("单号", 90, ElectricityPurchase::getId));
        table.getColumns().add(col("学号", 110, ElectricityPurchase::getBuyerId));
        table.getColumns().add(col("姓名", 100, p -> studentName(p.getBuyerId())));
        table.getColumns().add(col("房间", 110, ElectricityPurchase::getRoomKey));
        table.getColumns().add(col("购电(度)", 90, p -> FormatUtil.num(p.getDegree())));
        table.getColumns().add(col("应付(元)", 90, p -> FormatUtil.money(p.getAmount())));
        table.getColumns().add(col("状态", 90, ElectricityPurchase::getStatusName));
        table.getColumns().add(col("提交时间", 150, ElectricityPurchase::getCreateTime));
        table.getColumns().add(col("售电时间", 150, p -> safe(p.getHandleTime())));
        table.getColumns().add(col("经办", 120, p -> personLabel(p.getHandlerId())));
        box.getChildren().add(readOnlyTable(table, records));
        addEmptyHint(box, records);
        return wrap(box);
    }

    /** 报修记录页：本专业学生提交的维修工单及其处理进度。 */
    public Node buildRepairs() {
        VBox box = readOnlyPage("报修记录",
                "以下为本专业学生提交的维修工单；工单先由分管宿管核实上报，再由宿管科受理、办结。");
        List<RepairTicket> records = counselorService.repairsOfCounselor(counselor);
        int done = 0;
        for (RepairTicket t : records) {
            if (RepairTicket.STATUS_DONE.equals(t.getStatus())) {
                done++;
            }
        }
        box.getChildren().add(summaryLabel("共 " + records.size() + " 张工单，其中未办结 "
                + (records.size() - done) + " 张、已完成 " + done + " 张。"));

        TableView<RepairTicket> table = new TableView<>();
        table.getColumns().add(col("单号", 90, RepairTicket::getId));
        table.getColumns().add(col("学号", 110, RepairTicket::getReporterId));
        table.getColumns().add(col("姓名", 100, t -> studentName(t.getReporterId())));
        table.getColumns().add(col("房间", 110, RepairTicket::getRoomKey));
        table.getColumns().add(col("维修内容", 240, RepairTicket::getDescription));
        table.getColumns().add(col("状态", 100, RepairTicket::getStatusName));
        table.getColumns().add(col("提交时间", 150, RepairTicket::getCreateTime));
        table.getColumns().add(col("上报时间", 150, t -> safe(t.getEscalateTime())));
        table.getColumns().add(col("处理人", 120, t -> personLabel(t.getHandlerId())));
        table.getColumns().add(col("处理时间", 150, t -> safe(t.getHandleTime())));
        box.getChildren().add(readOnlyTable(table, records));
        addEmptyHint(box, records);
        return wrap(box);
    }

    /** 夜归记录页：宿管登记的本专业学生夜归情况与学生确认结果。 */
    public Node buildNightReturns() {
        VBox box = readOnlyPage("夜归记录",
                "以下为宿管登记的本专业学生夜归情况，以及学生在学生端的确认 / 异议结果。");
        List<NightReturnRecord> records = counselorService.nightReturnsOfCounselor(counselor);
        int pending = 0;
        for (NightReturnRecord r : records) {
            if (r.isPendingConfirm()) {
                pending++;
            }
        }
        box.getChildren().add(summaryLabel("共 " + records.size() + " 条夜归记录，其中待学生确认 " + pending + " 条。"));

        TableView<NightReturnRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, NightReturnRecord::getId));
        table.getColumns().add(col("学号", 110, NightReturnRecord::getStudentId));
        table.getColumns().add(col("姓名", 100, r -> studentName(r.getStudentId())));
        table.getColumns().add(col("日期", 110, NightReturnRecord::getDate));
        table.getColumns().add(col("夜归时间", 100, NightReturnRecord::getReturnTime));
        table.getColumns().add(col("登记原因", 200, r -> safe(r.getReason())));
        table.getColumns().add(col("确认状态与说明", 320, NightReturnRecord::getConfirmSummary));
        box.getChildren().add(readOnlyTable(table, records));
        addEmptyHint(box, records);
        return wrap(box);
    }

    /** 卫生检查页：本专业学生所在寝室的评分与评语（卫生检查按房间登记，故按寝室列出）。 */
    public Node buildHygiene() {
        VBox box = readOnlyPage("卫生检查记录",
                "卫生检查按寝室登记，故此处列出本专业学生所住寝室的历次评分与评语，以及学生的反馈结果。");
        List<HygieneRecord> records = counselorService.hygieneRecordsOfCounselor(counselor);
        double average = counselorService.averageHygieneScoreOfCounselor(counselor);
        box.getChildren().add(summaryLabel("共 " + records.size() + " 次检查记录"
                + (average < 0 ? "" : "，平均分 " + FormatUtil.num(average) + " 分") + "。"));

        Map<String, String> roomLabels = counselorService.roomStudentLabels(counselor);
        TableView<HygieneRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, HygieneRecord::getId));
        table.getColumns().add(col("寝室", 110, HygieneRecord::getRoomKey));
        table.getColumns().add(col("本专业学生", 200, r -> safe(roomLabels.get(r.getRoomKey()))));
        table.getColumns().add(col("检查日期", 110, HygieneRecord::getDate));
        table.getColumns().add(col("得分", 70, r -> String.valueOf((int) r.getScore())));
        table.getColumns().add(col("检查人", 130, r -> personLabel(r.getInspectorId())));
        table.getColumns().add(col("评语", 220, r -> safe(r.getComment())));
        table.getColumns().add(col("学生反馈", 240, HygieneRecord::getStudentSummary));
        box.getChildren().add(readOnlyTable(table, records));
        addEmptyHint(box, records);
        return wrap(box);
    }

    /** 贵重物品记录页：本专业学生携带贵重物品进出楼栋的登记与学生确认 / 遗失上报进度。 */
    public Node buildValuables() {
        VBox box = readOnlyPage("贵重物品记录",
                "以下为宿管登记的本专业学生贵重物品出入情况，以及学生的确认结果与遗失上报进度。");
        List<ValuablesRecord> records = counselorService.valuablesOfCounselor(counselor);
        int pending = 0;
        int reported = 0;
        for (ValuablesRecord r : records) {
            if (ValuablesRecord.CONFIRM_PENDING.equals(r.getConfirmStatus())) {
                pending++;
            }
            if (!ValuablesRecord.REPORT_NONE.equals(r.getReportStatus())) {
                reported++;
            }
        }
        box.getChildren().add(summaryLabel("共 " + records.size() + " 条记录，其中待学生确认 " + pending
                + " 件，涉及遗失上报 " + reported + " 件。"));

        TableView<ValuablesRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, ValuablesRecord::getId));
        table.getColumns().add(col("学号", 110, ValuablesRecord::getStudentId));
        table.getColumns().add(col("姓名", 100, r -> studentName(r.getStudentId())));
        table.getColumns().add(col("物品", 160, ValuablesRecord::getItemName));
        table.getColumns().add(col("方向", 70, ValuablesRecord::getDirectionName));
        table.getColumns().add(col("登记时间", 150, ValuablesRecord::getRecordTime));
        table.getColumns().add(col("登记人", 130, r -> personLabel(r.getHandlerId())));
        table.getColumns().add(col("确认状态", 90, ValuablesRecord::getConfirmStatusName));
        table.getColumns().add(col("上报进度", 110, ValuablesRecord::getReportStatusName));
        table.getColumns().add(col("学生说明", 200, r -> safe(r.getStudentRemark())));
        box.getChildren().add(readOnlyTable(table, records));
        addEmptyHint(box, records);
        return wrap(box);
    }

    /** 公告页：宿管科发布的全校公告（面向全校，不按专业过滤）。 */
    public Node buildAnnouncements() {
        VBox box = readOnlyPage("公告",
                "以下为宿管科面向全校发布的公告（当前为物品遗失通报）。");
        List<Announcement> list = counselorService.announcements();
        if (list.isEmpty()) {
            Label empty = new Label("暂无公告。");
            UI.style(empty, UI.MUTED);
            box.getChildren().add(empty);
            return wrap(box);
        }
        for (Announcement a : list) {
            box.getChildren().add(announcementCard(a));
        }
        return wrap(box);
    }

    /** 单条公告卡片：标题 + 类型 + 正文 + 发布人/时间（正文自动换行）。 */
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

    // ---------- 只读页面排版 ----------

    /**
     * 只读页面的公共骨架：标题 + 查询范围（专业年级 / 学生人数）+ 只读说明。
     * 范围称谓见 {@link CounselorService#scopeName}，与列表的过滤口径同源。
     */
    private VBox readOnlyPage(String titleText, String intro) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setMaxWidth(1180);

        Label title = new Label(titleText);
        UI.style(title, UI.PAGE_TITLE);

        Label scope = new Label("查询范围：" + counselorService.scopeName(counselor)
                + " · 共 " + counselorService.studentsOf(counselor).size() + " 名学生");
        UI.style(scope, UI.SECTION);

        Label note = new Label(intro + "（本页仅供查看，不能修改。）");
        UI.style(note, UI.MUTED);
        note.setWrapText(true);

        box.getChildren().addAll(title, scope, note);
        return box;
    }

    /**
     * 只读列表：填入数据，并按各列 prefWidth 保留列宽（宽表出横向滚动条），
     * 而不是被压缩到文字显示不全——{@code CONSTRAINED_RESIZE_POLICY} 会把 10 列硬挤进面板宽度，
     * "提交时间" 一类长文本会被省略号截断。
     * <p>数据由本方法一并绑定：漏绑 {@code setItems} 时表格空白、统计行却有数字，
     * 看起来像功能故障，故把 items 定为必填参数，从签名上杜绝漏绑。</p>
     */
    private <T> TableView<T> readOnlyTable(TableView<T> table, List<T> items) {
        table.setItems(FXCollections.observableArrayList(items));
        table.setPrefHeight(400);
        table.setMinWidth(240);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        return table;
    }

    /** 范围内无记录时补一行占位提示：空表格 + 只显示「共 0 条」容易被误读成功能故障。 */
    private void addEmptyHint(VBox box, List<?> records) {
        if (!records.isEmpty()) {
            return;
        }
        Label l = new Label("本专业（" + counselorService.scopeName(counselor) + "）暂无此类记录。");
        UI.style(l, UI.MUTED);
        l.setWrapText(true);
        box.getChildren().add(l);
    }

    /** 表格上方的统计行。 */
    private Label summaryLabel(String text) {
        Label l = new Label(text);
        UI.style(l, UI.FAINT);
        return l;
    }

    /** 人员标签：姓名（工号/学号）；查不到时退回编号原文。 */
    private String personLabel(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        Person p = DataCenter.instance().findPersonById(id);
        return p == null ? id : p.getName() + "（" + id + "）";
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
        // 辅导员姓名默认等于工号，未改名时显示「xx专业辅导员」，改过名则显示本人姓名
        return c == null ? id : counselorService.displayName(c);
    }

    private String originText(DormApplication app) {
        if (app.getOriginBuilding() == null && app.getOriginRoom() == null) {
            return "";
        }
        return safe(app.getOriginBuilding()) + "-" + safe(app.getOriginRoom());
    }

    /**
     * 房间下拉条目，形如 "20A栋-205（余3床·同班）"。
     * 前缀必须保持"楼栋-房号（…）"的形式，{@link #parseRoom} 依赖它切分楼栋与房号。
     */
    private String roomItem(RoomAssignmentService.Suggestion s) {
        return s.getRoom().displayKey() + "（余" + s.getRoom().availableBedCount() + "床·" + s.tag() + "）";
    }

    /** 推荐预览：所选房间的现有室友，便于辅导员确认学生能融入同班/同专业。 */
    private String previewText(String buildingName, String item) {
        if (item == null || item.isEmpty()) {
            return buildingName == null || buildingName.isEmpty() ? "" : "该楼栋暂无可分配的空床房间";
        }
        Room room = DataCenter.instance().findRoom(buildingName, parseRoom(item)[1]);
        if (room == null) {
            return "";
        }
        List<String> mates = new ArrayList<>();
        for (Bed b : room.getBeds()) {
            if (b.getOccupantId() != null) {
                mates.add(studentName(b.getOccupantId()));
            }
        }
        return mates.isEmpty()
                ? "推荐 " + room.displayKey() + "：整间空房，暂无室友"
                : "推荐 " + room.displayKey() + "　现有室友：" + String.join("、", mates);
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

    /** 操作用 FlowPane：面板偏窄时按钮换行，而不是被压缩到文字显示不全。 */
    private FlowPane buttonRow(Node... nodes) {
        FlowPane row = new FlowPane(10, 8, nodes);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button button(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        UI.style(b, UI.BTN, UI.variantClass(color));
        // 禁止布局把按钮压到小于文字所需宽度（否则文字会被省略号截断）
        b.setMinWidth(Region.USE_PREF_SIZE);
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

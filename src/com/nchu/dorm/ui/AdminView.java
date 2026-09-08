package com.nchu.dorm.ui;

import com.nchu.dorm.model.Admin;
import com.nchu.dorm.model.Building;
import com.nchu.dorm.model.College;
import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.application.ElectricityPurchase;
import com.nchu.dorm.model.application.RepairTicket;
import com.nchu.dorm.service.AdminService;
import com.nchu.dorm.service.ElectricityService;
import com.nchu.dorm.service.RepairService;
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
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 宿管科视图：工作台概览 / 楼栋分配 / 宿舍管理人员管理 / 修理管理 / 售电。
 * 迭代五落地宿管科四大职责，闭环逻辑收口在 {@link AdminService}、{@link RepairService}、
 * {@link ElectricityService}。
 */
public class AdminView {

    private final Admin admin;
    private final AdminService adminService = new AdminService();
    private final RepairService repairService = new RepairService();
    private final ElectricityService electricityService = new ElectricityService();

    // ---- 楼栋分配组件 ----
    private TableView<Building> buildingTable;
    private ComboBox<String> buildingFilter;
    private VBox buildingRight;

    // ---- 宿舍管理人员管理组件 ----
    private TableView<DormStaff> staffTable;
    private VBox staffRight;
    private DormStaff currentStaff;
    private TextField staffIdField;
    private TextField staffNameField;
    private ComboBox<String> staffGenderCombo;
    private TextField staffPhoneField;
    private TextField staffJobField;
    private ListView<String> staffBuildings;
    private Label staffAccountLabel;

    // ---- 修理管理组件 ----
    private TableView<RepairTicket> repairTable;
    private ComboBox<String> repairFilter;
    private VBox repairRight;

    // ---- 售电组件 ----
    private TableView<ElectricityPurchase> sellTable;
    private ComboBox<String> sellFilter;
    private VBox sellRight;

    public AdminView(Admin admin) {
        this.admin = admin;
    }

    // ==================== 工作台概览 ====================

    public Node build() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setMaxWidth(800);

        Label title = new Label("宿管科工作台");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        addKeyValueRows(box, new String[][]{
                {"姓名", admin.getName()},
                {"工号", admin.getId()},
                {"职务", admin.getJobTitle()},
        });

        DataCenter dc = DataCenter.instance();
        Label overviewTitle = new Label("系统概览");
        UI.style(overviewTitle, UI.SECTION);
        box.getChildren().add(overviewTitle);

        addKeyValueRows(box, new String[][]{
                {"学院数量", dc.getColleges().size() + " 个"},
                {"宿舍楼栋", dc.getBuildings().size() + " 栋"},
                {"登记学生", dc.getStudents().size() + " 人"},
                {"待处理维修单", countRepair(RepairTicket.STATUS_PENDING) + " 单"},
                {"待售电购电单", countPurchasePending() + " 单"},
        });

        Label hint = new Label("请在左侧功能菜单选择：楼栋分配 · 宿舍管理人员管理 · 修理管理 · 售电。");
        UI.style(hint, UI.MUTED);
        box.getChildren().add(hint);
        return wrap(box);
    }

    // ==================== 楼栋分配 ====================

    public Node buildBuildingAllocation() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("楼栋分配");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        Label guide = new Label("把楼栋分配给学院或解除分配。规则：已有学生入住的楼栋不能调整归属（须先腾空）；"
                + "每栋楼只能归属一个学院；分配后该学院学生（性别匹配）可在宿舍申请中转入选中的楼栋。");
        guide.setWrapText(true);
        UI.style(guide, UI.MUTED);
        box.getChildren().add(guide);

        buildingFilter = new ComboBox<>();
        buildingFilter.getItems().addAll("全部楼栋", "已分配", "未分配");
        buildingFilter.getSelectionModel().selectFirst();
        buildingFilter.setOnAction(e -> refreshBuildingTable());
        HBox filterRow = new HBox(10, new Label("范围："), buildingFilter);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(filterRow);

        buildingTable = new TableView<>();
        buildingTable.getColumns().add(col("楼栋", 120, Building::getName));
        buildingTable.getColumns().add(col("性别", 55, b -> b.isMale() ? "男" : b.isFemale() ? "女" : "未定"));
        buildingTable.getColumns().add(col("所属学院", 230, this::ownerText));
        buildingTable.getColumns().add(col("入住", 70, b -> DataCenter.instance().occupiedCountOfBuilding(b.getName()) + " 人"));
        buildingTable.getColumns().add(col("房间", 70, b -> DataCenter.instance().findRoomsOfBuilding(b.getName()).size() + " 间"));
        buildingTable.getColumns().add(col("卫浴", 90, b -> b.isHasBathroom() ? "独立卫浴" : "公共卫浴"));
        buildingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        buildingTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> showBuildingDetail(newVal));
        VBox.setVgrow(buildingTable, Priority.ALWAYS);

        buildingRight = emptyPanel();
        HBox content = new HBox(14, buildingTable, buildingRight);
        HBox.setHgrow(buildingTable, Priority.ALWAYS);
        box.getChildren().add(content);
        refreshBuildingTable();
        return wrap(box);
    }

    private void refreshBuildingTable() {
        DataCenter dc = DataCenter.instance();
        List<Building> shown = new ArrayList<>();
        String filter = buildingFilter.getValue();
        for (Building b : dc.getBuildings()) {
            boolean assigned = b.getCollegeCode() != null && !b.getCollegeCode().isEmpty();
            if ("全部楼栋".equals(filter)
                    || ("已分配".equals(filter) && assigned)
                    || ("未分配".equals(filter) && !assigned)) {
                shown.add(b);
            }
        }
        buildingTable.setItems(FXCollections.observableArrayList(shown));
        showBuildingDetail(null);
    }

    private void showBuildingDetail(Building b) {
        buildingRight.getChildren().clear();
        if (b == null) {
            buildingRight.getChildren().add(hintLabel("请在左侧选择一栋楼。"));
            return;
        }
        DataCenter dc = DataCenter.instance();
        int occupied = dc.occupiedCountOfBuilding(b.getName());
        Label detail = new Label("楼栋：" + b.getName()
                + "\n性别：" + (b.isMale() ? "男" : b.isFemale() ? "女" : "未定")
                + "\n所属学院：" + ownerText(b)
                + "\n当前入住：" + occupied + " 人"
                + "\n房间数：" + dc.findRoomsOfBuilding(b.getName()).size() + " 间"
                + "\n卫浴：" + (b.isHasBathroom() ? "独立卫浴" : "公共卫浴"));
        detail.setWrapText(true);
        UI.style(detail, UI.DETAIL);
        buildingRight.getChildren().add(detail);

        if (occupied > 0) {
            Label warn = new Label("⚠ 该楼栋有学生入住，不能调整归属。");
            UI.style(warn, UI.DANGER);
            warn.setWrapText(true);
            buildingRight.getChildren().add(warn);
            return;
        }

        final ComboBox<String> collegeCombo = new ComboBox<>();
        collegeCombo.setPrefWidth(270);
        collegeCombo.getItems().add("（解除分配）");
        for (College c : dc.getColleges()) {
            collegeCombo.getItems().add(c.getCode() + " " + c.getName());
        }
        String cur = b.getCollegeCode();
        if (cur == null || cur.isEmpty()) {
            collegeCombo.getSelectionModel().selectFirst();
        } else {
            selectOptionByPrefix(collegeCombo, cur);
        }
        buildingRight.getChildren().add(new Label("调整归属："));
        buildingRight.getChildren().add(collegeCombo);

        Button save = button("保存分配", "#27ae60", e -> {
            String option = collegeCombo.getValue();
            String code = option == null ? "" : option.startsWith("（") ? "" : option.substring(0, option.indexOf(' '));
            try {
                adminService.assignBuilding(b.getName(), code);
                AlertUtil.info("已保存：" + b.getName() + " → " + (code.isEmpty() ? "未分配" : dc.collegeName(code)));
                refreshBuildingTable();
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });
        buildingRight.getChildren().add(save);

        if (cur != null && !cur.isEmpty()) {
            buildingRight.getChildren().add(button("解除分配", "#e74c3c", e -> {
                try {
                    adminService.assignBuilding(b.getName(), "");
                    AlertUtil.info("已解除分配：" + b.getName());
                    refreshBuildingTable();
                } catch (BusinessException ex) {
                    AlertUtil.error(ex.getMessage());
                }
            }));
        }
    }

    private void selectOptionByPrefix(ComboBox<String> combo, String code) {
        for (String option : combo.getItems()) {
            if (option.startsWith(code + " ")) {
                combo.getSelectionModel().select(option);
                return;
            }
        }
    }

    private String ownerText(Building b) {
        String code = b.getCollegeCode();
        if (code == null || code.isEmpty()) {
            return "未分配";
        }
        return DataCenter.instance().collegeName(code);
    }

    // ==================== 宿舍管理人员管理 ====================

    public Node buildDormStaffManage() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("宿舍管理人员管理");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);
        Label guide = new Label("宿舍管理人员的增删改查。新增/编辑可指定其负责楼栋；删除前须先解除其负责楼栋。"
                + "新增员工自动生成登录账号（用户名 = 工号小写，默认密码 123456）。");
        guide.setWrapText(true);
        UI.style(guide, UI.MUTED);
        box.getChildren().add(guide);

        staffTable = new TableView<>();
        staffTable.getColumns().add(col("工号", 80, DormStaff::getId));
        staffTable.getColumns().add(col("姓名", 90, DormStaff::getName));
        staffTable.getColumns().add(col("性别", 55, DormStaff::getGender));
        staffTable.getColumns().add(col("电话", 130, DormStaff::getPhone));
        staffTable.getColumns().add(col("职务", 100, DormStaff::getJobTitle));
        staffTable.getColumns().add(col("负责楼栋", 260, s -> String.join("、", s.getManageBuildingNames())));
        staffTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        staffTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> loadStaffForm(newVal));
        VBox.setVgrow(staffTable, Priority.ALWAYS);

        staffRight = buildStaffForm();
        HBox content = new HBox(14, staffTable, staffRight);
        HBox.setHgrow(staffTable, Priority.ALWAYS);
        box.getChildren().add(content);
        refreshStaffTable();
        return wrap(box);
    }

    /** 右侧编辑/新增表单。字段控件保存为成员，供 load/清空直接回填。 */
    private VBox buildStaffForm() {
        VBox form = new VBox(8);
        form.setPrefWidth(330);
        form.setPadding(new Insets(16));
        UI.style(form, UI.CARD);

        staffIdField = new TextField();
        staffIdField.setPromptText("工号（新增时自动生成，可改）");
        staffIdField.textProperty().addListener((obs, oldVal, newVal) -> updateStaffAccountHint());

        staffNameField = new TextField();
        staffNameField.setPromptText("姓名");
        staffGenderCombo = new ComboBox<>();
        staffGenderCombo.getItems().addAll("男", "女");
        staffPhoneField = new TextField();
        staffPhoneField.setPromptText("联系电话");
        staffJobField = new TextField();
        staffJobField.setPromptText("职务（默认：楼栋管理员）");

        staffBuildings = new ListView<>();
        staffBuildings.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        for (Building b : DataCenter.instance().getBuildings()) {
            staffBuildings.getItems().add(b.getName());
        }
        staffBuildings.setPrefHeight(140);

        staffAccountLabel = new Label("登录账号：--");
        UI.style(staffAccountLabel, UI.MUTED);

        form.getChildren().addAll(
                new Label("工号："), staffIdField,
                new Label("姓名："), staffNameField,
                new Label("性别："), staffGenderCombo,
                new Label("联系电话："), staffPhoneField,
                new Label("职务："), staffJobField,
                new Label("负责楼栋（可多选，Ctrl/Shift）："), staffBuildings,
                staffAccountLabel);

        HBox actions = new HBox(10,
                button("新增", "#3498db", e -> resetStaffForm()),
                button("保存", "#27ae60", e -> saveStaff()),
                button("删除选中", "#e74c3c", e -> deleteStaff()));
        form.getChildren().add(actions);
        return form;
    }

    private void refreshStaffTable() {
        staffTable.setItems(FXCollections.observableArrayList(DataCenter.instance().getDormStaffs()));
        staffTable.getSelectionModel().clearSelection();
    }

    /** 点表格中的员工：进入编辑态并回填表单。 */
    private void loadStaffForm(DormStaff s) {
        if (s == null) {
            return;
        }
        currentStaff = s;
        staffIdField.setDisable(true);
        staffIdField.setText(s.getId());
        staffNameField.setText(s.getName());
        staffGenderCombo.getSelectionModel().select(s.getGender());
        staffPhoneField.setText(s.getPhone());
        staffJobField.setText(s.getJobTitle());
        staffBuildings.getSelectionModel().clearSelection();
        for (int i = 0; i < staffBuildings.getItems().size(); i++) {
            String name = staffBuildings.getItems().get(i);
            if (s.getManageBuildingNames().contains(name)) {
                staffBuildings.getSelectionModel().select(i);
            }
        }
        updateStaffAccountHint();
    }

    /** 新增态：重置表单并预生成工号。 */
    private void resetStaffForm() {
        currentStaff = null;
        staffIdField.setDisable(false);
        staffIdField.setText(DataCenter.instance().nextDormStaffId());
        staffNameField.clear();
        staffGenderCombo.getSelectionModel().selectFirst();
        staffPhoneField.clear();
        staffJobField.clear();
        staffBuildings.getSelectionModel().clearSelection();
        updateStaffAccountHint();
    }

    private void updateStaffAccountHint() {
        String id = staffIdField.getText() == null ? "" : staffIdField.getText().trim();
        if (id.isEmpty()) {
            staffAccountLabel.setText("登录账号：--");
            return;
        }
        staffAccountLabel.setText("登录账号：" + id.toLowerCase() + "　默认密码：" + AdminService.DEFAULT_PASSWORD);
    }

    private void saveStaff() {
        List<String> buildings = new ArrayList<>(staffBuildings.getSelectionModel().getSelectedItems());
        boolean adding = currentStaff == null;
        try {
            if (adding) {
                String id = staffIdField.getText() == null ? "" : staffIdField.getText().trim();
                if (id.isEmpty()) {
                    AlertUtil.error("请先点击「新增」生成或填写工号");
                    return;
                }
                adminService.addDormStaff(id, staffNameField.getText(), staffGenderCombo.getValue(),
                        staffPhoneField.getText(), staffJobField.getText(), buildings);
                AlertUtil.info("已新增宿舍管理人员 " + id + "，登录账号 " + id.toLowerCase() + "，默认密码 123456。");
            } else {
                adminService.updateDormStaff(currentStaff, staffNameField.getText(), staffGenderCombo.getValue(),
                        staffPhoneField.getText(), staffJobField.getText(), buildings);
                AlertUtil.info("已保存修改：" + currentStaff.getId());
            }
            refreshStaffTable();
            if (adding) {
                resetStaffForm();
            }
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    private void deleteStaff() {
        if (currentStaff == null) {
            AlertUtil.warn("请先在表格中选择一名宿舍管理人员");
            return;
        }
        DormStaff s = currentStaff;
        if (AlertUtil.confirm("确定删除宿舍管理人员 " + s.getName() + "（" + s.getId() + "）吗？")) {
            try {
                adminService.removeDormStaff(s);
                AlertUtil.info("已删除。");
                currentStaff = null;
                refreshStaffTable();
                resetStaffForm();
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        }
    }

    // ==================== 修理管理 ====================

    public Node buildRepairManage() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("修理管理（维修工单）");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        repairFilter = new ComboBox<>();
        repairFilter.getItems().addAll("待处理", "处理中", "已完成", "全部");
        repairFilter.getSelectionModel().selectFirst();
        repairFilter.setOnAction(e -> refreshRepairTable());
        HBox filterRow = new HBox(10, new Label("范围："), repairFilter);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(filterRow);

        repairTable = new TableView<>();
        repairTable.getColumns().add(col("单号", 90, RepairTicket::getId));
        repairTable.getColumns().add(col("房间", 100, RepairTicket::getRoomKey));
        repairTable.getColumns().add(col("报修人", 110, t -> studentName(t.getReporterId())));
        repairTable.getColumns().add(col("维修内容", 220, RepairTicket::getDescription));
        repairTable.getColumns().add(col("状态", 90, RepairTicket::getStatusName));
        repairTable.getColumns().add(col("提交时间", 150, RepairTicket::getCreateTime));
        repairTable.getColumns().add(col("处理人", 90, t -> safe(t.getHandlerId())));
        repairTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        repairTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> showRepairDetail(newVal));
        VBox.setVgrow(repairTable, Priority.ALWAYS);

        repairRight = emptyPanel();
        HBox content = new HBox(14, repairTable, repairRight);
        HBox.setHgrow(repairTable, Priority.ALWAYS);
        box.getChildren().add(content);
        refreshRepairTable();
        return wrap(box);
    }

    private void refreshRepairTable() {
        List<RepairTicket> shown = new ArrayList<>();
        String filter = repairFilter.getValue();
        for (RepairTicket t : repairService.findAll()) {
            if ("全部".equals(filter) || t.getStatusName().equals(filter)) {
                shown.add(t);
            }
        }
        repairTable.setItems(FXCollections.observableArrayList(shown));
        showRepairDetail(null);
    }

    private void showRepairDetail(RepairTicket t) {
        repairRight.getChildren().clear();
        if (t == null) {
            repairRight.getChildren().add(hintLabel("请在左侧选择一张维修工单。"));
            return;
        }
        Label detail = new Label("单号：" + t.getId()
                + "\n房间：" + t.getRoomKey()
                + "\n报修人：" + studentName(t.getReporterId())
                + "\n维修内容：" + t.getDescription()
                + "\n状态：" + t.getStatusName()
                + "\n提交时间：" + t.getCreateTime()
                + "\n处理人：" + safe(t.getHandlerId())
                + "\n处理时间：" + safe(t.getHandleTime()));
        detail.setWrapText(true);
        UI.style(detail, UI.DETAIL);
        repairRight.getChildren().add(detail);

        if (RepairTicket.STATUS_PENDING.equals(t.getStatus())) {
            repairRight.getChildren().add(buttonRow(
                    button("受理（进入处理中）", "#27ae60", e -> acceptRepair(t)),
                    button("直接办结", "#2c3e50", e -> finishRepair(t, true))));
        } else if (RepairTicket.STATUS_PROCESSING.equals(t.getStatus())) {
            repairRight.getChildren().add(button("办结（已完成）", "#27ae60", e -> finishRepair(t, false)));
        } else {
            repairRight.getChildren().add(new Label("（工单已办结，仅供查看）"));
        }
    }

    private void acceptRepair(RepairTicket t) {
        try {
            repairService.accept(t, admin);
            AlertUtil.info("已受理工单 " + t.getId() + "，状态置为处理中。");
            refreshRepairTable();
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    private void finishRepair(RepairTicket t, boolean fromPending) {
        try {
            if (fromPending) {
                repairService.accept(t, admin);
            }
            repairService.finish(t, admin);
            AlertUtil.info("工单 " + t.getId() + " 已办结。");
            refreshRepairTable();
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    // ==================== 售电 ====================

    public Node buildSellElectricity() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("售电（购电单处理）");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);
        Label guide = new Label("学生提交购电后在本窗口缴费，确认收费后电量计入对应房间电表。"
                + "单价：" + FormatUtil.money(ElectricityService.UNIT_PRICE) + " 元/度。");
        guide.setWrapText(true);
        UI.style(guide, UI.MUTED);
        box.getChildren().add(guide);

        sellFilter = new ComboBox<>();
        sellFilter.getItems().addAll("待售电", "已售", "全部");
        sellFilter.getSelectionModel().selectFirst();
        sellFilter.setOnAction(e -> refreshSellTable());
        HBox filterRow = new HBox(10, new Label("范围："), sellFilter);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(filterRow);

        sellTable = new TableView<>();
        sellTable.getColumns().add(col("单号", 90, ElectricityPurchase::getId));
        sellTable.getColumns().add(col("房间", 110, ElectricityPurchase::getRoomKey));
        sellTable.getColumns().add(col("购电人", 110, p -> studentName(p.getBuyerId())));
        sellTable.getColumns().add(col("电量(度)", 90, p -> FormatUtil.num(p.getDegree())));
        sellTable.getColumns().add(col("金额(元)", 90, p -> FormatUtil.money(p.getAmount())));
        sellTable.getColumns().add(col("状态", 80, ElectricityPurchase::getStatusName));
        sellTable.getColumns().add(col("提交时间", 150, ElectricityPurchase::getCreateTime));
        sellTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        sellTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> showSellDetail(newVal));
        VBox.setVgrow(sellTable, Priority.ALWAYS);

        sellRight = emptyPanel();
        HBox content = new HBox(14, sellTable, sellRight);
        HBox.setHgrow(sellTable, Priority.ALWAYS);
        box.getChildren().add(content);
        refreshSellTable();
        return wrap(box);
    }

    private void refreshSellTable() {
        List<ElectricityPurchase> shown = new ArrayList<>();
        String filter = sellFilter.getValue();
        for (ElectricityPurchase p : electricityService.findAll()) {
            if ("全部".equals(filter)
                    || ("待售电".equals(filter) && p.isPending())
                    || ("已售".equals(filter) && !p.isPending())) {
                shown.add(p);
            }
        }
        sellTable.setItems(FXCollections.observableArrayList(shown));
        showSellDetail(null);
    }

    private void showSellDetail(ElectricityPurchase p) {
        sellRight.getChildren().clear();
        if (p == null) {
            sellRight.getChildren().add(hintLabel("请在左侧选择一张购电单。"));
            return;
        }
        DataCenter dc = DataCenter.instance();
        Label detail = new Label("单号：" + p.getId()
                + "\n房间：" + p.getRoomKey()
                + "\n购电人：" + studentName(p.getBuyerId())
                + "\n电量：" + FormatUtil.num(p.getDegree()) + " 度"
                + "\n单价：" + FormatUtil.money(p.getUnitPrice()) + " 元/度"
                + "\n应付：" + FormatUtil.money(p.getAmount()) + " 元"
                + "\n提交时间：" + p.getCreateTime());
        detail.setWrapText(true);
        UI.style(detail, UI.DETAIL);
        sellRight.getChildren().add(detail);

        if (p.isPending()) {
            final Room room = dc.findRoomByKey(p.getRoomKey());
            if (room != null) {
                Label balance = new Label("售电前房间剩余电量：" + FormatUtil.num(room.getElectricityBalance()) + " 度");
                UI.style(balance, UI.MUTED);
                sellRight.getChildren().add(balance);
            }
            sellRight.getChildren().add(button("确认收费并售电", "#27ae60", e -> {
                try {
                    electricityService.sell(p, admin);
                    AlertUtil.info("已售电 " + FormatUtil.num(p.getDegree()) + " 度。"
                            + (room == null ? "" : "房间剩余电量变为 "
                            + FormatUtil.num(room.getElectricityBalance()) + " 度。"));
                    refreshSellTable();
                } catch (BusinessException ex) {
                    AlertUtil.error(ex.getMessage());
                }
            }));
        } else {
            sellRight.getChildren().add(new Label("售电时间：" + safe(p.getHandleTime())
                    + "\n经办：" + safe(p.getHandlerId())));
        }
    }

    // ==================== 工具 ====================

    private int countRepair(String status) {
        int n = 0;
        for (RepairTicket t : DataCenter.instance().getRepairTickets()) {
            if (status.equals(t.getStatus())) {
                n++;
            }
        }
        return n;
    }

    private int countPurchasePending() {
        int n = 0;
        for (ElectricityPurchase p : DataCenter.instance().getElectricityPurchases()) {
            if (p.isPending()) {
                n++;
            }
        }
        return n;
    }

    private String studentName(String studentId) {
        Student s = DataCenter.instance().findStudentById(studentId);
        return s == null ? studentId : s.getName();
    }

    private void addKeyValueRows(VBox box, String[][] rows) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);
        for (int i = 0; i < rows.length; i++) {
            Label key = new Label(rows[i][0] + "：");
            UI.style(key, UI.KEY);
            Label val = new Label(rows[i][1]);
            UI.style(val, UI.INK);
            grid.addRow(i, key, val);
        }
        box.getChildren().add(grid);
    }

    private VBox emptyPanel() {
        VBox panel = new VBox(10);
        panel.setPrefWidth(320);
        panel.setPadding(new Insets(16));
        UI.style(panel, UI.CARD);
        return panel;
    }

    private Label hintLabel(String text) {
        Label l = new Label(text);
        UI.style(l, UI.FAINT);
        return l;
    }

    private HBox buttonRow(Node... nodes) {
        HBox row = new HBox(10, nodes);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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

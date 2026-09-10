package com.nchu.dorm.ui;

import com.nchu.dorm.model.Account;
import com.nchu.dorm.model.Admin;
import com.nchu.dorm.model.Building;
import com.nchu.dorm.model.College;
import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.announcement.Announcement;
import com.nchu.dorm.model.application.ElectricityPurchase;
import com.nchu.dorm.model.application.RepairTicket;
import com.nchu.dorm.model.record.ValuablesRecord;
import com.nchu.dorm.service.AdminService;
import com.nchu.dorm.service.AnnouncementService;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 宿管科视图：工作台概览 / 楼栋分配 / 宿舍管理人员管理 / 学生账号管理 / 维修管理 / 售电 / 遗失公告。
 * 迭代五落地宿管科四大职责，闭环逻辑收口在 {@link AdminService}、{@link RepairService}、
 * {@link ElectricityService}；迭代六新增「遗失公告」——审核宿管上报的物品遗失记录并发布全校公告，
 * 逻辑收口在 {@link AnnouncementService}；「维修管理」只处理<b>宿管已上报</b>的工单，
 * 学生报修先由分管宿管在「报修处理」中接收并上报。
 */
public class AdminView {

    /** 学生查询结果最多展示的行数：全校 4 万余人，关键词过宽时避免整表塞进界面。 */
    private static final int STUDENT_SEARCH_LIMIT = 200;

    private final Admin admin;
    private final AdminService adminService = new AdminService();
    private final RepairService repairService = new RepairService();
    private final ElectricityService electricityService = new ElectricityService();
    private final AnnouncementService announcementService = new AnnouncementService();

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

    // ---- 学生账号管理（密码重置）组件 ----
    private TableView<Student> studentTable;
    private TextField studentSearchField;
    private VBox studentRight;
    private Label studentResultHint;

    // ---- 维修管理组件 ----
    private TableView<RepairTicket> repairTable;
    private ComboBox<String> repairFilter;
    private VBox repairRight;

    // ---- 售电组件 ----
    private TableView<ElectricityPurchase> sellTable;
    private ComboBox<String> sellFilter;
    private VBox sellRight;

    // ---- 遗失公告组件 ----
    private TableView<LossRow> lossTable;
    private ComboBox<String> lossFilter;
    private VBox lossRight;
    private TextField lossTitleField;
    private TextArea lossContentField;

    /**
     * 遗失公告页左侧表格的行：一条「待发布 / 已发布」的遗失记录。
     * 表格需要展示物品与学生信息，而 {@link ValuablesRecord} 自身不带姓名，故用一个小包装类拼好各列文本。
     */
    private static class LossRow {
        private final ValuablesRecord record;

        LossRow(ValuablesRecord record) {
            this.record = record;
        }

        ValuablesRecord getRecord() {
            return record;
        }
    }

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
                {"待发布遗失公告", announcementService.pendingPublishCount() + " 条"},
        });

        Label hint = new Label("请在左侧功能菜单选择：楼栋分配 · 宿舍管理人员管理 · 学生账号管理 · "
                + "维修管理 · 售电 · 遗失公告。");
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

        buildingTable.setMinWidth(280);
        buildingRight = emptyPanel();
        HBox content = new HBox(14, buildingTable, panelScroll(buildingRight, 320));
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

        staffTable.setMinWidth(280);
        staffRight = buildStaffForm();
        HBox content = new HBox(14, staffTable, panelScroll(staffRight, 330));
        HBox.setHgrow(staffTable, Priority.ALWAYS);
        box.getChildren().add(content);
        refreshStaffTable();
        return wrap(box);
    }

    /** 右侧编辑/新增表单。字段控件保存为成员，供 load/清空直接回填。 */
    private VBox buildStaffForm() {
        VBox form = new VBox(8);
        form.setPadding(new Insets(16));
        form.setFillWidth(true);

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

        form.getChildren().add(buttonRow(
                button("新增", "#3498db", e -> resetStaffForm()),
                button("保存", "#27ae60", e -> saveStaff()),
                button("删除选中", "#e74c3c", e -> deleteStaff())));
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

    // ==================== 学生账号管理（密码重置） ====================

    /**
     * 学生账号管理：学生忘记密码时，宿管科按学号或姓名定位其账号并重置为默认密码。
     * 左侧查询结果表 + 右侧详情/重置面板（与「维修管理」「售电」同构）。
     */
    public Node buildStudentAccountManage() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("学生账号管理（密码重置）");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        Label guide = new Label("学生忘记密码时，在此按学号或姓名定位其登录账号，重置为默认密码 "
                + AdminService.DEFAULT_PASSWORD + "；重置后请提醒学生登录后在「设置」里自行修改密码。");
        guide.setWrapText(true);
        UI.style(guide, UI.MUTED);
        box.getChildren().add(guide);

        studentSearchField = new TextField();
        studentSearchField.setPromptText("输入学号（可只输前几位）或姓名");
        studentSearchField.setPrefWidth(280);
        // 输入框内回车等同点「查询」
        studentSearchField.setOnAction(e -> searchStudents());
        HBox searchRow = new HBox(10, new Label("学号 / 姓名："), studentSearchField,
                button("查询", "#3498db", e -> searchStudents()));
        searchRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(searchRow);

        studentTable = new TableView<>();
        studentTable.getColumns().add(col("学号", 110, Student::getId));
        studentTable.getColumns().add(col("姓名", 100, Student::getName));
        studentTable.getColumns().add(col("性别", 55, Student::getGender));
        studentTable.getColumns().add(col("学院", 190, s -> DataCenter.instance().collegeName(s.getCollegeCode())));
        studentTable.getColumns().add(col("专业", 170, Student::getMajor));
        studentTable.getColumns().add(col("班级", 80, Student::getClassName));
        studentTable.getColumns().add(col("当前宿舍", 130, s -> s.isCheckedIn()
                ? s.getCurrentBuilding() + "-" + s.getCurrentRoom() : "未入住"));
        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        studentTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> showStudentDetail(newVal));
        VBox.setVgrow(studentTable, Priority.ALWAYS);

        studentTable.setMinWidth(300);
        studentRight = emptyPanel();
        HBox content = new HBox(14, studentTable, panelScroll(studentRight, 320));
        HBox.setHgrow(studentTable, Priority.ALWAYS);
        box.getChildren().add(content);

        studentResultHint = hintLabel("请输入学号或姓名后点「查询」。");
        box.getChildren().add(studentResultHint);

        showStudentDetail(null);
        return wrap(box);
    }

    /** 执行查询并刷新结果表（关键词为空或查无结果时给出提示）。 */
    private void searchStudents() {
        String keyword = studentSearchField.getText() == null ? "" : studentSearchField.getText().trim();
        if (keyword.isEmpty()) {
            studentTable.setItems(FXCollections.observableArrayList());
            showStudentDetail(null);
            studentResultHint.setText("请先输入学号（可只输前几位）或姓名。");
            return;
        }
        // 多取一条用于判断是否被截断
        List<Student> matched = adminService.searchStudents(keyword, STUDENT_SEARCH_LIMIT + 1);
        boolean truncated = matched.size() > STUDENT_SEARCH_LIMIT;
        if (truncated) {
            matched = matched.subList(0, STUDENT_SEARCH_LIMIT);
        }
        studentTable.setItems(FXCollections.observableArrayList(matched));
        showStudentDetail(null);
        if (matched.isEmpty()) {
            studentResultHint.setText("未找到匹配的学生，请核对学号或姓名。");
        } else if (truncated) {
            studentResultHint.setText("匹配结果超过 " + STUDENT_SEARCH_LIMIT + " 条，仅显示前 "
                    + STUDENT_SEARCH_LIMIT + " 条；请把学号或姓名输得更完整一些。");
        } else {
            studentResultHint.setText("共匹配 " + matched.size() + " 名学生，请在表格中选择后重置密码。");
        }
    }

    private void showStudentDetail(Student s) {
        studentRight.getChildren().clear();
        if (s == null) {
            studentRight.getChildren().add(hintLabel("请在左侧查询并选择一名学生。"));
            return;
        }
        Account account = DataCenter.instance().findAccountByPersonId(s.getId());
        Label detail = new Label("学号：" + s.getId()
                + "\n姓名：" + s.getName()
                + "\n性别：" + safe(s.getGender())
                + "\n学院：" + DataCenter.instance().collegeName(s.getCollegeCode())
                + "\n专业：" + s.getMajor()
                + "\n班级：" + s.getClassName()
                + "\n当前宿舍：" + (s.isCheckedIn()
                        ? s.getCurrentBuilding() + "-" + s.getCurrentRoom() : "未入住")
                + "\n登录账号：" + (account == null ? "（无）" : account.getUsername()));
        detail.setWrapText(true);
        UI.style(detail, UI.DETAIL);
        studentRight.getChildren().add(detail);

        if (account == null) {
            studentRight.getChildren().add(hintLabel("该学生没有绑定的登录账号，无法重置密码。"));
            return;
        }
        Label warn = new Label("重置后登录密码变为 " + AdminService.DEFAULT_PASSWORD + "，请当面或电话告知学生。");
        warn.setWrapText(true);
        UI.style(warn, UI.WARN_DEEP);
        studentRight.getChildren().add(warn);

        studentRight.getChildren().add(button("重置密码为 " + AdminService.DEFAULT_PASSWORD, "#e67e22",
                e -> resetStudentPassword(s)));
    }

    private void resetStudentPassword(Student s) {
        if (!AlertUtil.confirm("确定把学生 " + s.getName() + "（" + s.getId() + "）的登录密码重置为 "
                + AdminService.DEFAULT_PASSWORD + " 吗？")) {
            return;
        }
        try {
            adminService.resetStudentPassword(s);
            AlertUtil.info("已重置学生 " + s.getId() + " 的登录密码为 " + AdminService.DEFAULT_PASSWORD + "。");
            showStudentDetail(s);
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    // ==================== 维修管理 ====================

    public Node buildRepairManage() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("维修管理（维修工单）");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        Label guide = new Label("此处只处理宿舍管理人员核实并上报的维修工单。学生在「维修报修」中提交后，"
                + "先由分管宿管在「报修处理」中接收上报；受理（处理中）→ 办结（已完成）。");
        guide.setWrapText(true);
        UI.style(guide, UI.MUTED);
        box.getChildren().add(guide);

        repairFilter = new ComboBox<>();
        repairFilter.getItems().addAll("待处理", "处理中", "已完成", "全部");
        repairFilter.getSelectionModel().selectFirst();
        repairFilter.setOnAction(e -> refreshRepairTable());
        HBox filterRow = new HBox(10, new Label("范围："), repairFilter,
                button("一键受理全部待处理", "#27ae60", e -> acceptAllRepairs()),
                button("一键办结全部未办结", "#2c3e50", e -> finishAllRepairs()));
        filterRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(filterRow);

        repairTable = new TableView<>();
        repairTable.getColumns().add(col("单号", 90, RepairTicket::getId));
        repairTable.getColumns().add(col("房间", 100, RepairTicket::getRoomKey));
        repairTable.getColumns().add(col("报修人", 110, t -> studentName(t.getReporterId())));
        repairTable.getColumns().add(col("维修内容", 220, RepairTicket::getDescription));
        repairTable.getColumns().add(col("状态", 90, RepairTicket::getStatusName));
        repairTable.getColumns().add(col("提交时间", 150, RepairTicket::getCreateTime));
        repairTable.getColumns().add(col("上报宿管", 130, t -> personLabel(t.getEscalatorId())));
        repairTable.getColumns().add(col("处理人", 90, t -> safe(t.getHandlerId())));
        repairTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        repairTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> showRepairDetail(newVal));
        VBox.setVgrow(repairTable, Priority.ALWAYS);

        repairTable.setMinWidth(300);
        repairRight = emptyPanel();
        HBox content = new HBox(14, repairTable, panelScroll(repairRight, 320));
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
                + "\n上报宿管：" + personLabel(t.getEscalatorId())
                + "\n上报时间：" + safe(t.getEscalateTime())
                + "\n处理人：" + personLabel(t.getHandlerId())
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
        } else if (RepairTicket.STATUS_DONE.equals(t.getStatus())) {
            repairRight.getChildren().add(new Label("（工单已办结，仅供查看）"));
        } else {
            // SUBMITTED 已被 findAll() 排除，正常不会走到这里；兜底提示避免误导为「已办结」
            repairRight.getChildren().add(new Label("（该工单尚未上报宿管科，暂不可处理）"));
        }
    }

    /** 一键受理全部待处理工单（全校范围，不限于当前筛选结果）。 */
    private void acceptAllRepairs() {
        int pending = countRepair(RepairTicket.STATUS_PENDING);
        if (pending == 0) {
            AlertUtil.info("当前没有待处理的维修工单。");
            return;
        }
        if (!AlertUtil.confirm("确定一键受理全部 " + pending + " 张待处理工单吗？"
                + "（全校范围，不受当前筛选影响）")) {
            return;
        }
        int done = repairService.acceptAll(admin);
        refreshRepairTable();
        AlertUtil.info("已一键受理 " + done + " 张工单，状态置为处理中。");
    }

    /** 一键办结全部未办结工单（全校范围，不限于当前筛选结果）。 */
    private void finishAllRepairs() {
        int open = countRepair(RepairTicket.STATUS_PENDING) + countRepair(RepairTicket.STATUS_PROCESSING);
        if (open == 0) {
            AlertUtil.info("当前没有未办结的维修工单。");
            return;
        }
        if (!AlertUtil.confirm("确定一键办结全部 " + open + " 张未办结工单（待处理 "
                + countRepair(RepairTicket.STATUS_PENDING) + " 张 + 处理中 "
                + countRepair(RepairTicket.STATUS_PROCESSING) + " 张）吗？"
                + "（全校范围，不受当前筛选影响）")) {
            return;
        }
        int done = repairService.finishAll(admin);
        refreshRepairTable();
        AlertUtil.info("已一键办结 " + done + " 张工单。");
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
                + "单价：" + FormatUtil.money(ElectricityService.UNIT_PRICE) + " 元/度。"
                + "单据量大时可使用「一键售电全部待售电」批量收费。");
        guide.setWrapText(true);
        UI.style(guide, UI.MUTED);
        box.getChildren().add(guide);

        sellFilter = new ComboBox<>();
        sellFilter.getItems().addAll("待售电", "已售", "全部");
        sellFilter.getSelectionModel().selectFirst();
        sellFilter.setOnAction(e -> refreshSellTable());
        HBox filterRow = new HBox(10, new Label("范围："), sellFilter,
                button("一键售电全部待售电", "#27ae60", e -> sellAllPending()));
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

        sellTable.setMinWidth(300);
        sellRight = emptyPanel();
        HBox content = new HBox(14, sellTable, panelScroll(sellRight, 320));
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

    /** 一键售电：把全部待售电单据批量确认收费（全校范围，不限于当前筛选结果）。 */
    private void sellAllPending() {
        int pending = countPurchasePending();
        if (pending == 0) {
            AlertUtil.info("当前没有待售电的购电单。");
            return;
        }
        if (!AlertUtil.confirm("确定对全部 " + pending + " 张待售电单据一键确认收费吗？"
                + "（全校范围，不受当前筛选影响，售出后电量立即计入各房间电表）")) {
            return;
        }
        int sold = electricityService.sellAll(admin);
        refreshSellTable();
        int skipped = pending - sold;
        AlertUtil.info("已一键售电 " + sold + " 单。"
                + (skipped > 0 ? "另有 " + skipped + " 单因对应房间不存在被跳过，请人工核对。" : ""));
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

    // ==================== 遗失公告 ====================

    /**
     * 遗失公告页：宿舍管理人员上报的学生物品遗失记录在此汇总，宿管科审核（可修改标题与正文，
     * 默认按模板生成「某学生（学号：…）xx 物品遗失…」）后发布为全校公告，学生在「公告」页可见。
     */
    public Node buildAnnouncementManage() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        Label title = new Label("遗失公告");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().add(title);

        Label guide = new Label("宿舍管理人员上报的物品遗失记录在此审核发布。选中待发布记录后，"
                + "标题与正文会按模板自动填入，可自行修改；确认无误后点「发布公告」，"
                + "公告将展示在全体学生的「公告」页。");
        guide.setWrapText(true);
        UI.style(guide, UI.MUTED);
        box.getChildren().add(guide);

        lossFilter = new ComboBox<>();
        lossFilter.getItems().addAll("待发布", "已发布", "全部");
        lossFilter.getSelectionModel().selectFirst();
        lossFilter.setOnAction(e -> refreshLossTable());
        HBox filterRow = new HBox(10, new Label("范围："), lossFilter);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(filterRow);

        lossTable = new TableView<>();
        lossTable.getColumns().add(col("记录号", 90, r -> r.getRecord().getId()));
        lossTable.getColumns().add(col("学号", 110, r -> r.getRecord().getStudentId()));
        lossTable.getColumns().add(col("姓名", 100, r -> studentName(r.getRecord().getStudentId())));
        lossTable.getColumns().add(col("物品", 150, r -> r.getRecord().getItemName()));
        lossTable.getColumns().add(col("上报人", 120, r -> r.getRecord().getReporterId()));
        lossTable.getColumns().add(col("上报时间", 150, r -> safe(r.getRecord().getReportTime())));
        lossTable.getColumns().add(col("状态", 110, r -> r.getRecord().getReportStatusName()));
        lossTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        lossTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> showLossDetail(newVal == null ? null : newVal.getRecord()));
        VBox.setVgrow(lossTable, Priority.ALWAYS);

        lossTable.setMinWidth(300);
        lossRight = emptyPanel();
        HBox content = new HBox(14, lossTable, panelScroll(lossRight, 360));
        HBox.setHgrow(lossTable, Priority.ALWAYS);
        box.getChildren().add(content);
        refreshLossTable();
        return wrap(box);
    }

    private void refreshLossTable() {
        List<LossRow> shown = new ArrayList<>();
        String filter = lossFilter.getValue();
        for (ValuablesRecord r : announcementService.escalatedAll()) {
            boolean pending = ValuablesRecord.REPORT_ESCALATED.equals(r.getReportStatus());
            if ("全部".equals(filter)
                    || ("待发布".equals(filter) && pending)
                    || ("已发布".equals(filter) && !pending)) {
                shown.add(new LossRow(r));
            }
        }
        lossTable.setItems(FXCollections.observableArrayList(shown));
        showLossDetail(null);
    }

    private void showLossDetail(ValuablesRecord r) {
        lossRight.getChildren().clear();
        lossTitleField = null;
        lossContentField = null;
        if (r == null) {
            lossRight.getChildren().add(hintLabel("请在左侧选择一条遗失记录。"));
            return;
        }
        Label detail = new Label("记录号：" + r.getId()
                + "\n学生：" + studentName(r.getStudentId()) + "（" + r.getStudentId() + "）"
                + "\n物品：" + r.getItemName()
                + "\n带出时间：" + r.getRecordTime()
                + "\n学生说明：" + safe(r.getStudentRemark())
                + "\n上报人：" + safe(r.getReporterId())
                + "\n上报时间：" + safe(r.getReportTime())
                + "\n当前状态：" + r.getReportStatusName());
        detail.setWrapText(true);
        UI.style(detail, UI.DETAIL);
        lossRight.getChildren().add(detail);

        boolean pending = ValuablesRecord.REPORT_ESCALATED.equals(r.getReportStatus());
        Label warn = new Label(pending
                ? "发布后公告将展示在全体学生的「公告」页，且该记录标记为「已发布公告」。"
                : "该记录已发布过公告，可在下方查看内容（不可重复发布）。");
        warn.setWrapText(true);
        UI.style(warn, pending ? UI.WARN_DEEP : UI.MUTED);
        lossRight.getChildren().add(warn);

        lossTitleField = new TextField(pending ? announcementService.defaultTitle(r) : publishedTitleOf(r));
        lossTitleField.setEditable(pending);
        lossTitleField.setDisable(!pending);
        TextArea contentField = new TextArea(pending ? announcementService.defaultContent(r) : publishedContentOf(r));
        contentField.setWrapText(true);
        contentField.setPrefRowCount(7);
        contentField.setEditable(pending);
        contentField.setDisable(!pending);
        lossContentField = contentField;

        lossRight.getChildren().addAll(new Label("公告标题："), lossTitleField,
                new Label("公告正文："), contentField);

        if (pending) {
            lossRight.getChildren().add(button("发布公告", "#27ae60", e -> publishLoss(r)));
        }
    }

    private void publishLoss(ValuablesRecord r) {
        if (!AlertUtil.confirm("确定发布公告吗？公告将展示在全体学生的「公告」页。")) {
            return;
        }
        try {
            Announcement a = announcementService.publishLostItem(admin, r,
                    lossTitleField.getText(), lossContentField.getText());
            AlertUtil.info("已发布公告 " + a.getId() + "：《" + a.getTitle() + "》。");
            refreshLossTable();
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    /** 已发布记录对应的公告标题（按关联记录号回溯）。 */
    private String publishedTitleOf(ValuablesRecord r) {
        Announcement a = findAnnouncementOf(r.getId());
        return a == null ? announcementService.defaultTitle(r) : a.getTitle();
    }

    /** 已发布记录对应的公告正文（按关联记录号回溯）。 */
    private String publishedContentOf(ValuablesRecord r) {
        Announcement a = findAnnouncementOf(r.getId());
        return a == null ? announcementService.defaultContent(r) : a.getContent();
    }

    private Announcement findAnnouncementOf(String valuablesRecordId) {
        for (Announcement a : DataCenter.instance().getAnnouncements()) {
            if (valuablesRecordId.equals(a.getRelatedId())) {
                return a;
            }
        }
        return null;
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

    /** 人员标签：姓名 + 工号/学号；查不到时退回编号原文，空编号返回空串。 */
    private String personLabel(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        com.nchu.dorm.model.Person p = DataCenter.instance().findPersonById(id);
        return p == null ? id : p.getName() + "（" + id + "）";
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

    /** 右侧详情面板的内容容器；卡片外观与保底宽度由 {@link #panelScroll} 统一提供。 */
    private VBox emptyPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16));
        panel.setFillWidth(true);
        return panel;
    }

    /**
     * 把右侧面板套进卡片式滚动容器。
     * 直接放进 HBox 时，固定宽度的面板会被左侧表格挤到只剩一两百像素（按钮文字被截断），
     * 这里用 minWidth 给宽度保底，再让内容超高时在面板内部滚动，而不是把按钮压扁。
     * 传入的 {@code panel} 仍是原来的内容容器，各页面的 setAll/clear 逻辑无需改动。
     */
    private ScrollPane panelScroll(VBox panel, double width) {
        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setPrefWidth(width);
        sp.setMinWidth(width - 40);
        UI.style(sp, UI.PANEL_SCROLL);
        return sp;
    }

    private Label hintLabel(String text) {
        Label l = new Label(text);
        UI.style(l, UI.FAINT);
        return l;
    }

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

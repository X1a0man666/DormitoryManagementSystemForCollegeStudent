package com.nchu.dorm.ui;

import com.nchu.dorm.model.Bed;
import com.nchu.dorm.model.Building;
import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.model.record.HygieneRecord;
import com.nchu.dorm.model.record.NightReturnRecord;
import com.nchu.dorm.model.record.ValuablesRecord;
import com.nchu.dorm.service.DormStaffService;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.ui.component.AlertUtil;
import com.nchu.dorm.ui.component.UI;
import com.nchu.dorm.util.BusinessException;
import com.nchu.dorm.util.TimeUtil;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 宿舍管理人员视图：日常管理工作台 / 夜归登记 / 卫生检查 / 贵重物品出入登记。
 * 登记规则收口在 {@link DormStaffService}：只能登记本人分管楼栋内入住的学生 / 房间。
 */
public class DormStaffView {

    private final DormStaff staff;
    private final DormStaffService dormService = new DormStaffService();

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
        Label hint = new Label("请在左侧功能菜单选择：夜归登记 · 卫生检查 · 贵重物品登记。");
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

        final TextField dateField = new TextField(TimeUtil.today());
        dateField.setPromptText("yyyy-MM-dd");
        dateField.setPrefWidth(140);
        final TextField timeField = new TextField();
        timeField.setPromptText("夜归时间，如 23:30");
        timeField.setPrefWidth(140);
        final TextField reasonField = new TextField();
        reasonField.setPromptText("夜归原因（可选）");
        reasonField.setPrefWidth(300);

        Button register = button("登记夜归", "#27ae60", e -> {
            try {
                dormService.registerNightReturn(staff, studentField.getText(),
                        dateField.getText(), timeField.getText(), reasonField.getText());
                AlertUtil.info("已登记夜归记录。");
                renderNightReturn(body);
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        HBox studentRow = new HBox(10, new Label("学生："), studentField, query, preview);
        studentRow.setAlignment(Pos.CENTER_LEFT);
        HBox timeRow = new HBox(10, new Label("日期："), dateField,
                new Label("时间："), timeField);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        HBox reasonRow = new HBox(10, new Label("原因："), reasonField, register);
        reasonRow.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().addAll(studentRow, timeRow, reasonRow);

        body.getChildren().add(sectionTitle("夜归记录"));
        TableView<NightReturnRecord> table = new TableView<>();
        table.getColumns().add(col("编号", 80, NightReturnRecord::getId));
        table.getColumns().add(col("学号", 90, NightReturnRecord::getStudentId));
        table.getColumns().add(col("姓名·宿舍", 220, r -> occupantLabel(r.getStudentId())));
        table.getColumns().add(col("日期", 110, NightReturnRecord::getDate));
        table.getColumns().add(col("夜归时间", 100, NightReturnRecord::getReturnTime));
        table.getColumns().add(col("原因", 200, r -> safe(r.getReason())));
        table.setPrefHeight(320);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(managedNightRecords()));
        body.getChildren().add(table);
    }

    private List<NightReturnRecord> managedNightRecords() {
        List<NightReturnRecord> list = new ArrayList<>();
        for (NightReturnRecord r : DataCenter.instance().getNightReturnRecords()) {
            if (inManagedStudent(r.getStudentId())) {
                list.add(r);
            }
        }
        sortNight(list);
        return list;
    }

    private void sortNight(List<NightReturnRecord> list) {
        Collections.sort(list, new Comparator<NightReturnRecord>() {
            @Override
            public int compare(NightReturnRecord a, NightReturnRecord b) {
                String ka = a.getDate() + " " + a.getReturnTime();
                String kb = b.getDate() + " " + b.getReturnTime();
                return kb.compareTo(ka);
            }
        });
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
        table.setPrefHeight(300);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(managedHygieneRecords()));
        body.getChildren().add(table);
    }

    private List<HygieneRecord> managedHygieneRecords() {
        List<HygieneRecord> list = new ArrayList<>();
        DataCenter dc = DataCenter.instance();
        for (HygieneRecord r : dc.getHygieneRecords()) {
            String building = r.getRoomKey() == null ? "" : r.getRoomKey().split("-", -1)[0];
            if (staff.manages(building) || staff.getId().equals(r.getInspectorId())) {
                list.add(r);
            }
        }
        Collections.sort(list, new Comparator<HygieneRecord>() {
            @Override
            public int compare(HygieneRecord a, HygieneRecord b) {
                return b.getDate().compareTo(a.getDate());
            }
        });
        return list;
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
        table.getColumns().add(col("姓名·宿舍", 220, r -> occupantLabel(r.getStudentId())));
        table.getColumns().add(col("物品", 160, ValuablesRecord::getItemName));
        table.getColumns().add(col("方向", 70, ValuablesRecord::getDirectionName));
        table.getColumns().add(col("登记时间", 160, ValuablesRecord::getRecordTime));
        table.getColumns().add(col("登记人", 100, r -> personLabel(r.getHandlerId())));
        table.setPrefHeight(300);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(managedValuablesRecords()));
        body.getChildren().add(table);
    }

    private List<ValuablesRecord> managedValuablesRecords() {
        List<ValuablesRecord> list = new ArrayList<>();
        for (ValuablesRecord r : DataCenter.instance().getValuablesRecords()) {
            if (inManagedStudent(r.getStudentId()) || staff.getId().equals(r.getHandlerId())) {
                list.add(r);
            }
        }
        Collections.sort(list, new Comparator<ValuablesRecord>() {
            @Override
            public int compare(ValuablesRecord a, ValuablesRecord b) {
                return b.getRecordTime().compareTo(a.getRecordTime());
            }
        });
        return list;
    }

    // ==================== 工具 ====================

    private boolean inManagedStudent(String studentId) {
        Student s = DataCenter.instance().findStudentById(studentId);
        return s != null && s.isCheckedIn() && staff.manages(s.getCurrentBuilding());
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

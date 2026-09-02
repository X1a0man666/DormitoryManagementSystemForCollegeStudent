package com.nchu.dorm.ui;

import com.nchu.dorm.model.Building;
import com.nchu.dorm.model.DormStaff;
import com.nchu.dorm.storage.DataCenter;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * 宿舍管理人员视图（初版占位）。
 * 夜归管理、卫生管理、贵重物品出入登记将在后续迭代实现。
 */
public class DormStaffView {

    private final DormStaff staff;

    public DormStaffView(DormStaff staff) {
        this.staff = staff;
    }

    public Node build() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setMaxWidth(760);

        Label title = new Label("日常管理工作台");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().add(title);

        GridPane info = new GridPane();
        info.setHgap(20);
        info.setVgap(8);
        info.addRow(0, new Label("姓名："), new Label(staff.getName()));
        info.addRow(1, new Label("工号："), new Label(staff.getId()));
        info.addRow(2, new Label("职务："), new Label(staff.getJobTitle()));
        info.addRow(3, new Label("负责楼栋："), new Label(String.join("、", staff.getManageBuildingNames())));
        box.getChildren().add(info);

        Label upcoming = new Label("以下功能将在后续迭代开放");
        upcoming.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22;");
        box.getChildren().add(upcoming);

        box.getChildren().add(featureLine("夜归管理", "登记学生晚归时间与原因"));
        box.getChildren().add(featureLine("卫生管理", "楼栋房间卫生检查与评分"));
        box.getChildren().add(featureLine("贵重物品出入登记", "学生携带贵重物品进出楼栋登记"));

        // 楼栋简况
        Label buildingTitle = new Label("负责楼栋简况");
        buildingTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().add(buildingTitle);
        DataCenter dc = DataCenter.instance();
        for (String name : staff.getManageBuildingNames()) {
            Building b = dc.findBuilding(name);
            if (b != null) {
                String text = name + (b.getAlias() == null || b.getAlias().isEmpty() ? "" : "（" + b.getAlias() + "）")
                        + " · " + b.getFloorCount() + " 层"
                        + (b.isHasBathroom() ? " · 独立卫浴" : " · 公共卫浴")
                        + " · 房间数 " + dc.findRoomsOfBuilding(name).size();
                box.getChildren().add(new Label(text));
            }
        }

        return wrap(box);
    }

    private Label featureLine(String name, String desc) {
        Label l = new Label("· " + name + " —— " + desc);
        l.setStyle("-fx-text-fill: #7f8c8d;");
        return l;
    }

    private Node wrap(VBox box) {
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #f8f9fa;");
        return sp;
    }
}

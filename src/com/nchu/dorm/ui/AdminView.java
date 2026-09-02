package com.nchu.dorm.ui;

import com.nchu.dorm.model.Admin;
import com.nchu.dorm.storage.DataCenter;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * 宿管科视图（初版占位）。
 * 楼栋分配、宿舍管理人员管理、修理管理、售电将在后续迭代实现。
 */
public class AdminView {

    private final Admin admin;

    public AdminView(Admin admin) {
        this.admin = admin;
    }

    public Node build() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setMaxWidth(760);

        Label title = new Label("宿管科工作台");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().add(title);

        GridPane info = new GridPane();
        info.setHgap(20);
        info.setVgap(8);
        info.addRow(0, new Label("姓名："), new Label(admin.getName()));
        info.addRow(1, new Label("工号："), new Label(admin.getId()));
        info.addRow(2, new Label("职务："), new Label(admin.getJobTitle()));
        box.getChildren().add(info);

        DataCenter dc = DataCenter.instance();
        Label overviewTitle = new Label("系统概览");
        overviewTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().add(overviewTitle);

        GridPane overview = new GridPane();
        overview.setHgap(40);
        overview.setVgap(8);
        overview.addRow(0, new Label("学院数量："), new Label(String.valueOf(dc.getColleges().size())));
        overview.addRow(1, new Label("宿舍楼栋："), new Label(String.valueOf(dc.getBuildings().size()) + " 栋"));
        overview.addRow(2, new Label("登记学生："), new Label(String.valueOf(dc.getStudents().size()) + " 人"));
        overview.addRow(3, new Label("待处理维修单："), new Label(String.valueOf(dc.getRepairTickets().size()) + " 单"));
        box.getChildren().add(overview);

        Label upcoming = new Label("以下功能将在后续迭代开放");
        upcoming.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22;");
        box.getChildren().add(upcoming);

        box.getChildren().add(featureLine("楼栋分配", "为各学院分配宿舍楼栋"));
        box.getChildren().add(featureLine("宿舍管理人员管理", "宿舍管理人员的增删改查"));
        box.getChildren().add(featureLine("修理管理", "维修工单的派单与处理"));
        box.getChildren().add(featureLine("售电", "学生购电收费处理"));

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

package com.nchu.dorm.ui;

import com.nchu.dorm.model.Person;
import com.nchu.dorm.service.AccountService;
import com.nchu.dorm.service.AvatarService;
import com.nchu.dorm.ui.component.AlertUtil;
import com.nchu.dorm.ui.component.UI;
import com.nchu.dorm.util.BusinessException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * 设置页（学生与辅导员共用）：更换头像、修改本人姓名与登录密码。
 * <p>
 * 头像默认校徽，可换成自己的图片；姓名默认与学号/工号一致，可自行修改（学号/工号是唯一标识，不可更改）；
 * 改密码须先验证当前账号的密码，且两次输入的新密码一致才会生效。
 * <p>
 * 本类只负责收集输入与展示，校验与落库在 {@link AccountService}、{@link AvatarService}；
 * 角色差异只有称谓（学号 / 工号）与提示文案，由构造参数传入。
 */
public class SettingsView {

    private final Person person;

    /** 唯一标识在本角色下的称谓：学生为「学号」，教职工为「工号」。 */
    private final String idLabel;

    /** 个人信息区的提示文案。 */
    private final String nameTip;

    private final AccountService accountService = new AccountService();
    private final AvatarService avatarService = new AvatarService();

    public SettingsView(Person person, String idLabel, String nameTip) {
        this.person = person;
        this.idLabel = idLabel;
        this.nameTip = nameTip;
    }

    /**
     * @param onProfileChanged 头像/姓名修改成功后的回调（主框架据此刷新顶栏头像与问候语），可为 null
     */
    public Node build(Runnable onProfileChanged) {
        VBox box = new VBox(20);
        box.setPadding(new Insets(24));
        box.setMaxWidth(720);

        Label title = new Label("设置");
        UI.style(title, UI.PAGE_TITLE);
        box.getChildren().addAll(title, buildAvatarSection(onProfileChanged),
                buildProfileSection(onProfileChanged), buildPasswordSection());
        return wrap(box);
    }

    /** 头像：预览 + 从本地选图更换 / 恢复默认校徽。 */
    private Node buildAvatarSection(Runnable onProfileChanged) {
        Label head = new Label("头像");
        UI.style(head, UI.SECTION);

        final ImageView preview = UI.circularAvatar(96);
        preview.setImage(currentAvatarImage());

        Button choose = new Button("更换头像…");
        UI.style(choose, UI.BTN, UI.BTN_PRIMARY);
        choose.setOnAction(e -> chooseAvatar(preview, onProfileChanged));

        Button reset = new Button("恢复默认");
        UI.style(reset, UI.BTN, UI.BTN_GHOST);
        reset.setOnAction(e -> {
            try {
                avatarService.clearAvatar(person);
                preview.setImage(currentAvatarImage());
                AlertUtil.info("已恢复默认头像。");
                if (onProfileChanged != null) {
                    onProfileChanged.run();
                }
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });

        VBox buttons = new VBox(8, choose, reset);
        buttons.setAlignment(Pos.CENTER_LEFT);
        HBox row = new HBox(18, preview, buttons);
        row.setAlignment(Pos.CENTER_LEFT);

        Label tip = new Label("提示：支持 png / jpg / jpeg / gif / bmp，建议 5MB 以内的图片；"
                + "更换后登录页与顶栏都会显示新头像。");
        tip.setWrapText(true);
        UI.style(tip, UI.FAINT);

        return new VBox(10, head, row, tip);
    }

    /** 本人当前头像图片：已更换且能读出则用自定义头像，否则用默认校徽。 */
    private Image currentAvatarImage() {
        Image custom = UI.avatarImage(avatarService.avatarFileOf(person));
        return custom != null ? custom : UI.appIcon();
    }

    private void chooseAvatar(ImageView preview, Runnable onProfileChanged) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择头像图片");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File chosen = chooser.showOpenDialog(preview.getScene() == null ? null : preview.getScene().getWindow());
        if (chosen == null) {
            return; // 用户取消选择，不改动头像
        }
        try {
            avatarService.changeAvatar(person, chosen);
            preview.setImage(currentAvatarImage());
            AlertUtil.info("头像已更换。");
            if (onProfileChanged != null) {
                onProfileChanged.run();
            }
        } catch (BusinessException ex) {
            AlertUtil.error(ex.getMessage());
        }
    }

    /** 个人信息：展示当前姓名与唯一标识，并提供改名入口。 */
    private Node buildProfileSection(Runnable onProfileChanged) {
        Label head = new Label("个人信息");
        UI.style(head, UI.SECTION);

        Label current = new Label(profileText());
        UI.style(current, UI.MUTED);

        final TextField nameField = new TextField();
        nameField.setPromptText("请输入新的姓名");
        nameField.setPrefWidth(260);

        Button saveName = new Button("保存姓名");
        UI.style(saveName, UI.BTN, UI.BTN_PRIMARY);
        saveName.setOnAction(e -> {
            try {
                accountService.changeName(person, nameField.getText());
                nameField.clear();
                current.setText(profileText());
                AlertUtil.info("姓名修改成功，当前姓名：" + person.getName());
                if (onProfileChanged != null) {
                    onProfileChanged.run();
                }
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });
        nameField.setOnAction(e -> saveName.fire());

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.add(fieldLabel("新姓名："), 0, 0);
        form.add(nameField, 1, 0);
        form.add(saveName, 1, 1);

        Label tip = new Label(nameTip);
        tip.setWrapText(true);
        UI.style(tip, UI.FAINT);

        return new VBox(10, head, current, form, tip);
    }

    /** 修改密码：验证当前密码 + 两次输入新密码，一致方生效。 */
    private Node buildPasswordSection() {
        Label head = new Label("修改密码");
        UI.style(head, UI.SECTION);

        final PasswordField currentField = new PasswordField();
        currentField.setPromptText("请输入当前账号的密码");
        final PasswordField newField = new PasswordField();
        newField.setPromptText("请输入您的新密码");
        final PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("请确认您的新密码");
        // 未输入时显示框内提示文字；一旦输入（或获得焦点）提示文字即消失，无需额外处理
        for (PasswordField f : new PasswordField[]{currentField, newField, confirmField}) {
            f.setPrefWidth(260);
        }

        Button submit = new Button("确认修改密码");
        UI.style(submit, UI.BTN, UI.BTN_SUCCESS);
        submit.setOnAction(e -> {
            try {
                accountService.changePassword(person, currentField.getText(),
                        newField.getText(), confirmField.getText());
                currentField.clear();
                newField.clear();
                confirmField.clear();
                AlertUtil.info("密码修改成功，请牢记新密码。");
            } catch (BusinessException ex) {
                AlertUtil.error(ex.getMessage());
            }
        });
        confirmField.setOnAction(e -> submit.fire());

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.add(fieldLabel("当前密码："), 0, 0);
        form.add(currentField, 1, 0);
        form.add(fieldLabel("新密码："), 0, 1);
        form.add(newField, 1, 1);
        form.add(fieldLabel("确认新密码："), 0, 2);
        form.add(confirmField, 1, 2);
        form.add(submit, 1, 3);

        return new VBox(10, head, form);
    }

    private String profileText() {
        return "当前姓名：" + person.getName() + "　　" + idLabel + "：" + person.getId() + "（不可更改）";
    }

    /** 表单字段名（比 key-value 的 label 更醒目）。 */
    private Label fieldLabel(String text) {
        Label l = new Label(text);
        UI.style(l, UI.FIELD);
        return l;
    }

    private Node wrap(VBox box) {
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        UI.style(sp, UI.PAGE_SCROLL);
        return sp;
    }
}

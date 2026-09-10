package com.nchu.dorm.service;

import com.nchu.dorm.model.Account;
import com.nchu.dorm.model.Person;
import com.nchu.dorm.storage.DataCenter;
import com.nchu.dorm.util.BusinessException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/**
 * 头像服务：头像图片的文件管理与账号绑定。
 * <p>
 * 所选图片会被复制到 {@code data/avatars/} 下，文件名取「人员标识.扩展名」（如 {@code 25201101.png}），
 * 账号上只记录文件名——这样即使原图被移动或删除，头像依然可用，也便于整个 data 目录整体搬迁。
 * 本类只做文件与持久化，把文件转成 JavaFX 图片由界面层（{@code UI.avatarImage}）负责。
 */
public class AvatarService {

    /** 允许的图片扩展名（小写）。 */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg", "gif", "bmp");

    /** 头像图片大小上限：头像只作展示，过大的文件会拖慢启动与每次存档。 */
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private DataCenter dc() {
        return DataCenter.instance();
    }

    /**
     * 取某人已更换的头像文件；未更换或文件已丢失返回 {@code null}（由界面退回默认头像）。
     */
    public File avatarFileOf(Person person) {
        return person == null ? null : avatarFileOfUsername(person.getId());
    }

    /**
     * 按登录账号名取头像文件（登录页尚未登录，只能按输入框里的学号查）。
     */
    public File avatarFileOfUsername(String username) {
        Account account = dc().findAccount(username == null ? null : username.trim());
        if (account == null || account.getAvatar().isEmpty()) {
            return null;
        }
        File file = new File(dc().avatarDirectory().toFile(), account.getAvatar());
        return file.isFile() ? file : null;
    }

    /**
     * 更换头像：把所选图片复制进 {@code data/avatars/}，并把文件名记到该人员的登录账号上。
     *
     * @param source 用户在文件选择器里选中的原图
     * @return 复制后的头像文件
     * @throws BusinessException 未选择图片 / 格式不支持 / 图片过大 / 该人员没有登录账号
     */
    public File changeAvatar(Person person, File source) {
        Account account = requireAccount(person);
        if (source == null) {
            throw new BusinessException("请选择一张图片作为头像");
        }
        if (!source.isFile()) {
            throw new BusinessException("头像文件不存在：" + source.getName());
        }
        String extension = extensionOf(source.getName());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("头像仅支持 " + String.join(" / ", ALLOWED_EXTENSIONS) + " 格式的图片");
        }
        if (source.length() > MAX_BYTES) {
            throw new BusinessException("头像图片过大（" + (source.length() / 1024 / 1024)
                    + "MB），请选择 " + (MAX_BYTES / 1024 / 1024) + "MB 以内的图片");
        }

        String fileName = person.getId() + "." + extension;
        Path target = dc().avatarDirectory().resolve(fileName);
        try {
            Files.createDirectories(dc().avatarDirectory());
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("头像保存失败：" + e.getMessage());
        }
        // 扩展名变了才会生成新文件名，此时旧头像文件留着就是垃圾，删掉
        if (!fileName.equals(account.getAvatar())) {
            deleteAvatarFile(account.getAvatar());
        }
        account.setAvatar(fileName);
        dc().saveAll();
        return target.toFile();
    }

    /**
     * 恢复默认头像：删除已保存的头像文件并清空账号上的记录。
     *
     * @throws BusinessException 该人员没有登录账号
     */
    public void clearAvatar(Person person) {
        Account account = requireAccount(person);
        deleteAvatarFile(account.getAvatar());
        account.setAvatar("");
        dc().saveAll();
    }

    private Account requireAccount(Person person) {
        if (person == null) {
            throw new BusinessException("请先登录后再设置头像");
        }
        Account account = dc().findAccountByPersonId(person.getId());
        if (account == null) {
            throw new BusinessException("未找到 " + person.getId() + " 绑定的登录账号，无法设置头像");
        }
        return account;
    }

    private void deleteAvatarFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        try {
            Files.deleteIfExists(dc().avatarDirectory().resolve(fileName));
        } catch (IOException e) {
            // 删不掉旧头像不影响换头像本身，忽略即可
            System.err.println("[Avatar] 旧头像删除失败 " + fileName + "：" + e.getMessage());
        }
    }

    /** 取文件扩展名（小写，不含点）；无扩展名返回空串。 */
    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || dot == fileName.length() - 1 ? "" : fileName.substring(dot + 1).toLowerCase();
    }
}

# 南昌航空大学学生宿舍管理系统

面向对象课程设计：openjdk-25 + JavaFX 25（OpenJFX 25.0.4，外部库在 `D:\develop\javafx-25\lib`）+ 文本文件存储，四种角色。

## 运行（IntelliJ IDEA）

1. 用 IDEA 打开本目录。项目 SDK 为 **openjdk-25**，JavaFX 用 **OpenJFX 25.0.4**（装在 `D:\develop\javafx-25\lib\`）。
   - 项目已配好 `.idea`：模块已把 `src` 标为源码根、加入了 javafx 库；MainApp 运行配置已带 VM 参数 `--module-path "D:/develop/javafx-25/lib" --add-modules javafx.controls`。
   - 若报 javafx 找不到：`File → Project Structure → Modules` 检查 JavaFX 库，或确认 `D:\develop\javafx-25\lib` 下三个 jar 存在。
2. 打开 `src/com/nchu/dorm/MainApp.java`，点 `main` 方法左侧**绿色箭头** → Run 'MainApp'。
3. 首次运行自动在项目根目录生成 `data/` 演示数据。

> ⚠️ openjdk-25 不带 JavaFX，必须外挂 OpenJFX。运行配置里的 `--module-path ... --add-modules javafx.controls` 是必需的，别删。

## 演示账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 宿管科 | admin | admin123 |
| 辅导员 | counselor | 123456 |
| 宿舍管理人员 | ld001 | 123456 |
| 学生 | 24010101 | 123456 |

## 目录说明

- `src/com/nchu/dorm/`：源码（model 数据模型 / storage 存储 / service 业务 / ui 界面 / util 工具）
- `data/`：运行时文本数据（首次运行自动生成，可人工编辑）
- `md/`：文档 —— [markdown.md](md/markdown.md) 长期记忆 · [X1a0man01.md](md/X1a0man01.md) 初版更新记录 · [设计资料.md](md/设计资料.md) 调研与设计过程

## 当前功能（迭代一）

- ✅ 完整账号登录，按角色进入不同主界面
- ✅ 学生：提交宿舍入住申请、查看我的申请
- ✅ 学号为 8 位规则：入学年份后两位 + 学院代码 + 专业代码 + 班级代码 + 学号序号；转专业后学号不变（学号标识入学时信息，见 `StudentId` 类）
- ✅ 辅导员：审批本院申请并分配房间床位
- ⏳ 宿管科 / 宿舍管理人员端为占位工作台（功能预告），后续迭代实现

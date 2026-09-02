# X1a0man01.md —— 初版更新记录

> 版本：初版（迭代一） · 日期：2026-09-02
> 本文档记录本次迭代**做了什么、验证结果、踩坑修复、已知限制**。后续迭代请新建 X1a0man0X.md。

---

## 一、本次完成内容

1. **完整账号登录系统**（用户确认采用）
   - `Account` 模型 + `AuthService` 登录校验 + JavaFX 登录界面
   - 登录后按角色进入不同主界面（多态分发：`Person#getRoleKey()`）

2. **面向对象类体系**（封装 / 继承 / 接口 / 多态）
   - 继承链：`Person`（抽象）→ `Student`；`Person` → `Staff`（抽象）→ `Counselor` / `DormStaff` / `Admin`
   - 接口：`RoleCapable`（角色能力）、`Storage`（存储）
   - 多态：抽象方法 `getRoleName() / getRoleKey() / getDutyDescription()` 各子类实现；`AuthService.login` 返回 `Person` 基类
   - 组合链：`Building → Room → Bed`（宿舍信息）；学院 → 楼栋
   - 数据模型：`College / Building / Room / Bed / Account`
   - 业务模型：`DormApplication`（申请/转移/退出）、`RepairTicket`、`ElectricityPurchase`
   - 日常记录：`NightReturnRecord`、`HygieneRecord`、`ValuablesRecord`

3. **文件存储**（文本文件）
   - `Storage` 接口 + `TextStorage` 实现：`data/` 目录，每实体一个 `.txt`，`|` 分隔，`#` 文件头注释
   - `DataCenter` 单例门面：加载/保存 + 查询 + 编号生成；首次运行自动生成南昌航空大学演示数据

4. **业务闭环（核心验证点）**
   - 学生：登录 → 提交入住申请（楼栋限本院分配范围）→ 查看"我的申请"
   - 辅导员：查看本院待审批申请 → 选择空床房间 → 通过并分配 / 驳回
   - 审批通过后同步更新：房间床位占用、学生当前宿舍、申请状态

5. **界面（JavaFX 8，全中文）**
   - `LoginView`（含演示账号提示）
   - `MainFrame`（顶部用户栏 + 左侧按角色菜单 + 右侧内容区）
   - `StudentView`（我的宿舍 / 宿舍申请 / 我的申请 三个 Tab）
   - `CounselorView`（待审批列表 + 审批分配面板）
   - `DormStaffView` / `AdminView`：占位工作台（功能预告 + 系统概览）

6. **文档**：本文件 + [markdown.md](markdown.md)（长期记忆）+ [设计资料.md](设计资料.md)（调研/设计/迭代）

## 二、验证结果

- 编译：`javac -encoding UTF-8 -d out @sources.txt` ✅ 零错误
- 冒烟测试（无 GUI）：17 学院 / 32 楼栋 / 56 房间 / 6 学生 / 9 账号 ✅
- 登录 ✅ · 错误密码拦截 ✅ · 学生提交申请（AP0003→PENDING）✅
- 辅导员待审批=3 ✅ · 审批通过分配 1栋-103 ✅ · 学生宿舍更新 ✅
- 1栋-103 空床数 = 3（容量 4）✅ · 数据文件回读一致 ✅
- JavaFX 界面：`java -cp out com.nchu.dorm.MainApp` 启动后稳定运行 15 秒（timeout 终止）✅

## 三、踩坑与修复

| 问题 | 原因 | 修复 |
|------|------|------|
| `TextStorage.hasFile` 抛 NPE | `DataCenter` 静态字段 `INSTANCE` 先于 `DATA_DIR` 初始化，构造时读到 null | 把 `DATA_DIR` 声明移到 `INSTANCE` 之前 |
| 冒烟测试遗留脏数据 | 测试改写了 data/ | 测试后 `rm -rf data`，首次启动自动重建干净种子数据 |

## 四、数据文件清单（data/，均带字段说明文件头）

`accounts.txt` · `admins.txt` · `buildings.txt` · `colleges.txt` · `counselors.txt` · `dorm_applications.txt` · `dorm_staffs.txt` · `electricity_purchases.txt` · `hygiene_records.txt` · `night_return_records.txt` · `repair_tickets.txt` · `rooms.txt` · `students.txt` · `valuables_records.txt`

## 五、运行方式（IntelliJ IDEA）

1. 用 IDEA 打开项目根目录。项目 SDK 为 **openjdk-25**，JavaFX 用 **OpenJFX 25.0.4**（`D:\develop\javafx-25\lib\`，3 个 win jar）。
2. 主类：`com.nchu.dorm.MainApp`，点击 `main` 左侧绿色箭头运行。运行配置已含 VM 参数 `--module-path "D:/develop/javafx-25/lib" --add-modules javafx.controls`。
3. 首次运行会在项目根目录自动生成 `data/` 演示数据。
4. 若报 javafx 包不存在：检查 `.idea` 模块里的 javafx 库是否在、`D:\develop\javafx-25\lib` 三个 jar 是否存在。
5. 原初版曾用 JDK 8 + JavaFX 8（内置 jfxrt.jar）方案，后按用户要求改用 openjdk-25 + OpenJFX 25；JDK 8 仍在本机（`D:\develop\Java\.jdks\jdk`），可随时切回。

## 六、已知限制 / 后续迭代

- **学生**：转移、退出、购电、维修申请尚未形成闭环（模型与存储已就绪，UI 未开放）。
- **宿管科**：楼栋分配、宿舍管理人员管理、修理、售电为占位。
- **宿舍管理人员**：夜归、卫生、贵重物品出入登记为占位。
- 无性别-楼栋校验、无复杂输入校验、无报表导出。
- 密码明文存储（课程演示，正式系统需加密）。

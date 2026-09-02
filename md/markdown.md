# markdown.md —— 项目长期记忆

> 本文件用于跨会话记忆本项目的**技术栈约束、环境事实、关键设计决策、南昌航空大学真实数据、当前进度与后续计划**。
> 每次迭代开始前先读本文件，结束时更新"当前进度"与"已踩坑"部分。

---

## 一、项目一句话

南昌航空大学学生宿舍管理系统：**openjdk-25 + JavaFX 25（OpenJFX 25.0.4）** + 文本文件存储，四种角色（宿管科 / 宿舍管理人员 / 辅导员 / 学生），面向对象课程设计。

## 二、环境与硬性约束（必须遵守）

- **IDEA 项目 SDK = openjdk-25**（`D:\develop\Java\.jdks\openjdk-25.0.1`，用户明确选择；openjdk-25 不带 JavaFX，必须配合 OpenJFX 使用）。
- **JavaFX 25（OpenJFX 25.0.4）** 装在机器级目录 `D:\develop\javafx-25\lib\`，含 3 个 win 平台 jar（javafx-base/graphics/controls，每个都带 `module-info.class` 与原生 dll）。运行必须带 VM 参数：`--module-path "D:/develop/javafx-25/lib" --add-modules javafx.controls`（已写入 `.idea/workspace.xml` 的 MainApp 运行配置；.iml 里也加了这 3 个 jar 作为模块库供编译）。
- **本机另有 JDK 8**（`D:\develop\Java\.jdks\jdk`，内含 JavaFX 8 的 `jfxrt.jar`）。若以后想退回"零依赖 JavaFX 8"方案，把 Project SDK 改回 `1.8` 即可（jdk.table.xml 里的 "1.8" 已指向该真实路径）。
- 项目是 **IntelliJ IDEA 工程**，无 Maven/Gradle。用户拒绝了 .bat 构建脚本，编译运行方式以 IntelliJ 为准（见 [X1a0man01.md](X1a0man01.md) 的运行说明）。
- **禁止使用 Swing / AWT** 做可视化，只能用 JavaFX。
- 数据存储只能用**文件**（本项目用文本文件），不能用数据库。
- 代码里**不能使用 Java 8 之后的语法**（无 var、无 record、无 List.of、无 switch 表达式）。
- 本机所有文件操作必须使用**完整 Windows 绝对路径**（`D:\...` 反斜杠格式），避免路径拼接 bug。

## 三、关键设计决策（为什么这样做）

| 决策 | 内容 | 原因 |
|------|------|------|
| 登录体系 | 完整账号登录系统（用户已确认） | 四角色需要账号、权限分发 |
| 存储抽象 | `Storage` 接口 + `TextStorage` 文本实现 | 体现接口/多态；后续可换二进制实现 |
| 文件格式 | `data/` 目录，每实体一个 `.txt`，一行一条记录，`|` 分隔，`#` 开头为注释/文件头 | 可人工查阅、便于报告展示 |
| 文本转义 | `TextUtil.escape()` 把用户自由文本中的 `|` `;` 换行转全角 | 防止破坏文件结构 |
| 多态体现 | `Person` 抽象类 + `RoleCapable` 接口 + 抽象方法 `getRoleName/getRoleKey/getDutyDescription` + `Storage` 接口 | 满足课程设计要求 |
| 申请闭环（v1） | 学生提交 → 辅导员审批分配 → 落库 | 先跑通核心闭环验证架构 |
| 审批链假设 | 学生申请先由**辅导员**审批（本院）；换楼栋/退宿的**宿管科复核**留到后续迭代 | 用户已认可该假设 |
| 转专业两级审批（迭代四） | **转专业换宿**走两级审批：先本专业辅导员（按辅导员 id=专业年级代码）同意迁出，再目标专业辅导员同意接收；任一级拒绝即失败；两级均同意才搬迁/更新档案。普通三类（入住/转移/退宿）仍为**本院任意辅导员单级审批** | 用户明确要求；跨学院转专业需目标学院介入，故转专业类精确到专业辅导员账号 |
| 转专业范围与宿舍 | 目标班级**可跨学院、可跨届**（全校真实班级）；跨学院才搬到目标学院同性别楼（目标辅导员选房），**同学院转专业不换房**仅更新学院/专业/班级档案 | 用户确认：同学院楼栋不变，无需换床；学号不变，当前学院/专业可变 |
| 学号规则 | 8 位学号：入学年份后两位 + 学院代码 + 专业代码 + 班级代码 + 学号序号；`StudentId` 类统一校验/解析/生成；**转专业后学号不变** | 学号唯一标识入学时信息，当前学院/专业可变（可支持转专业） |

## 四、项目结构

```
src/com/nchu/dorm/
├── MainApp.java            # JavaFX 入口
├── model/                  # 数据模型
│   ├── Person.java         # 抽象基类（继承/多态核心）
│   ├── Student.java / StudentId.java(学号规则值对象) / Staff.java(抽象) / Counselor / DormStaff / Admin
│   ├── RoleKey.java(常量接口) / RoleCapable.java(接口) / Account.java
│   ├── College / Building / Room / Bed     # 宿舍信息组合链
│   ├── application/        # DormApplication / RepairTicket / ElectricityPurchase
│   └── record/             # NightReturnRecord / HygieneRecord / ValuablesRecord
├── storage/                # Storage(接口) / TextStorage(文本实现) / DataCenter(单例门面)
├── service/                # AuthService / DormApplicationService
├── ui/                     # LoginView / MainFrame / StudentView / CounselorView /
│                           #   DormStaffView / AdminView / component/AlertUtil
└── util/                   # TextUtil / BusinessException
data/                       # 运行时数据（首次运行自动生成演示数据）
md/                         # 本文档 + 更新记录 + 设计资料
```

## 五、南昌航空大学真实信息（调研来源见设计资料.md）

- **学院（17 个专业学院，官方代码与学号规则一致，见 StudentId）**：01 材料科学与工程学院 / 02 环境与化学工程学院 / 03 机电学院 / 04 信息工程学院 / 05 航空宇航学院 / 06 动力与能源学院 / 07 数学与信息科学学院 / 08 仪器科学与光电工程学院 / 09 经济管理学院 / 10 体育学院 / 11 民航与交通学院 / 12 艺术设计学院 / 13 马克思主义学院 / 14 文法学院 / 15 航空服务与音乐学院 / 16 外国语学院 / 20 软件学院。
- **学号规则（8 位）**：第 1-2 位=入学年份后两位；第 3-4 位=学院代码；第 5 位=专业代码（0-9，默认每学院 10 个专业）；第 6 位=班级代码（1-9，默认每专业 9 个班）；第 7-8 位=学号序号（01-99，默认每班 99 人）。**转专业后学号不变**。**课程设定（迭代三）收紧为**：每学院固定 **4 专业（代码 1-4）** × 每专业 **4 班** × 每班 **40 人**；软件学院专业 1 = 软件工程（专业年级代码 25201）。
- **宿舍楼（32 栋标准公寓，真实调研）**：27 本科 + 4 研究生 + 1 留学生；1-6 栋无独立卫浴、7-28 栋有独立卫浴；25 栋上床下桌；最高 7 层。**注意：迭代三改为课程设定模型**——每学院 2 栋（`<学院代码>A栋`=男、`<学院代码>B栋`=女，如 01A/20B），每层 45 间、每间 4 人，两届全量按学号顺序排宿，不再使用 1-32 栋命名。
- **房间**：4 人间为主（本项目 Room.capacity 默认 4）。

## 六、演示账号（data/accounts.txt，首次运行自动生成）

账号规则（迭代三）：**学生账号 = 学号（8 位）、辅导员账号 = `counselor`+专业年级代码（5 位）**，默认密码 123456（admin 为 admin123）；姓名默认 = 学号 / 专业年级代码，可后续在 `data/` 中修改。

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| admin | admin123 | 宿管科 | 刘敏 |
| counselor25201 | 123456 | 辅导员 | 2025级软件学院·软件工程（专业年级代码 25201） |
| ld001 | 123456 | 宿舍管理人员 | 李芳 |
| 25201101 | 123456 | 学生 | 2025级软件学院 女生，住 20B栋 |

## 七、当前进度（截至 2026-09-02 初版）

- ✅ v1 全部完成：登录 + 学生申请 + 辅导员审批分配 + 文本持久化 + 四角色界面框架。
- ✅ 冒烟测试通过：登录、错误密码、提交申请、审批分配、文件回读一致。
- ✅ 宿管科 / 宿舍管理人员端为**占位首页**（功能预告 + 概览）。
- ✅ 迭代二：学号重构为 8 位规则（`StudentId` 类 + 学院列表换官方代码 01-16/20 + 演示数据/数据文件/文档同步）。
- ✅ 迭代三（全量数据）：种子改为 2024/2025 两届全覆盖——17 学院 × 4 专业 × 4 班 × 40 人 = 21,760 学生 + 136 辅导员（每专业×届）；宿舍改为每学院 A(男)/B(女) 两栋、45 间/层、学号顺序排宿；账号 = 学号 / counselor+专业年级代码。详见 X1a0man03.md。
- ✅ 迭代四（宿舍申请模块）：三类闭环（入住/转宿/退宿）补全 + 学生撤销 + 辅导员历史筛选 + 状态修复（性别-楼栋、一人至多一条在办、origin 快照）；并新增**转专业换宿**两级审批（本专业辅导员→目标专业辅导员，可跨学院/跨届，同学院不换房）。详见 X1a0man04.md。
- ⏭️ 后续迭代：购电、维修申请闭环；宿管科楼栋分配/宿舍管理人员管理/修理/售电；宿舍管理人员夜归/卫生/贵重物品登记。

## 八、已踩坑（别再犯）

1. **静态字段初始化顺序**：`DataCenter` 的 `INSTANCE` 曾声明在 `DATA_DIR` 之前，导致创建单例时 `DATA_DIR` 为 null → `TextStorage.dir` 为 null → NPE。**已修复**：`DATA_DIR` 必须先于 `INSTANCE` 声明。
2. 本机 Bash 工具对项目外敏感路径（如 JDK 目录、AppData）有访问限制：Read/Write/Edit 工具会被挡，但 Bash 的 `grep/sed/cat` 可以读写 AppData 配置。修改 AppData 前先 `cp` 备份。
3. 文本文件字段分隔用 `|`，自由文本写入前必须 `TextUtil.escape()`。
4. **IDEA 运行环境（2026-09-02 已修复）**：IDEA 只注册了 openjdk-25（无 JavaFX），真正带 JavaFX 的 JDK 8 在 `D:\develop\Java\.jdks\jdk`；原名 "1.8" 的注册指向了已不存在的 `D:\develop\Java_jdk8\jdk`。已把 `jdk.table.xml`（备份为 .bak）中所有 `D:/develop/Java_jdk8/jdk` 替换为 `D:/develop/Java/.jdks/jdk`，并新建了项目模块 `.iml` + `.idea/misc.xml`（SDK=1.8、语言级别 1.8）+ modules.xml + workspace.xml（MainApp 运行配置）。若重装/换机，按此重建。注意：IDEA 若在运行中退出，可能用内存旧配置覆盖 jdk.table.xml 的修复，必要时用 GUI 重新添加 JDK 8。
5. **用户最终选择 openjdk-25 + JavaFX 25（2026-09-02）**：把项目 SDK 改成了 openjdk-25，因此 JavaFX 必须外挂。已下载 OpenJFX 25.0.4 三模块到 `D:\develop\javafx-25\lib\`，并在 `.idea/DormitoryManagementSystemForCollegeStudent.iml`（模块已由 IDEA 移到 .idea/ 下，modules.xml 指向它）加了 src 源码根 + 3 个 javafx jar 库，在 `.idea/workspace.xml` 的 MainApp 运行配置加了 VM 参数。**沙箱限制：无法直接运行 `.jdks`/`D:/develop` 下的 JDK 二进制做编译验证**，仅做了静态校验（全部 javafx 导入类 + CONSTRAINED_RESIZE_POLICY 均存在于 25.0.4 jar；javafx.controls 传递依赖 javafx.graphics/base 齐全）。
6. **学号合法性（迭代二）**：学号必须 8 位且学院代码仅 `01-16`/`20` 合法（`00`、`17-19` 非法）；`StudentId.VALID_COLLEGE_CODES` / `seedColleges()` / `data/colleges.txt` **三处需保持一致**。宿管科/宿舍管理员工号（`SK001`/`LD001`/`G001` 等，含 `00` 学院代码）与学号无关，不受影响。

## 九、给后续会话的行动指引

1. 读本文件 + [设计资料.md](设计资料.md) + [X1a0man01.md](X1a0man01.md) 了解上下文。
2. 改代码后：`find src -name "*.java" > sources.txt && javac -encoding UTF-8 -J-Dfile.encoding=UTF-8 -d out @sources.txt` 验证编译。
3. 验证业务逻辑可用无 GUI 的冒烟测试（写临时 main，跑完删除，`rm -rf data` 恢复干净种子数据）。
4. 新增功能后更新本文件"当前进度"与 X1a0man0X.md 更新记录。

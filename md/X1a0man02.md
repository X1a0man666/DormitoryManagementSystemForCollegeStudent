# X1a0man02.md —— 迭代二更新记录（学号规则重构）

> 版本：迭代二 · 日期：2026-09-02
> 本次迭代把学生学号从占位字符串重构为南昌航空大学 8 位规则学号。

---

## 一、本次完成内容

1. **新增 `StudentId` 值对象**（`src/com/nchu/dorm/model/StudentId.java`）
   - 8 位学号规则：第 1-2 位=入学年份后两位；第 3-4 位=学院代码；第 5 位=专业代码(0-9)；第 6 位=班级代码(1-9)；第 7-8 位=学号序号(01-99)
   - 提供：`isValid / parseOrNull / parse / of(生成) / getAdmissionYear / getFullAdmissionYear / getCollegeCode / getMajorCode / getClassCode / getStudentNo`
   - 明确语义：**学号一经生成不变（转专业后学号不变），编码的是入学时信息**

2. **学院列表替换为官方代码**（`DataCenter.seedColleges()` + `data/colleges.txt`）
   - 由初版占位列表（01-17：航空制造与机械工程学院、飞行器工程学院、通航学院(民航学院)等）替换为官方 17 学院（01-16 + 20）
   - 楼栋→学院分配保留（1/2→01、3/4→02、5→03、6→04、7→06、8→08），`buildings.txt` 无需改动

3. **演示学生改为合规 8 位学号**（`DataCenter.seedPersons/seedAccounts/seedRooms/seedApplications` + `data/students/accounts/rooms/dorm_applications.txt`）

| 姓名 | 新学号 | 解析 | 专业 | className | 入住 |
|------|--------|------|------|-----------|------|
| 张三 | `24010101` | 24\|01\|0\|1\|01 | 金属材料工程 | 240101 | 1栋101 |
| 李四 | `24010102` | 24\|01\|0\|1\|02 | 金属材料工程 | 240101 | 1栋101 |
| 王五 | `24011201` | 24\|01\|1\|2\|01 | 材料成型及控制工程 | 240112 | 1栋102 |
| 赵六 | `24012301` | 24\|01\|2\|3\|01 | 焊接技术与工程 | 240123 | 未入住 |
| 钱七 | `24013401` | 24\|01\|3\|4\|01 | 高分子材料与工程 | 240134 | 未入住 |
| 孙八 | `24020101` | 24\|02\|0\|1\|01 | 环境工程 | 240201 | 未入住 |

4. **界面展示学号解析信息**（`StudentView.buildMyDorm()`）
   - "我的宿舍"新增由学号解析出的入学信息：入学年份/入学学院/专业代码/班级代码/学号序号
   - 与当前学院/专业/班级并列展示，体现"学号不变、当前信息可变"；非法学号优雅降级

5. **登录提示更新**（`LoginView`）→ 学生演示账号 `24010101`

## 二、验证结果（沙箱无法运行 D:\develop 下 JDK，采用静态一致性校验）

- ✅ 6 个新学号均 8 位纯数字，学院代码 ∈ {01..16,20}，班级代码 1-9，学号序号 01-99
- ✅ `StudentId.VALID_COLLEGE_CODES` == `colleges.txt` 代码 == {01..16,20}（三处一致）
- ✅ 种子方法与 `data/` 各文件逐行一致（students/accounts/rooms/dorm_applications/colleges）
- ✅ 引用完整性：accounts STUDENT 行 personId=username=学号且 ∈ students；rooms/dorm_applications 的 studentId ∈ students；students/buildings 的 collegeCode ∈ colleges（员工 00 豁免）
- ✅ 新文件 Java 8 语法审计（无 var/record/List.of/switch 表达式）
- ✅ 工作流合理性：赵六/钱七（学院01）可申请 1 栋，辅导员 G001（学院01）可审批；张三已入住

## 三、已知限制 / 后续

- 未加入"转专业学生"演示账号（用户确认不加）；转专业语义仅体现在 `StudentId` 规则与界面"入学信息 vs 当前信息"的对照中。
- 学号规则类未接入登录/注册校验（当前无学生注册流程），仅用于界面展示与种子生成。
- 后续迭代仍按计划推进：转移/退出、购电、维修、宿管科楼栋分配等。

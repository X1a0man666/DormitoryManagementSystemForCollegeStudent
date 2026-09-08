# X1a0man04.md —— 迭代四更新记录（初版宿舍申请模块 + 转专业换宿两级审批）

> 版本：迭代四 · 日期：2026-09-02
> 本次按 `宿舍申请.md` 思路实现"初版宿舍申请模块"，并按其要求**新增「转专业更换宿舍」**类型：
> 学生选目标班级 → **本专业辅导员同意迁出 → 目标专业辅导员同意接收**，两级均同意才成功，任一级拒绝即失败。

---

## 一、本次完成内容

### 1. 宿舍申请四类闭环

| 类型 | 前置 | 通过时的数据变更 |
|------|------|------------------|
| 入住 APPLY | 未入住 | 目标楼栋（本院+性别匹配）占空床，写 current* |
| 转宿 TRANSFER | 已入住 | 释放原房床位 → 新楼栋/房间占空床 → 写 current* |
| 退宿 EXIT | 已入住 | 释放原床位 + 清空 current*（变未入住） |
| **转专业换宿 MAJOR_TRANSFER** | 已入住 | 见下方"两级审批" |

### 2. 转专业换宿两级审批（本次新增）

- 学生提交时选择**目标班级**（全校真实班级，**可跨学院、可跨届**，排除本班/本专业年级），并写 origin 快照。
- 第一级：**本专业辅导员**（辅导员 id = 学生当前班级的专业年级代码，前 5 位）在"宿舍审批"待处理中同意迁出
  → 状态 `AWAITING_TARGET`（待目标专业审批），**床位暂不动**（目标拒绝可回退原位）。
- 第二级：**目标专业辅导员**（id = 目标班级前 5 位）同意接收：
  - **跨学院**：释放原房床位 → 目标学院同性别楼占新床 → 更新学生 学院/专业/班级 档案（学号不变）；
  - **同学院**：不换房，仅更新 专业/班级 档案。
- 任一级驳回 → `REJECTED`，学生档案/床位**无任何变更**；两级都记录痕迹（step1 迁出 + 最终接收）。

### 3. 状态修复与体验（宿舍申请.md 所提缺口）

- 性别-楼栋匹配：A栋=男 / B栋=女，提交与审批双层校验。
- 一人至多一条在办（PENDING / AWAITING_TARGET 均计）。
- 学生可**撤销**自己待审批（含等待目标专业阶段）的申请。
- 新增状态 `CANCELLED`（已撤销）与 `AWAITING_TARGET`（待目标专业审批）。
- origin 快照列（originBuilding/originRoom/originClass）保证退宿/转宿/转专业后历史仍可追溯原宿舍与原班级。

### 4. 界面

- **学生端「宿舍申请」**：按居住状态展示类型（入住 / 转宿 / 退宿 / 转专业换宿），切换类型重排表单；在办黄字提示并禁提交。
- **学生端「我的申请」**：状态筛选 + 原宿舍/目标班级列 + 「撤销选中申请」。
- **辅导员端「宿舍审批」**（原"待审批申请"更名）：范围筛选（待处理/已通过/已驳回/已撤销/全部）；右侧面板按类型/阶段自适应
  （入住/转宿选房、退宿免选房、转专业 PENDING→"同意迁出"、转专业 AWAITING_TARGET→"通过并接收"、历史只读展示两级痕迹）。
- 普通三类（入住/转宿/退宿）审批粒度维持"本院任意辅导员"；仅转专业类精确到本专业/目标专业辅导员账号。

## 二、改动文件

- `model/application/DormApplication.java`：+`TYPE_MAJOR_TRANSFER`、+`STATUS_CANCELLED/AWAITING_TARGET`、+origin/target/step1 列（11→18 列，fromLine 向后兼容）、类型/状态中文名。
- `service/DormApplicationService.java`：`submitApply`(加固)/`submitTransfer`/`submitExit`/`submitMajorTransfer`；
  `approve`/`approveExit`/`approveMajorMoveOut`/`approveMajorAccept`/`reject`/`cancel`；
  `findActionableOf`/`findHistoryOf`；班级编码静态工具（collegeOfClass/gradeOfClass/isRealClass/classLabel）。
- `storage/DataCenter.java`：申请文件表头 18 列；+`findCounselorById`。
- `ui/StudentView.java`、`ui/CounselorView.java`、`ui/MainFrame.java`（菜单"宿舍审批"）、`ui/component/AlertUtil.java`（+confirm）。
- `data/dorm_applications.txt`：表头更新（无数据行）。
- 文档：README / markdown.md / 设计资料.md / 本文件。

## 三、验证结果

- ✅ `javac -encoding UTF-8` 全量编译通过（JDK 8 内置 JavaFX，仅静态验证）。
- ✅ 无 GUI 冒烟（在 `data/` 副本上跑临时 main，跑后删除）：S1 退宿→床位释放、S2 再入住、S3 转宿换房+origin 快照、
  S4 **跨学院转专业两级同意**（同意迁出床位不动 → 16B 接收后学院/班级更新、20B 原床位释放）、
  S5 目标辅导员拒绝（档案/床位不变）、S6 同学院转专业两级通过（不换房，仅档案更新）、
  S7 撤销/重复在办拦截/性别楼拦截/越权与陈旧对象拦截、S8 辅导员待办与历史可见性（含跨学院目标专业可追溯）。
- 输出 `SMOKE OK`（exit 0）。

## 四、已知限制 / 后续

- 宿舍楼命名仍约定 `<学院代码>A栋`=男 / `B栋`=女；若未来使用真实楼栋名，需给 `Building` 加 gender 字段。
- 转专业跨学院成功即改写学生 `collegeCode/major/className`；此后其后续申请将自动归属目标学院（学号不变，符合设计）。
- 审批即生效、无宿管科二级复核（沿用既有假设）；错操作需反向流程（如误跨院转专业可再转回）。
- 全量种子学生均已入住，故"入住申请"主要出现在退宿后再入住的场景。

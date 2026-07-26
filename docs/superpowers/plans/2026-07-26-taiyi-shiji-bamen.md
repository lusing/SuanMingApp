# 太乙时计八门王希明法 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 时计直使门改按《太乙金镜式经》卷一王希明法（积时 mod 120 ÷ 30 + 阴阳遁四门池），替换现行误用的年计 mod 240÷30 八门公式。

**Architecture:** 纯算法修正，收口在 `TaiyiCalculator.computeZhiShiMen` 一处；`TaiyiMen` 增两个四门池常量。年计分支、`computeMenOnTaiyi` 飞布、格局判定、UI 全不动。依据 spec：`docs/superpowers/specs/2026-07-26-taiyi-shiji-bamen-design.md`。

**Tech Stack:** Kotlin / Android (Gradle)，JUnit4 单元测试。

## Global Constraints

- 测试命令：`./gradlew :app:testDebugUnitTest`（单类加 `--tests "cn.xulun.bazi.TaiyiCalculatorTest"`），工作目录 `G:\code\zhouyi\bazia`。
- 门池（卷一原文，张良时序=王希明序）：阳遁 `[开,生,惊,休]`、阴遁 `[杜,死,伤,景]`；30时一移、120时一周。
- 两遁共用连续绝对积时（suanBase）同轨，仅换门池；**不加**卷一夏至加时实 +2192 平气偏移（用户裁定）。
- 年计公式 `积年 mod 240 ÷ 30 → TaiyiMen.order` 一字不动（已合卷一岁计法，开元十二年开门锚）。
- 时计公式：`门数 = floorMod(suanBase, 120) / 30`（0..3）。

---

### Task 1: 时计八门王希明法（测试 + 实现）

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/model/Taiyi.kt:104-107`（`TaiyiMen` companion object）
- Modify: `app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt:65`（调用点）和 `:90-95`（`computeZhiShiMen`）
- Test: `app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt`

**Interfaces:**
- Consumes: 现有 `TaiyiMen` enum（成员 `KAI, XIU, SHENG, SHANG, DU, JING, SI_MEN, JING2`；`JING`=景、`JING2`=惊、`SI_MEN`=死）；`TaiyiJu.dun: TaiyiDun`、`TaiyiJu.suanBase: Long`；`TaiyiDun { YANG, YIN }`。
- Produces: `TaiyiMen.poolYang: List<TaiyiMen>`、`TaiyiMen.poolYin: List<TaiyiMen>`；`computeZhiShiMen(suanBase: Long, dun: TaiyiDun, type: TaiyiCountType): TaiyiMen`（private）。`TaiyiPlate.zhiShiMen` 字段不变，下游（UI/`执提`格局）无感。

- [ ] **Step 1: Write the failing tests**

在 `TaiyiCalculatorTest.kt` 既有 `` `2024 年计八门 直使为伤门` `` 测试之后追加：

```kotlin
    // ── 时计八门：卷一王希明法（积时 mod 120 ÷ 30，阳遁池开生惊休/阴遁池杜死伤景）──
    @Test
    fun `时计阳遁 2023冬至午时 直使开门`() {
        // 2023-12-22 12:30 已过冬至时刻(约11:27)→阳遁；积时 mod 120 = 6 → 门数0
        val p = TaiyiCalculator.build(LocalDateTime.of(2023, 12, 22, 12, 30), TaiyiCountType.HOUR)
        assertEquals(TaiyiDun.YANG, p.ju.dun)
        assertEquals(TaiyiMen.KAI, p.zhiShiMen)
        assertEquals(TaiyiMen.KAI, p.menOnTaiyi[p.ju.taiyiPalaceNo]) // 直使落太乙宫
    }

    @Test
    fun `时计阳遁 四门轮转 30时一移`() {
        // 积时 mod 120 = 36/66/96 → 门数1/2/3 → 生/惊/休；126→6 → 门数0 → 开
        assertEquals(TaiyiMen.SHENG,
            TaiyiCalculator.build(LocalDateTime.of(2023, 12, 25, 0, 30), TaiyiCountType.HOUR).zhiShiMen)
        assertEquals(TaiyiMen.JING2,
            TaiyiCalculator.build(LocalDateTime.of(2023, 12, 27, 12, 30), TaiyiCountType.HOUR).zhiShiMen)
        assertEquals(TaiyiMen.XIU,
            TaiyiCalculator.build(LocalDateTime.of(2023, 12, 30, 0, 30), TaiyiCountType.HOUR).zhiShiMen)
        assertEquals(TaiyiMen.KAI,
            TaiyiCalculator.build(LocalDateTime.of(2024, 1, 1, 12, 30), TaiyiCountType.HOUR).zhiShiMen)
    }

    @Test
    fun `时计阴遁 2024-10-09子时 直使杜门`() {
        // 积时 mod 120 = 24 → 门数0 → 阴遁池[杜,死,伤,景][0]
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 10, 9, 0, 30), TaiyiCountType.HOUR)
        assertEquals(TaiyiDun.YIN, p.ju.dun)
        assertEquals(TaiyiMen.DU, p.zhiShiMen)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest"`
Expected: FAIL——三个新测试中断言不符（现行时计走 mod 240÷30 八门序，如 2023-12-22 得 `floorMod(suanBase,240)/30` 对应的门而非开门）。

- [ ] **Step 3: Implement**

`app/src/main/java/cn/xulun/bazi/model/Taiyi.kt` 的 `TaiyiMen` companion object 中，`order` 之后追加：

```kotlin
        /** 时计阳遁四门池（卷一王希明法，张良时序）：开→生→惊→休，30时一移、120时一周。 */
        val poolYang: List<TaiyiMen> = listOf(KAI, SHENG, JING2, XIU)
        /** 时计阴遁四门池（卷一王希明法，张良时序）：杜→死→伤→景，30时一移、120时一周。 */
        val poolYin: List<TaiyiMen> = listOf(DU, SI_MEN, SHANG, JING)
```

`app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt` 替换 `computeZhiShiMen`（原 90-95 行）：

```kotlin
    /**
     * 直使门。
     * 年计：order[积年 mod 240 / 30]（卷一「推八門占嵗計法」命起开门以次休生门；开元十二年甲子开门锚）。
     * 时计：王希明法——门数 = 积时 mod 120 ÷ 30，阳遁池[开,生,惊,休]、阴遁池[杜,死,伤,景]，30时一移
     * （卷一「推八門用法」王希明「门则随数起，时尽则移，不拘其甲丙己辛日」+「推冬至/夏至加時所直門法」；
     *  夏至法阙文「命起杜门以次而行」依王希明阴遁四门序补；两遁连续积时同轨仅换池，不加 +2192 平气偏移——用户裁定 2026-07-26）。
     */
    private fun computeZhiShiMen(suanBase: Long, dun: TaiyiDun, type: TaiyiCountType): TaiyiMen {
        if (type == TaiyiCountType.YEAR) {
            val idx = floorMod(suanBase + 1, 240).toInt() / 30
            return TaiyiMen.order[idx]
        }
        val menShu = floorMod(suanBase, 120).toInt() / 30
        return (if (dun == TaiyiDun.YANG) TaiyiMen.poolYang else TaiyiMen.poolYin)[menShu]
    }
```

调用点（原 65 行 `val zhiShiMen = computeZhiShiMen(ju.suanBase, type)`）改为：

```kotlin
        val zhiShiMen = computeZhiShiMen(ju.suanBase, ju.dun, type)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest"`
Expected: PASS（含既有 `` `2024 年计八门 直使为伤门` `` 回归）。

- [ ] **Step 5: Full regression**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全量测试 PASS，无回归。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/cn/xulun/bazi/model/Taiyi.kt app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt
git commit -m "feat(太乙): 时计直使门改卷一王希明法(积时mod120÷30+阴阳遁四门池)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: 文档同步（参考文档 §12 + memory）

**Files:**
- Modify: `.taiyi-reference.md:131`（§12 直使求法行）
- Modify: `G:\xulun\.claude\projects\G--code-zhouyi-bazia\memory\taiyi-feature.md`（P4 待办 → P4-a 完成）
- Modify: `G:\xulun\.claude\projects\G--code-zhouyi-bazia\memory\build-environment.md`（git 状态过时行）

**Interfaces:**
- Consumes: Task 1 已合入的实现（门池常量名 `poolYang`/`poolYin`、公式 `积时 mod 120 ÷ 30`）。
- Produces: 文档与代码口径一致；无代码接口。

- [ ] **Step 1: 改写 `.taiyi-reference.md` §12 直使求法行**

将 §12 中这一行：

```
- **直使求法**：年计 `积年 mod 240 ÷ 30 → [开,休,生,伤,杜,景,死,惊]`（30年一移）；时计 张良法（甲日开/丙日生/己日惊/辛日休起，30时一移，阴遁反此）。
```

替换为：

```
- **直使求法**（卷一原文三说调和，2026-07-26 核定）：年计 `积年 mod 240 ÷ 30 → [开,休,生,伤,杜,景,死,惊]`（30年一移；卷一「推八門占嵗計法」命起开门以次休生门，开元十二年甲子开门/三十一年甲午休门锚 ✓）；时计 **王希明法**：`门数 = 积时 mod 120 ÷ 30`，阳遁池 `[开,生,惊,休]`、阴遁池 `[杜,死,伤,景]`（30时一移、120时一周）。
  - 依据：卷一「推八門用法」张良四门时序 = 王希明四门序（「阳遁四门开生惊休，阴遁四门杜死伤景」），王希明「门则随数起，时尽则移，不拘其甲丙己辛日」明言废张良锚日法；「推夏至加時所直門法」阙文「为门数(阙)而行之」依阴遁四门序补「命起杜门以次而行」。「以次而行」指四门时序，非八门自然序（开休生伤…系误读）。
  - 口径：两遁连续绝对积时同轨、仅换门池；**不加**卷一「夏至加时实」+2192 平气偏移（与 app 太乙宫连续积时口径一致，用户裁定）。
```

- [ ] **Step 2: 更新 memory `taiyi-feature.md`**

- frontmatter description 中 `P3(卷四符神/乙神 四神/天乙/直符)已实现` 后补 `；P4-a(时计八门王希明法)已实现`。
- 「仍待做(P4…)」节中删去 `**时计八门张良法**` 整条，新增一段：

```markdown
**P4-a 时计八门王希明法已完成(2026-07-26)**：直使门 = 积时 mod 120 ÷ 30，阳遁池[开,生,惊,休]/阴遁池[杜,死,伤,景]（`TaiyiMen.poolYang/poolYin`）；两遁连续积时同轨仅换池（用户裁定，不加卷一+2192平气偏移）。卷一三说调和：张良四门时序=王希明四门序，王希明废锚日法；夏至法阙文补「命起杜门以次而行」。测试 27→30 全绿。
```

- [ ] **Step 3: 修正 memory `build-environment.md` 过时行**

删去「项目不是 git 仓库（截至 2026-07-25）。」一行（现已是 git 仓库，main 分支有提交；spec/plan 均入库）。

- [ ] **Step 4: Commit**

```bash
git add .taiyi-reference.md
git commit -m "docs(太乙): §12 时计直使改王希明法口径(卷一三说调和)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

（memory 目录在仓库外，不入库。）

---

## Self-Review 记录

- **Spec coverage**：spec §2 算法→Task 1 Step 3；§3 代码改动→Task 1 Step 3（门池常量、`computeZhiShiMen` 签名、调用点全覆盖）；§4 测试锚点→Task 1 Step 1（冬至开门/四门轮转/阴遁杜门/年计回归四项全有）；§5 文档同步→Task 2；§6 不做项→计划中无对应任务 ✓。
- **Placeholder scan**：无 TBD/TODO；所有代码与测试均为完整可粘贴内容。
- **Type consistency**：`poolYang`/`poolYin`（Task 1 定义）与 Task 2 引用一致；`computeZhiShiMen(suanBase, dun, type)` 签名在定义、调用点、Interfaces 三处一致；测试用枚举成员名（`KAI/SHENG/JING2/XIU/DU`）与 `Taiyi.kt` 现有成员逐一核对 ✓。
- **锚点数值复核**：2023冬至积时 137185080 = 1143209×120（mod 120=0）；2023-12-22 12:30 → +6 → 门数0 开；2023-12-25 00:30 → 3×12+0=36 → 门数1 生；2023-12-27 12:30 → 5×12+6=66 → 门数2 惊；2023-12-30 00:30 → 8×12+0=96 → 门数3 休；2024-01-01 12:30 → 10×12+6=126，mod 120=6 → 门数0 开；2024-10-09 00:30 → 292×12+0=3504，mod 120=24 → 门数0，阴遁 → 杜 ✓。

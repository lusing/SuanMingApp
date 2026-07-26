# 太乙阳九百六 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 卷七阳九/百六两灾厄周期入盘，基星区追加两行（复用 `TaiyiJiXing`，UI 零改动）。

**Architecture:** 改动收口 `TaiyiCalculator.computeJiXing`（签名加 `taiSui` 用于冲太岁 flag）；模型/UI 不动。依据 spec：`docs/superpowers/specs/2026-07-26-taiyi-yangjiu-bailiu-design.md`。

**Tech Stack:** Kotlin / Android (Gradle)，JUnit4。

## Global Constraints

- 测试命令：`./gradlew :app:testDebugUnitTest`（单类加 `--tests "cn.xulun.bazi.TaiyiCalculatorTest"`），工作目录 `G:\code\zhouyi\bazia`。
- 积年：`u = year + 12606`（= 演纪上元甲寅积年−1；卷七 13331@724 锚，与五福同源）。
- 阳九：元=`u/4560+1`；元内=`(u/456)%10+1`；入年=`u%456+1`；邦=寅+`(u%156)/13`（存疑标注）。
- 百六：元=`u/4320−1`；元内=`(u/288)%15+1`；入年=`u%288+1`；邦=寅+`(u%288)/24`。
- `EarthlyBranch`：ordinal/fromIndex 均 子=0、寅=2；六冲判定 `floorMod(bangIdx − taiSui.ordinal, 12) == 6`。
- 警示：入年第 1 年→注加「·值会」；邦冲太岁→注加「·冲太岁」。

---

### Task 1: 阳九百六（测试 + 实现）

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt:69`（调用点）、`:187`（`computeJiXing` 签名）、文件内 `computeJiXing` 尾部（`out += fsGod("直符", ...)` 之后、`return out` 之前）
- Test: `app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt`

**Interfaces:**
- Consumes: `EarthlyBranch.fromIndex(Int)`（既有）；`TaiyiJiXing(name, location, palaceNo, period, inYear, auspicious, note)`；`build` 内 `taiSui: EarthlyBranch`。
- Produces: `p.jiXing` 追加 `阳九`/`百六` 两条目（`location="X邦"`、`palaceNo=null`、`inYear=入第X年`）；`computeJiXing(year: Int, taiyiPalaceNo: Int, inGongOffset: Int, taiSui: EarthlyBranch)` 新签名（private）。

- [ ] **Step 1: Write the failing tests**

`TaiyiCalculatorTest.kt` 文件末尾（类结束 `}` 前）追加，并把既有 `` `卷五十神齐备 飞符不列` `` 测试中 `listOf(...)` 一行改为含阳九/百六的 12 神、测试名改 `` `卷五卷七十神齐备 飞符不列` ``：

```kotlin
    private fun jiXingMap(year: Int) =
        TaiyiCalculator.build(LocalDateTime.of(year, 7, 1, 12, 0), TaiyiCountType.YEAR).jiXing.associateBy { it.name }

    // ── 卷七 阳九百六（灾厄周期；演纪积年 u=year+12606）──
    @Test
    fun `618 武德元年 第三元第十阳九第1年 值会`() {
        val y = jiXingMap(618)["阳九"]!!
        assertTrue(y.note, y.note.contains("第三元第十阳九"))
        assertEquals(1, y.inYear)
        assertTrue(y.note, y.note.contains("值会"))
    }

    @Test
    fun `162 延熹五年 第三元第九阳九第1年`() {
        val y = jiXingMap(162)["阳九"]!!
        assertTrue(y.note, y.note.contains("第三元第九阳九"))
        assertEquals(1, y.inYear)
    }

    @Test
    fun `1074 第四元第一阳九第1年 开元后350年方会`() {
        val y = jiXingMap(1074)["阳九"]!!
        assertTrue(y.note, y.note.contains("第四元第一阳九"))
        assertEquals(1, y.inYear)
    }

    @Test
    fun `354 永和十年 第二元一百六第1年寅邦 642 第二百六`() {
        jiXingMap(354)["百六"]!!.let {
            assertTrue(it.note, it.note.contains("第二元第一百六"))
            assertEquals(1, it.inYear)
            assertEquals("寅邦", it.location)
        }
        jiXingMap(642)["百六"]!!.let {
            assertTrue(it.note, it.note.contains("第二元第二百六"))
            assertEquals(1, it.inYear)
            assertEquals("寅邦", it.location)
        }
    }

    @Test
    fun `2024 第四元第三阳九第39年亥邦 第二元第六百六第231年亥邦`() {
        val m = jiXingMap(2024)
        m["阳九"]!!.let {
            assertTrue(it.note, it.note.contains("第四元第三阳九"))
            assertEquals(39, it.inYear)
            assertEquals("亥邦", it.location)
        }
        m["百六"]!!.let {
            assertTrue(it.note, it.note.contains("第二元第六百六"))
            assertEquals(231, it.inYear)
            assertEquals("亥邦", it.location)
        }
    }
}
```

`卷五十神齐备` 测试的 listOf 行改为：

```kotlin
        listOf("君基", "臣基", "民基", "五福", "大游", "小游", "四神", "天乙", "地乙", "直符", "阳九", "百六").forEach {
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest"`
Expected: FAIL——`m["阳九"]`/`m["百六"]` 为 null 抛 NPE；12 神 listOf 断言失败。

- [ ] **Step 3: Implement**

① `computeJiXing` 签名（187 行附近）改为：

```kotlin
    private fun computeJiXing(year: Int, taiyiPalaceNo: Int, inGongOffset: Int, taiSui: EarthlyBranch): List<TaiyiJiXing> {
```

② 调用点（69 行）改为：

```kotlin
        val jiXing = computeJiXing(dt.year, ju.taiyiPalaceNo, ju.inGongOffset, taiSui)
```

③ `computeJiXing` 尾部（`out += fsGod("直符", 4, false, "火神值日巡察")` 之后、`return out` 之前）追加：

```kotlin
        // ── 卷七 阳九百六（灾厄周期；演纪上元甲寅积年，与五福同锚 13331@724）──
        // 阳九：4560元/456一会，元=u/4560+1、元内=(u/456)%10+1、入年=u%456+1；邦=寅+(u%156)/13
        //   （13年/邦；156 与跨度 456 不整除，原文如此，存疑——用户裁定按原文+标注）。
        // 百六：4320元(15周×288)，元=u/4320−1（卷七元1起 J8641）、元内=(u/288)%15+1、入年=u%288+1；
        //   邦=寅+(u%288)/24（24年/邦）。锚：618 第十阳九/162 第九/354 元2一百六/642 元2二百六 ✓。
        val u = year + 12606L                                  // 积年−1
        fun chongTaiSui(bangIdx: Int): Boolean = floorMod(bangIdx - taiSui.ordinal, 12) == 6
        val y9yuan = u / 4560 + 1
        val y9ci = (u / 456) % 10 + 1
        val y9in = (u % 456).toInt() + 1
        val y9bangIdx = (2 + ((u % 156) / 13).toInt()) % 12
        out += TaiyiJiXing("阳九", "${EarthlyBranch.fromIndex(y9bangIdx).chinese}邦", null,
            "4560元/456年一会", y9in, false,
            "第${y9yuan}元第${y9ci}阳九·亡国弑君之厄·起寅邦13年一移(卷七;邦周期156与跨度456不整除,原文存疑)" +
                (if (y9in == 1) "·值会" else "") + (if (chongTaiSui(y9bangIdx)) "·冲太岁" else ""))
        val b6yuan = u / 4320 - 1
        val b6ci = (u / 288) % 15 + 1
        val b6in = (u % 288).toInt() + 1
        val b6bangIdx = (2 + ((u % 288) / 24).toInt()) % 12
        out += TaiyiJiXing("百六", "${EarthlyBranch.fromIndex(b6bangIdx).chinese}邦", null,
            "4320元/288年一会", b6in, false,
            "第${b6yuan}元第${b6ci}百六·篡弑之祸·起寅邦24年一移(卷七)" +
                (if (b6in == 1) "·值会" else "") + (if (chongTaiSui(b6bangIdx)) "·冲太岁" else ""))
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest"`
Expected: PASS（35 个）。

- [ ] **Step 5: Full regression**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全量 PASS。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt
git commit -m "feat(太乙): 阳九百六灾厄周期(卷七;4560/4320元+支邦+值会冲太岁flag)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: 文档同步（参考文档 + memory）

**Files:**
- Modify: `.taiyi-reference.md`（§17 末「三堂坐标」行之后追加）
- Modify: `G:\xulun\.claude\projects\G--code-zhouyi-bazia\memory\taiyi-feature.md`

**Interfaces:**
- Consumes: Task 1 已合入实现。
- Produces: 文档与代码口径一致；无代码接口。

- [ ] **Step 1: `.taiyi-reference.md` §17 末追加**

```
- **阳九/百六**（卷七 推太岁有阳九之灾法/推太乙有百六之厄法，2026-07-26 原文核定）：积年同五福锚 `u=year+12606`（卷七 13331@724）。阳九：4560元/456一会，元=`u/4560+1`、元内=`(u/456)%10+1`、入年=`u%456+1`、邦=寅+`(u%156)/13`（13年/邦；156与456不整除,原文如此,存疑）。百六：4320元(15周×288)，元=`u/4320−1`（卷七元1起 J8641，与阳九元编号相位不同）、元内=`(u/288)%15+1`、入年=`u%288+1`、邦=寅+`(u%288)/24`。锚：618武德→元3第十阳九第1年 ✓、162延熹→第九 ✓、-750周平王→第七 ✓、1074→元4第一（「开元后350年方会」）✓；354永和→元2一百六·寅邦 ✓、642贞观→元2二百六·寅邦 ✓。警示：入第1年=值会；邦冲太岁=冲太岁（「与太岁冲并于分野」简化）。
```

- [ ] **Step 2: 更新 memory `taiyi-feature.md`**

- frontmatter description 末补 `+P4-d(卷七阳九百六)`。
- P4-c 段后追加：

```markdown
**P4-d 卷七阳九百六已完成(2026-07-26)**：u=year+12606(与五福同锚13331@724)。阳九 4560元/456一会(元=u/4560+1,元内=(u/456)%10+1,邦=寅+(u%156)/13 存疑标注:156与456不整除)；百六 4320元/288一会(元=u/4320−1 卷七元1起J8641,元内=(u/288)%15+1,邦=寅+(u%288)/24)。computeJiXing 加 taiSui 入参(冲太岁flag)。锚:618/162/1074 阳九、354/642 百六(原文例)、2024(阳九39年亥邦/百六231年亥邦)。测试 35 全绿。spec/plan: docs/superpowers/{specs,plans}/2026-07-26-taiyi-yangjiu-bailiu*。
```

- 「仍待做(P4 余项…)」节删去 `**阳九百六**` 条，节名改「仍待做：月计/日计入局」。

- [ ] **Step 3: Commit**

```bash
git add .taiyi-reference.md
git commit -m "docs(太乙): §17 补阳九百六(卷七公式+锚+存疑)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

（memory 目录在仓库外，不入库。）

---

## Self-Review 记录

- **Spec coverage**：spec §1 公式→Task 1 Step 3（元/元内/入年/邦/双 flag 全覆盖）；§2 代码改动→Task 1（签名+调用点+尾部追加）；§3 测试锚点→Task 1 Step 1（618/162/1074/354/642/2024 全在）；§4 文档→Task 2；§5 不做项→无对应任务 ✓。
- **Placeholder scan**：无 TBD；代码完整可粘贴。
- **Type consistency**：`computeJiXing(year, taiyiPalaceNo, inGongOffset, taiSui)` 签名在定义/调用点/Interfaces 三处一致；`TaiyiJiXing` 七参顺序与既有调用一致；`taiSui.ordinal` 与 `EarthlyBranch.fromIndex` 均为既有成员。
- **锚点数值复核**（u=year+12606）：
  - 618：u=13224；/4560=2→元3；/456=29%10=9→第十；%456=0→第1年 ✓；邦 (13224%156=120)/13=9→亥；太岁寅不冲亥 → 无冲太岁，有值会 ✓
  - 162：u=12768；/4560=2→元3；/456=28%10=8→第九；%456=0→1 ✓
  - 1074：u=13680；/4560=3→元4；/456=30%10=0→第一；%456=0→1 ✓
  - 354：u=12960；/4320=3→元2(−1)；/288=45%15=0→第一；%288=0→1；邦 0→寅 ✓
  - 642：u=13248；/4320=3→元2；/288=46%15=1→第二；%288=0→1；寅 ✓
  - 2024：u=14630；/4560=3→元4；/456=32%10=2→第三；%456=38→39；邦 (14630%156=122)/13=9→亥 ✓；/4320=3→元2；/288=50%15=5→第六；%288=230→231；邦 230/24=9→亥 ✓

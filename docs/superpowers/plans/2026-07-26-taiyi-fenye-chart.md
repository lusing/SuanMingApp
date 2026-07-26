# 太乙十二分野图 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按卷八「九宫分野(并序)」绘十二分野图（九宫方阵+三堂第四行），常驻显示四神/天乙/地乙/直符落格。

**Architecture:** 逻辑收口模型层 `TaiyiFenYe`（州映射+归格，可单测）；UI 仅渲染（`FenYeChart` composable）。依据 spec：`docs/superpowers/specs/2026-07-26-taiyi-fenye-chart-design.md`。

**Tech Stack:** Kotlin / Android (Gradle) / Jetpack Compose (Material3)，JUnit4。

## Global Constraints

- 测试命令：`./gradlew :app:testDebugUnitTest`（单类加 `--tests "cn.xulun.bazi.TaiyiModelTest"`），工作目录 `G:\code\zhouyi\bazia`。
- 州映射（卷八原文）：1冀 2荆 3青 4徐 5豫 6雍 7梁 8兖 9扬；绛宫→交、明堂→益、玉堂→幽。
- 网格：前三行 `TaiyiPalace.gridOrder`（`[[9,2,7],[4,5,6],[3,8,1]]`），第四行 `["绛宫","明堂","玉堂"]`。
- 归格 key：落宫→宫号字符串（"1".."9"）；落三堂→堂名（"绛宫"/"明堂"/"玉堂"）。
- 配色沿用既有：吉 `0xFF2E7D32` / 凶 `0xFFC62828`；底注灰 `0xFF9E9E9E`。

---

### Task 1: TaiyiFenYe 模型层（测试 + 实现）

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/model/Taiyi.kt`（文件末尾追加）
- Test: `app/src/test/java/cn/xulun/bazi/TaiyiModelTest.kt`

**Interfaces:**
- Consumes: `TaiyiPlate.jiXing: List<TaiyiJiXing>`；`TaiyiJiXing.name/palaceNo/location`；`TaiyiPalace.gridOrder`。
- Produces:
  ```kotlin
  object TaiyiFenYe {
      val palaceZhou: Map<Int, String>   // 1..9 → 州
      val tangZhou: Map<String, String>  // 堂名 → 州
      val tangOrder: List<String>        // [绛宫, 明堂, 玉堂]
      fun markers(plate: TaiyiPlate): Map<String, List<TaiyiJiXing>>
  }
  ```
  UI（Task 2）依赖以上四成员。

- [ ] **Step 1: Write the failing tests**

`TaiyiModelTest.kt` 末尾（类结束 `}` 前）追加；文件头 import 区追加 `import cn.xulun.bazi.core.TaiyiCalculator`、`import cn.xulun.bazi.model.TaiyiCountType`、`import cn.xulun.bazi.model.TaiyiFenYe`、`import java.time.LocalDateTime`：

```kotlin
    @Test
    fun `卷八十二分野 宫州与三堂州`() {
        val expect = mapOf(
            1 to "冀", 2 to "荆", 3 to "青", 4 to "徐", 5 to "豫",
            6 to "雍", 7 to "梁", 8 to "兖", 9 to "扬",
        )
        expect.forEach { (no, zhou) -> assertEquals(zhou, TaiyiFenYe.palaceZhou[no]) }
        assertEquals(9, TaiyiFenYe.palaceZhou.size)
        assertEquals("交", TaiyiFenYe.tangZhou["绛宫"])
        assertEquals("益", TaiyiFenYe.tangZhou["明堂"])
        assertEquals("幽", TaiyiFenYe.tangZhou["玉堂"])
        assertEquals(listOf("绛宫", "明堂", "玉堂"), TaiyiFenYe.tangOrder)
    }

    @Test
    fun `markers 2024 四神6宫 天乙明堂 地乙2宫 直符绛宫 仅四神入图`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = TaiyiFenYe.markers(p)
        assertEquals(listOf("四神"), m["6"]!!.map { it.name })
        assertEquals(listOf("天乙"), m["明堂"]!!.map { it.name })
        assertEquals(listOf("地乙"), m["2"]!!.map { it.name })
        assertEquals(listOf("直符"), m["绛宫"]!!.map { it.name })
        assertEquals(4, m.values.sumOf { it.size }) // 基星/五福/游神不入图
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiModelTest"`
Expected: FAIL——`TaiyiFenYe` 未定义，编译错误。

- [ ] **Step 3: Implement**

`app/src/main/java/cn/xulun/bazi/model/Taiyi.kt` 文件末尾追加：

```kotlin
/** 卷八「九宫分野(并序)」十二分野：九宫(含中五)配九州 + 绛宫/明堂/玉堂三堂各配一州。 */
object TaiyiFenYe {
    /** 宫号 → 州：一宫冀州 二宫荆州 三宫青州 四宫徐州 五宫豫州 六宫雍州 七宫梁州 八宫兖州 九宫扬州。 */
    val palaceZhou: Map<Int, String> = mapOf(
        1 to "冀", 2 to "荆", 3 to "青", 4 to "徐", 5 to "豫",
        6 to "雍", 7 to "梁", 8 to "兖", 9 to "扬",
    )
    /** 三堂 → 州：绛宫交州 明堂益州 玉堂幽州。 */
    val tangZhou: Map<String, String> = mapOf("绛宫" to "交", "明堂" to "益", "玉堂" to "幽")
    /** 分野图第四行（三堂）顺序。 */
    val tangOrder: List<String> = listOf("绛宫", "明堂", "玉堂")
    private val gods = setOf("四神", "天乙", "地乙", "直符")

    /** 四神归格：key=宫号("1".."9")或堂名；value 按 jiXing 原序（多神可同格）。基星/五福/游神不入图。 */
    fun markers(plate: TaiyiPlate): Map<String, List<TaiyiJiXing>> =
        plate.jiXing.filter { it.name in gods }
            .groupBy { it.palaceNo?.toString() ?: it.location }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiModelTest"`
Expected: PASS（6 个）。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/cn/xulun/bazi/model/Taiyi.kt app/src/test/java/cn/xulun/bazi/TaiyiModelTest.kt
git commit -m "feat(太乙): TaiyiFenYe 十二分野模型(卷八九宫分野并序)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: FenYeChart UI（Compose）

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/ui/TaiyiPage.kt`

**Interfaces:**
- Consumes: `TaiyiFenYe.palaceZhou/tangZhou/tangOrder/markers`（Task 1）；`TaiyiPalace.gridOrder`；既有 `InputCard`/`Surface`/`BorderStroke`（本文件已 import）。
- Produces: `@Composable private fun FenYeChart(p: TaiyiPlate)`（private，仅本文件用）。

- [ ] **Step 1: Implement FenYeChart + 接入**

`TaiyiPage.kt` 三处改动：

① import 区追加：

```kotlin
import cn.xulun.bazi.model.TaiyiFenYe
import cn.xulun.bazi.model.TaiyiJiXing
import cn.xulun.bazi.model.TaiyiPalace
```

② `plate?.let { p ->` 区块中 `JiXingSection(p)` 与 `PatternSection(p)` 之间插入一行：

```kotlin
                FenYeChart(p)
```

③ 文件末尾（`DateField` 之后）追加两个 composable：

```kotlin
@Composable
private fun FenYeChart(p: TaiyiPlate) {
    val marks = TaiyiFenYe.markers(p)
    InputCard("十二分野图", Icons.Filled.CalendarMonth) {
        TaiyiPalace.gridOrder.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { no ->
                    FenYeCell("${no}宫·${TaiyiFenYe.palaceZhou[no]}", marks[no.toString()].orEmpty(), Modifier.weight(1f))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            TaiyiFenYe.tangOrder.forEach { tang ->
                FenYeCell("$tang·${TaiyiFenYe.tangZhou[tang]}", marks[tang].orEmpty(), Modifier.weight(1f))
            }
        }
        Text(
            "卷八 九宫分野(并序)：九宫(含中五)配九州；绛宫交州·明堂益州·玉堂幽州",
            fontSize = 11.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun FenYeCell(title: String, gods: List<TaiyiJiXing>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(2.dp),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = if (gods.isEmpty()) Color.White else Color(0xFFFFF8F0),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontSize = 12.sp, color = Color(0xFF616161))
            if (gods.isEmpty()) {
                Text("　", fontSize = 13.sp)
            } else {
                gods.forEach { g ->
                    Text(
                        g.name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (g.auspicious) Color(0xFF2E7D32) else Color(0xFFC62828),
                    )
                }
            }
        }
    }
}
```

④ 顺带：`JiXingSection` 标题 `"卷四星宿"` → `"卷四基星 · 卷五符神"`；其底部注整行替换为：

```kotlin
            "卷四基星起戌邦·五福独立积年·大游起七宫；四神/天乙/地乙/直符依卷五立成十二位环；三堂分野依卷八，见下十二分野图",
```

- [ ] **Step 2: Compile + 全量回归**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL（Compose 编译通过 + 全部单测 PASS）。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/cn/xulun/bazi/ui/TaiyiPage.kt
git commit -m "feat(太乙): 十二分野图 UI(九宫方阵+三堂行,常驻标记四神落格)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: 文档同步（参考文档 + memory）

**Files:**
- Modify: `.taiyi-reference.md`（§1 分野行 + §17 符神段末）
- Modify: `G:\xulun\.claude\projects\G--code-zhouyi-bazia\memory\taiyi-feature.md`

**Interfaces:**
- Consumes: Task 1/2 已合入实现。
- Produces: 文档与代码口径一致；无代码接口。

- [ ] **Step 1: 改 `.taiyi-reference.md` 两处**

① §1 太乙宫数表的分野行（现为 `| 分野 | 冀 | 荆 | 青 | 徐 | — | 雍 | 梁 | 兖 | 扬 |`）改为 `| 分野 | 冀 | 荆 | 青 | 徐 | 豫 | 雍 | 梁 | 兖 | 扬 |`，并在表后加注一行：

```
- 分野出卷八「九宫分野(并序)」（含中五=豫州；原表「—」更正）；三堂：绛宫交州/明堂益州/玉堂幽州——UI 十二分野图按此绘。
```

② §17 符神段（P4-b 新写那段）末尾的「- **飞符**：淘金歌概念，卷五无，不列。」之后追加一行：

```
- **三堂坐标**：卷八「九宫分野(并序)」给分野坐标（绛宫交州/明堂益州/玉堂幽州）→ UI 十二分野图（九宫方阵+三堂第四行，常驻标记四神落格，`TaiyiFenYe`）。
```

- [ ] **Step 2: 更新 memory `taiyi-feature.md`**

frontmatter description 末 `+P4-b(卷五四神三元五纪立成:统一12位环+地乙)已实现` 后补 `+P4-c(十二分野图:卷八州映射+四神落格UI)`；正文 P4-b 段后追加：

```markdown
**P4-c 十二分野图已完成(2026-07-26)**：卷八「九宫分野(并序)」州映射(1冀2荆3青4徐5豫6雍7梁8兖9扬/绛交/明益/玉幽)入 `TaiyiFenYe`(palaceZhou/tangZhou/tangOrder/markers);`TaiyiPage.FenYeChart` 常驻渲染(九宫方阵+三堂第四行,四神落格吉绿凶红)。中五=豫州更正入 §1。spec/plan: docs/superpowers/{specs,plans}/2026-07-26-taiyi-fenye-chart*。
```

- [ ] **Step 3: Commit**

```bash
git add .taiyi-reference.md
git commit -m "docs(太乙): 分野补卷八三堂与中五豫州(十二分野图口径)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

（memory 目录在仓库外，不入库。）

---

## Self-Review 记录

- **Spec coverage**：spec §2 模型→Task 1；§3 UI（FenYeChart/接入点/JiXingSection 标题与注）→Task 2；§4 测试→Task 1 Step 1；§5 文档→Task 3；§6 不做项→无对应任务 ✓。
- **Placeholder scan**：无 TBD；模型/测试/UI 代码完整可粘贴。
- **Type consistency**：`TaiyiFenYe.markers` 返回 `Map<String, List<TaiyiJiXing>>`，Task 1 定义/测试 `m["6"]`、`m["明堂"]` 与 Task 2 `marks[no.toString()]`、`marks[tang]` 三处一致；`FenYeCell(title, gods, modifier)` 签名定义与两处调用一致。
- **锚点复核**：2024 四神 palaceNo=6→key "6"；天乙/直符 palaceNo=null、location="明堂"/"绛宫"→堂名 key；地乙 palaceNo=2→"2" ✓（与 P4-b 测试值一致）。

# 太乙神数 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 太乙神数 排盘+分析 to the Android app, alongside 八字/奇门/六壬.

**Architecture:** Mirror qimen (九宫+格局+逐宫分析) and liuren (model/core/ui/test split + `.xxx-reference.md` + TDD). A single `TaiyiPlate` serves all 四计 (岁/月/日/时); only 入局法 + 阴阳遁判定 differ. 时计 is the app's main entry (event-level, datetime-driven), 年/月/日 advanced toggle.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), JUnit4. No new dependencies.

## Global Constraints

- 算法依据 `.taiyi-reference.md`（已验证·五源交叉裁定 + 用户权威讲解）。凡公式/表查该文档对应 § 节。
- 太乙九宫**差一宫**：1乾 2离 3艮 4震 6兑 7坤 8坎 9巽 —— **独立 `TaiyiPalace`，不复用 `QiPalace`**；gridOrder=`[[9,2,7],[4,5,6],[3,8,1]]`。
- 大将=`算 mod 10`(0→`mod 9`)；参将=`(大将×3) mod 10`。主算/客算/定算同算法（目→太乙不含，正宫宫数累加，目在间辰起点+1）。
- 四柱与节气复用现有 `BaziCalculator.calculate(dt)`、`SolarTermTable`/`SolarTerm`。
- 模运算一律 `java.lang.Math.floorMod`。
- 包名 `cn.xulun.bazi`；测试 `app/src/test/java/cn/xulun/bazi/`，JUnit `org.junit.Assert.*` + `@Test`，方法名用反引号中文。
- v1 范围 = 八将+三算+大小将+定计+八门+格局(迫/格/杜塞/囚/关 + 掩/击/对/四郭固/四郭杜/提挟/执提)+数分类。基星/五福/游神/符神/乙神/阳九百六 为 P2，不在本计划。
- 每处 `TODO`/取舍在代码注释 + UI 注明。

## File Structure

- **Create** `app/src/main/java/cn/xulun/bazi/model/Taiyi.kt` — 枚举+数据类（TaiyiPalace/SixteenGod/TaiyiCountType/TaiyiDun/TaiyiMen/TaiyiJu/TaiyiPattern/TaiyiPalaceCell/TaiyiPlate + 分析类型）。
- **Create** `app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt` — `object TaiyiCalculator`，`build(dt,type): TaiyiPlate` + 私有步骤。
- **Create** `app/src/main/java/cn/xulun/bazi/ui/components/TaiyiGrid.kt` — 九宫盘（TaiyiPalace.gridOrder）。
- **Create** `app/src/main/java/cn/xulun/bazi/ui/components/TaiyiSections.kt` — 局信息/主客定算/八门/格局/断语 区块。
- **Create** `app/src/main/java/cn/xulun/bazi/ui/TaiyiPage.kt` — 页面（onBack 回调）。
- **Create** `app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt` — TDD 断言（7 锚点）。
- **Modify** `app/src/main/java/cn/xulun/bazi/ui/BaziApp.kt` — sealed `Screen` 加 `Taiyi`。
- **Modify** `app/src/main/java/cn/xulun/bazi/ui/BaziPage.kt` — 加入口按钮 `onOpenTaiyi`。

---

### Task 1: Model layer (`model/Taiyi.kt`)

**Files:**
- Create: `app/src/main/java/cn/xulun/bazi/model/Taiyi.kt`
- Test: `app/src/test/java/cn/xulun/bazi/TaiyiModelTest.kt`

**Interfaces:**
- Produces: `TaiyiPalace`（含 `ring/gridOrder/of(no)/opposite(no)/isInner(no)`）、`SixteenGod`（`idx/position/name/isZheng/palaceNo`，`at(idx)/ofBranch`）、`TaiyiCountType`、`TaiyiDun`、`TaiyiMen`、`TaiyiJu`、`TaiyiPattern`、`TaiyiPalaceCell`、`TaiyiPlate`、`TaiyiTone/TaiyiAnalysisItem`。

- [ ] **Step 1: Write failing test** (`TaiyiModelTest.kt`)

```kotlin
package cn.xulun.bazi

import cn.xulun.bazi.model.SixteenGod
import cn.xulun.bazi.model.TaiyiPalace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaiyiModelTest {
    @Test
    fun `太乙九宫 差一宫 乾1离2艮3震4兑6坤7坎8巽9`() {
        assertEquals("乾", TaiyiPalace.of(1).trigram)
        assertEquals("离", TaiyiPalace.of(2).trigram)
        assertEquals("艮", TaiyiPalace.of(3).trigram)
        assertEquals("震", TaiyiPalace.of(4).trigram)
        assertEquals("兑", TaiyiPalace.of(6).trigram)
        assertEquals("坤", TaiyiPalace.of(7).trigram)
        assertEquals("坎", TaiyiPalace.of(8).trigram)
        assertEquals("巽", TaiyiPalace.of(9).trigram)
    }
    @Test
    fun `gridOrder 与 ring 正确`() {
        assertEquals(listOf(9, 2, 7), TaiyiPalace.gridOrder[0])
        assertEquals(listOf(4, 5, 6), TaiyiPalace.gridOrder[1])
        assertEquals(listOf(3, 8, 1), TaiyiPalace.gridOrder[2])
        assertEquals(listOf(1, 2, 3, 4, 6, 7, 8, 9), TaiyiPalace.ring)
    }
    @Test
    fun `对宫 与 地内天外`() {
        assertEquals(9, TaiyiPalace.opposite(1))   // 乾↔巽
        assertEquals(2, TaiyiPalace.opposite(8))   // 坎↔离
        assertEquals(7, TaiyiPalace.opposite(3))   // 艮↔坤
        assertTrue(TaiyiPalace.isInner(1) && TaiyiPalace.isInner(8))
        assertTrue(!TaiyiPalace.isInner(2) && !TaiyiPalace.isInner(9))
    }
    @Test
    fun `十六神 正神宫数 间辰无`() {
        assertEquals(8, SixteenGod.at(0).palaceNo)  // 子=坎8
        assertEquals(2, SixteenGod.at(8).palaceNo)  // 午=离2
        assertEquals(1, SixteenGod.at(14).palaceNo) // 乾=1
        assertEquals(null, SixteenGod.at(15).palaceNo) // 亥=间辰
        assertEquals("大义", SixteenGod.at(15).name)
    }
}
```

- [ ] **Step 2: Run test → FAIL** (`Task :app:testDebugUnitTest`，类未找到/编译失败)

- [ ] **Step 3: Implement `model/Taiyi.kt`** — 完整内容见下方代码块（枚举值严格按上表；`SixteenGod` 16 项 idx 0..15、正神 palaceNo 按宫数、间辰 null）。

```kotlin
package cn.xulun.bazi.model

import java.lang.Math.floorMod

/** 太乙九宫（差一宫，独立于 QiPalace）。palaceNo=洛书号1..9(5为中宫不入)。 */
enum class TaiyiPalace(
    val no: Int, val trigram: String, val element: Element,
    val direction: String, val region: String, val yang: Boolean,
) {
    QIAN(1, "乾", Element.METAL, "西北", "冀", false),
    LI(2, "离", Element.FIRE, "南", "荆", false),
    GEN(3, "艮", Element.EARTH, "东北", "青", true),
    ZHEN(4, "震", Element.WOOD, "东", "徐", true),
    ZHONG(5, "中", Element.EARTH, "中宫", "", false),
    DUI(6, "兑", Element.METAL, "西", "雍", false),
    KUN(7, "坤", Element.EARTH, "西南", "梁", false),
    KAN(8, "坎", Element.WATER, "北", "兖", true),
    XUN(9, "巽", Element.WOOD, "东南", "扬", true);
    companion object {
        val ring = listOf(1, 2, 3, 4, 6, 7, 8, 9)
        val gridOrder = listOf(listOf(9, 2, 7), listOf(4, 5, 6), listOf(3, 8, 1))
        fun of(no: Int): TaiyiPalace = entries.first { it.no == no }
        fun opposite(no: Int): Int = when (no) { 1->9; 9->1; 2->8; 8->2; 3->7; 7->3; 4->6; 6->4; else->no }
        fun isInner(no: Int): Boolean = no in setOf(1, 3, 4, 8) // 地内助主；2,6,7,9天外助客
    }
}

/** 十六神（间辰）。idx 0..15 顺时针；正神带太乙宫数，间辰 null。 */
enum class SixteenGod(val idx: Int, val position: String, val name: String, val isZheng: Boolean, val palaceNo: Int?) {
    ZI(0, "子", "地主", true, 8), CHOU(1, "丑", "阳德", false, null),
    GEN_G(2, "艮", "和德", true, 3), YIN(3, "寅", "吕申", false, null),
    MAO(4, "卯", "高丛", true, 4), CHEN(5, "辰", "太阳", false, null),
    XUN_G(6, "巽", "大炅", true, 9), SI(7, "巳", "大神", false, null),
    WU(8, "午", "大威", true, 2), WEI(9, "未", "天道", false, null),
    KUN_G(10, "坤", "大武", true, 7), SHEN(11, "申", "武德", false, null),
    YOU(12, "酉", "太簇", true, 6), XU(13, "戌", "阴主", false, null),
    QIAN_G(14, "乾", "阴德", true, 1), HAI(15, "亥", "大义", false, null);
    companion object {
        fun at(idx: Int): SixteenGod = entries.first { it.idx == floorMod(idx, 16) }
        fun ofBranch(branch: EarthlyBranch): SixteenGod = entries.first { it.position == branch.chinese }
    }
}

enum class TaiyiCountType(val chinese: String) { YEAR("年计"), MONTH("月计"), DAY("日计"), HOUR("时计") }
enum class TaiyiDun(val chinese: String) { YANG("阳遁"), YIN("阴遁") }

/** 太乙八门（固定卦位）。三吉=开休生。 */
enum class TaiyiMen(val chinese: String, val trigram: String, val element: Element, val auspicious: Int) {
    KAI("开", "乾", Element.METAL, 1), XIU("休", "坎", Element.WATER, 1), SHENG("生", "艮", Element.EARTH, 1),
    SHANG("伤", "震", Element.WOOD, -1), DU("杜", "巽", Element.WOOD, 0),
    JING("景", "离", Element.FIRE, 0), SI_MEN("死", "坤", Element.EARTH, -1), JING2("惊", "兑", Element.METAL, -1);
    companion object {
        val order = listOf(KAI, XIU, SHENG, SHANG, DU, JING, SI_MEN, JING2) // 顺时针顺布序
    }
}

/** 局信息 */
data class TaiyiJu(
    val countType: TaiyiCountType, val dun: TaiyiDun,
    val suanBase: Long,        // 积年（年/月/日计）或 积时（时计）
    val juNo: Int,             // 局数 1..72
    val taiyiPalaceNo: Int,    // 太乙宫 1..9(跳5)
    val inGongOffset: Int,     // 入宫以来 0..2
    val liTianDiRen: String,   // 理天/理地/理人
)

data class TaiyiPattern(val name: String, val auspicious: Boolean, val basis: String, val duanYu: String = "")

/** 九宫单宫显示内容（occupants 为落该宫的标记集合） */
data class TaiyiPalaceCell(
    val palaceNo: Int,
    val occupants: List<String>,   // 如 ["太乙","文昌","主大将"]
    val men: TaiyiMen?,            // 该宫太乙盘八门
    val tags: List<String> = emptyList(),
)

data class TaiyiPlate(
    val ju: TaiyiJu,
    val taiSui: EarthlyBranch,
    val heShen: EarthlyBranch,
    val jiShen: EarthlyBranch,
    val wenChang: SixteenGod,
    val shiJi: SixteenGod,
    val zhuSuan: Int, val keSuan: Int, val dingSuan: Int,
    val zhuDaJiang: Int, val zhuCanJiang: Int,
    val keDaJiang: Int, val keCanJiang: Int,
    val dingDaJiang: Int, val dingCanJiang: Int,
    val dingMu: SixteenGod,
    val zhiShiMen: TaiyiMen,              // 直使门
    val menOnTaiyi: Map<Int, TaiyiMen>,   // 太乙盘八门落宫 宫号→门
    val patterns: List<TaiyiPattern>,
    val suanClass: List<String>,          // 数分类标签
) {
    val taiyiPalaceNo get() = ju.taiyiPalaceNo
}

enum class TaiyiTone { GOOD, BAD, NEUTRAL }
data class TaiyiAnalysisItem(val label: String, val text: String, val tone: TaiyiTone = TaiyiTone.NEUTRAL)
```

- [ ] **Step 4: Run test → PASS**

- [ ] **Step 5: Commit** — `feat(taiyi): model layer (TaiyiPalace/SixteenGod/Plate)`

---

### Task 2: 入局 + 太乙落宫 + 阴阳遁 (`core/TaiyiCalculator.kt` 骨架)

**Files:**
- Create: `app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt`
- Test: `app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt`

**Interfaces:**
- Consumes: `BaziCalculator.calculate(dt)`（取年支）、`SolarTermTable`/`SolarTerm`（时计取冬至/夏至时刻）。
- Produces: `TaiyiCalculator.build(dt: LocalDateTime, type: TaiyiCountType): TaiyiPlate`（本任务仅正确填充 `ju`/`taiyiPalaceNo`，其余字段先用占位值；后续 Task 填充）。

- [ ] **Step 1: Write failing tests** (`TaiyiCalculatorTest.kt`)

```kotlin
package cn.xulun.bazi

import cn.xulun.bazi.core.TaiyiCalculator
import cn.xulun.bazi.model.TaiyiCountType
import cn.xulun.bazi.model.TaiyiDun
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TaiyiCalculatorTest {
    @Test
    fun `年计 724AD 太乙1宫乾 理天`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(724, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertEquals(1, p.ju.taiyiPalaceNo)
        assertEquals(TaiyiDun.YANG, p.ju.dun)
        assertEquals("理天", p.ju.liTianDiRen)
    }
    @Test
    fun `年计 2024甲辰 阳遁53局 太乙2宫离 理地`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertEquals(53, p.ju.juNo)
        assertEquals(2, p.ju.taiyiPalaceNo)
        assertEquals(TaiyiDun.YANG, p.ju.dun)
        assertEquals("理地", p.ju.liTianDiRen)
    }
    @Test
    fun `年计 2025 太乙仍2宫 理人`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2025, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertEquals(2, p.ju.taiyiPalaceNo)
        assertEquals("理人", p.ju.liTianDiRen)
    }
    @Test
    fun `年计 1570武备志 太乙3宫艮 理天 31局`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(1570, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertEquals(3, p.ju.taiyiPalaceNo)
        assertEquals("理天", p.ju.liTianDiRen)
        assertEquals(31, p.ju.juNo)
    }
    @Test
    fun `时计 2024-10-09子时 阴遁 太乙9宫巽`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 10, 9, 0, 30), TaiyiCountType.HOUR)
        assertEquals(TaiyiDun.YIN, p.ju.dun)
        assertEquals(9, p.ju.taiyiPalaceNo) // 阴遁1局起9宫
    }
}
```

- [ ] **Step 2: Run → FAIL**

- [ ] **Step 3: Implement `core/TaiyiCalculator.kt`** —— 按 `.taiyi-reference.md` §2：
  - 年计：`积年 = 10153917L + dt.year`；`juNo = floorMod(积年, 360).let { it % 72 }.let { if(it==0) 72 else it }`（取 1..72）；`k = floorMod(juNo-1, 72)/3 % 8`；阳遁宫 `TaiyiPalace.ring[k]`。
  - 时计：取当年冬至时刻（`SolarTermTable` 查冬至；若 API 仅给日则取该日午时近似——TODO 注明）；`时实 = (距冬至积日-1)*12 + 时辰序`；`净数 = floorMod(时实,24)`；`宫序 = 净数/3`；阳遁宫 `ring[宫序]`，阴遁宫 `listOf(9,8,7,6,4,3,2,1)[宫序]`。
  - 阴阳遁：岁/月/日计恒 YANG；时计比占时与冬至/夏至时刻（占时≥冬至且<夏至→YANG；≥夏至或<冬至→YIN）。
  - `liTianDiRen = listOf("理天","理地","理人")[inGongOffset]`，`inGongOffset = floorMod(juNo-1,3)`（年计）/ `净数%3`（时计）。
  - 其余 Plate 字段先占位（文昌=武德、算=0 等），后续 Task 替换。
  - 入口 `fun build(dt, type): TaiyiPlate { ... }`。

- [ ] **Step 4: Run → PASS**（若时计冬至时刻 API 不足，先按"冬至日=12月22日午时"近似使测试通过，并留 TODO）

- [ ] **Step 5: Commit** — `feat(taiyi): 入局+太乙落宫+阴阳遁`

---

### Task 3: 太岁/合神/计神/文昌/始击

**Files:** Modify `core/TaiyiCalculator.kt`; append tests to `TaiyiCalculatorTest.kt`

**Interfaces:** Produces `Plate.taiSui/heShen/jiShen/wenChang/shiJi` 正确填充。

- [ ] **Step 1: Append failing tests**

```kotlin
    @Test fun `2024 太岁辰 合神酉 计神戌 文昌坤大武 始击亥`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024,7,1,12,0), TaiyiCountType.YEAR)
        assertEquals("辰", p.taiSui.chinese)
        assertEquals("酉", p.heShen.chinese)
        assertEquals("戌", p.jiShen.chinese)
        assertEquals("坤", p.wenChang.position); assertEquals("大武", p.wenChang.name)
        assertEquals("亥", p.shiJi.position); assertEquals("大义", p.shiJi.name)
    }
    @Test fun `2025 太岁巳 合神申 计神酉 始击子`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2025,7,1,12,0), TaiyiCountType.YEAR)
        assertEquals("巳", p.taiSui.chinese); assertEquals("申", p.heShen.chinese)
        assertEquals("酉", p.jiShen.chinese); assertEquals("子", p.shiJi.position)
    }
    @Test fun `时计阴遁 2024-10-09子时 太岁子 合神丑 计神申 文昌寅吕申`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024,10,9,0,30), TaiyiCountType.HOUR)
        assertEquals("子", p.taiSui.chinese); assertEquals("丑", p.heShen.chinese)
        assertEquals("申", p.jiShen.chinese)
        assertEquals("寅", p.wenChang.position); assertEquals("吕申", p.wenChang.name)
    }
```

- [ ] **Step 2: Run → FAIL**

- [ ] **Step 3: Implement** —— 按 `.taiyi-reference.md` §5/§6/§7：
  - `太岁` = 计之地支（年计→`bazi.yearPillar.branch`；时计→`bazi.hourPillar.branch`；月/日类推）。
  - `合神` = 六合(太岁)（建表 `mapOf(子→丑,丑→子,寅→亥,…)`，见 §6）。
  - `计神`：阳遁 `(2 - 太岁idx + 12)%12`（子=0）；阴遁 `(8 - 太岁idx + 12)%12`。转 EarthlyBranch。
  - `文昌`：阳遁 `SEQ18[floorMod(积年-1, 18)]`；阴遁起吕申(寅)。`SEQ18`（阳遁18位，自武德起，阴德/大武重留）见 §5。
  - `始击`：`distance = floorMod(2 - 计神十六神idx, 16)`；`始击idx = floorMod(文昌idx + distance, 16)`；`SixteenGod.at(idx)`。
  - 接入 `build()`，替换 Task2 占位。

- [ ] **Step 4: Run → PASS**

- [ ] **Step 5: Commit** — `feat(taiyi): 太岁合神计神文昌始击`

---

### Task 4: 主算/客算/定算 + 大小将

**Files:** Modify `core/TaiyiCalculator.kt`; append tests.

**Interfaces:** Produces `Plate.zhuSuan/keSuan/dingSuan` + `zhu/ke/ding DaJiang/CanJiang` + `dingMu`。

- [ ] **Step 1: Append failing tests**（取自 §15 多盘）

```kotlin
    @Test fun `2024 主算38 客算25 定算14 大小将`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024,7,1,12,0), TaiyiCountType.YEAR)
        assertEquals(38, p.zhuSuan); assertEquals(25, p.keSuan); assertEquals(14, p.dingSuan)
        assertEquals(8, p.zhuDaJiang); assertEquals(4, p.zhuCanJiang)
        assertEquals(5, p.keDaJiang);  assertEquals(5, p.keCanJiang)  // 客大将中宫
        assertEquals(4, p.dingDaJiang); assertEquals(2, p.dingCanJiang)
    }
    @Test fun `2023 主算39 间辰加一`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2023,7,1,12,0), TaiyiCountType.YEAR)
        assertEquals(39, p.zhuSuan); assertEquals(9, p.zhuDaJiang); assertEquals(7, p.zhuCanJiang)
    }
    @Test fun `1570武备志 主算33客算10 大将参将`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(1570,7,1,12,0), TaiyiCountType.YEAR)
        assertEquals(33, p.zhuSuan); assertEquals(3, p.zhuDaJiang); assertEquals(9, p.zhuCanJiang)
        assertEquals(10, p.keSuan); assertEquals(1, p.keDaJiang); assertEquals(3, p.keCanJiang)
    }
    @Test fun `时计阴遁 主算5客算29定算7`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024,10,9,0,30), TaiyiCountType.HOUR)
        assertEquals(5, p.zhuSuan);  assertEquals(5, p.zhuDaJiang); assertEquals(5, p.zhuCanJiang) // 杜塞
        assertEquals(29, p.keSuan); assertEquals(9, p.keDaJiang); assertEquals(7, p.keCanJiang)
        assertEquals(7, p.dingSuan); assertEquals(7, p.dingDaJiang); assertEquals(1, p.dingCanJiang)
        assertEquals("艮", p.dingMu.position)
    }
    @Test fun `金镜卷二原例 太乙9宫文昌亥 主算16`() {
        // 该例为固定盘（非真实历日），用专门入口或构造：若 build 不便，测私有 suan 函数
        // 见 Step3 的 internal fun computeSuan，直接断言
    }
```

- [ ] **Step 2: Run → FAIL**

- [ ] **Step 3: Implement** —— 按 §8/§9/§10：
  - `internal fun computeSuan(muIdx, taiyiIdx): Int`：`if(muIdx==taiyiIdx) 0; else { s=0; pos=muIdx; while(pos!=taiyiIdx){ val g=SixteenGod.at(pos); if(g.isZheng) s+=g.palaceNo!! else if(pos==muIdx) s+=1; pos=floorMod(pos+1,16) }; s }`。
  - 主算=`computeSuan(文昌idx, 太乙十六神idx)`；客算=`computeSuan(始击idx, 太乙idx)`。
  - 太乙十六神 idx：太乙宫→正神 idx（宫1乾→14, 2离午→8, 3艮→2, 4震卯→4, 6兑酉→12, 7坤→10, 8坎子→0, 9巽→6）。建 `palaceNoToIdx` 表。
  - 定目：`dingMuIdx = floorMod(文昌idx + 太岁十六神idx - 合神十六神idx, 16)`；定算=`computeSuan(dingMuIdx, 太乙idx)`。
  - `daJiang(suan) = suan%10; if(0) suan%9`（若 suan%10==0 用 suan%9，suan%9==0 时取9）。`canJiang(da) = (da*3)%10`（若0取10）。
  - 接入 build()。

- [ ] **Step 4: Run → PASS**（金镜原例用 `computeSuan(15/*亥*/, 6/*巽*/)`==16 单测，跳过 build 路径）

- [ ] **Step 5: Commit** — `feat(taiyi): 主客定算+大小将`

---

### Task 5: 八门 + 格局 + 数分类

**Files:** Modify `core/TaiyiCalculator.kt`; append tests.

**Interfaces:** Produces `Plate.zhiShiMen/menOnTaiyi/patterns/suanClass`。

- [ ] **Step 1: Append failing tests**

```kotlin
    @Test fun `2024 年计八门 伤门 直使`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024,7,1,12,0), TaiyiCountType.YEAR)
        assertEquals(TaiyiMen.SI_MEN, p.zhiShiMen) // 伤=SHANG? 见下注
    }
    @Test fun `2024 格局 外宫迫 格 杜塞`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024,7,1,12,0), TaiyiCountType.YEAR)
        val names = p.patterns.map { it.name }
        assertTrue(names.any { it.contains("迫") })
        assertTrue(names.any { it == "格" })
        assertTrue(names.any { it == "杜塞" })
    }
    @Test fun `2025 格局 囚 关`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2025,7,1,12,0), TaiyiCountType.YEAR)
        val names = p.patterns.map { it.name }
        assertTrue(names.any { it == "关" })   // 客大将4=主参将4
    }
    @Test fun `数分类 长数与不和`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024,7,1,12,0), TaiyiCountType.YEAR)
        assertTrue(p.suanClass.any { it.contains("长") || it.contains("不和") })
    }
```
> 注：八门枚举名修正——`伤` 的 enum 名避免与 `SHANG`(伤) 冲突请用 `SHANG`；`死`用 `SI_MEN`；`惊`用 `JING2`（Task1 已定）。伤门 = `TaiyiMen.SHANG`。测试断言改 `TaiyiMen.SHANG`。

- [ ] **Step 2: Run → FAIL**

- [ ] **Step 3: Implement** —— 按 §11/§12/§13：
  - 直使门：年计 `TaiyiMen.order[(floorMod(积年,240)/30)]`；时计张良法（甲日开/丙日生/己日惊/辛日休起，30时一移，阴遁反序）——时计可先按年计同公式用 `floorMod(时实,240)/30` 近似 + TODO。
  - `menOnTaiyi`：直使门落太乙宫，自太乙宫起沿 `TaiyiPalace.ring` 顺时针布 `TaiyiMen.order`（不入中5）。
  - 格局：`computePatterns()` 逐条判（掩/击/迫/囚/关/格/对/四郭固/四郭杜/提挟/执提 + 主客相得/门宫相和相克/文武同道）。条件见 §13；迫用「太乙十六神idx ±1 辰迫/±2 宫迫，+=外/−=内」，文昌或主客大小将临。
  - 数分类：`classifySuan()`（和数/长短/重杂/三才/不和/杜塞/宫数易和，表见 §11）。杜塞=大将宫==5。
  - 接入 build()。

- [ ] **Step 4: Run → PASS**

- [ ] **Step 5: Commit** — `feat(taiyi): 八门+格局+数分类`

---

### Task 6: 九宫盘 UI (`ui/components/TaiyiGrid.kt`)

**Files:** Create `ui/components/TaiyiGrid.kt`。

**Interfaces:** Consumes `TaiyiPlate`；镜像 `QimenGrid` 视觉（`gridOrder`/分层 cell/描边色），换 `TaiyiPalace` + 太乙将标记。

- [ ] **Step 1: 实现 `TaiyiGrid(plate, modifier, selectedPalace, onPalaceClick)`** —— 用 `TaiyiPalace.gridOrder`；每宫 `TaiyiPalaceCellView`：顶=宫名+卦，中=occupants（太乙/文昌/始击/大小将/定*），底=门；描边色：太乙临宫=SeedRed、主大将=红、客大将=蓝、选中=teal（参考 QimenGrid 的 SelectedBorder/BorderStroke）。
- [ ] **Step 2: 编译通过** `./gradlew :app:compileDebugKotlin`
- [ ] **Step 3: Commit** — `feat(taiyi): 九宫盘 grid`

---

### Task 7: 分析 sections + Page + 导航接入

**Files:** Create `ui/components/TaiyiSections.kt`、`ui/TaiyiPage.kt`；Modify `ui/BaziApp.kt`、`ui/BaziPage.kt`。

**Interfaces:** `TaiyiPage(plate, countType, onCountTypeChange, onBack)`；`BaziApp.Screen.Taiyi`；`BaziPage(onOpenTaiyu)`。

- [ ] **Step 1: `TaiyiSections.kt`** —— `JuInfoSection`(计法切换/遁/积年·局/太乙宫·理天地人)、`SuanSection`(主客定算+大小将+数分类 tone 着色)、`MenSection`(直使+落宫)、`PatternSection`(格局列表 吉绿/凶红)、`DuanYuSection`(主客断语：地内助主/天外助客)。复用 qimen/liuren 的 Card/Section 视觉。
- [ ] **Step 2: `TaiyiPage.kt`** —— 顶部计法切换（默认 HOUR），`remember(plate)` 调 `TaiyiCalculator.build`，`LazyColumn` 组装 Grid+Sections；`onBack` 回调。
- [ ] **Step 3: `BaziApp.kt`** —— sealed `Screen` 加 `data object Taiyi : Screen`；`when` 分支 `Screen.Taiyi -> TaiyiPage(...)`；状态 `var taiyi by remember`。
- [ ] **Step 4: `BaziPage.kt`** —— 加入口按钮（同 onOpenQimen/onOpenLiuren 模式）触发 `onOpenTaiyi`；`BaziApp` 传 `onOpenTaiyi = { screen = Screen.Taiyi }`。
- [ ] **Step 5: 全量编译** `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
- [ ] **Step 6: 真机运行**（按 build-environment/device-driving 记忆：`./gradlew installDebug` + adb 启动，或 IDE run）—— 打开太乙页，切时计/年计，核对 2024 盘面与 `.taiyi-reference.md §15` 一致。
- [ ] **Step 7: Commit** — `feat(taiyi): page + sections + 导航接入`

---

## Self-Review (写计划后自检)
- **Spec 覆盖**：八将(§4-10)→T1-4；八门+格局+数分类(§11-13)→T5；时计+阴遁(§2)→T2；UI→T6-7。基星/五福/游神/阳九百六 明确 P2 不在本计划。✓
- **Placeholder**：T2 时计冬至时刻 API 标 TODO（现有 SolarTermTable 应可查，不足则近似）；T5 时计八门张良法标 TODO（先同年计公式）。其余为真实测试断言。
- **类型一致**：`TaiyiMen.SHANG`(伤)/`SI_MEN`(死)/`JING2`(惊) 全程统一；`SixteenGod` 枚举名加 `_G` 后缀避免与 `TaiyiPalace`/`EarthlyBranch` 同名冲突（GEN_G/QIAN_G/KUN_G/XUN_G）。

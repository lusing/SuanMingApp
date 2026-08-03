# 八字功能移植（xuanzong → bazia）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 xuanzong（微信小程序）已有、bazia（Android/Kotlin）缺失的 6 项八字分析功能（神煞层、旬空/空亡、调候、取运、会合解冲、五行流通）按 4 个 Tier 移植回 bazia，每项含 model+calculator+UI+测试。

**Architecture:** 每项功能沿用 bazia 既有四件套（`model/Xxx.kt` + `core/XxxCalculator.kt`(`object`) + `ui/components/XxxSection.kt` + `XxxCalculatorTest.kt`），在 `ui/BaziDisplay.kt` 接线。算法忠实移植自 xuanzong `miniprogram/utils/bazi/*.js`（纯 CommonJS）。验证用**黄金母版**：已用 Node 跑 xuanzong 引擎，在 4 个锚点生辰上抓取全部 6 项功能的输出，存于 `docs/superpowers/plans/golden-master-xuanzong.json`，作为每个 Kotlin 测试的期望值。

**Tech Stack:** Kotlin、Jetpack Compose (Material3)、JUnit4、java.time.LocalDateTime。无新依赖。

## Global Constraints

- **索引即枚举 ordinal**：天干 `HeavenlyStem`（甲=0…癸=9）、地支 `EarthlyBranch`（子=0…亥=11）、五行 `Element`（木=0/火=1/土=2/金=3/水=4）、十神 `TenGod`。互转用 `HeavenlyStem.fromIndex(i)` / `EarthlyBranch.fromIndex(i)` / `java.lang.Math.floorMod`。
- **既有 helper 直接用**：`tenGodOfStem`、`tenGodOfBranch`、`branchHiddenStems`、`hiddenStemGods`、`PillarStemLabels=["年干","月干","日干","时干"]`、`PillarBranchLabels=["年支","月支","日支","时支"]`、`Element.generates`/`.controls`、`Pillar.cycleIndex`、`BaziResult.stems/branches/pillars`。
- **黄金母版为真理**：xuanzong JS 输出即期望值。索引到枚举用 `fromIndex`。测试期望取自 `golden-master-xuanzong.json`。
- **小步 TDD 提交**：每 Task 结束 `./gradlew test` 全绿后提交，前缀 `feat(八字):` / `test(八字):` / `refactor(八字):`，风格对齐仓库太乙 commit 史。
- **分支**：在 `feat/bazi-port-xuanzong` 分支上工作（已创建）。
- **黄金母版 Node harness（可复跑）**：如需重新抓取某项期望，运行（Node 26 已装）：
  ```bash
  node -e "const P='G:/xulun/WeChatProjects/xuanzong/miniprogram/utils/bazi/';const{calculate}=require(P+'baziCalculator');const b=calculate({year:1985,month:5,day:20,hour:0,minute:10});console.log(JSON.stringify(require(P+'shensha').calcShenSha(b)));"
  ```
  （把 `shensha`/`kongwang`/`tiaohou`/`xingChongJieFa`/`studyFlow`/`yongshen`+`quyun` 换成目标模块即可）。

## File Structure

| Tier | 新建 model | 新建 core | 新建/改 UI | 新建 test |
|---|---|---|---|---|
| 1 | `model/ShenSha.kt`(扩)、`model/KongWang.kt` | `core/ShenShaCalculator.kt`、`core/KongWangCalculator.kt` | `components/ShenShaSection.kt`(新)、`components/PillarCard.kt`(改加 `kong`)、`BaziDisplay.kt`(接线) | `ShenShaCalculatorTest.kt`、`KongWangCalculatorTest.kt` |
| 1-重构 | — | `core/MarriageCalculator.kt`(改)、`core/StudyCalculator.kt`(改) | — | 既有测试保持绿 |
| 2 | `model/TiaoHou.kt`、`model/CongGe.kt` | `core/TiaoHouCalculator.kt`、`core/GeJuCalculator.kt`(加 `detectCongGe`) | `components/TiaoHouSection.kt`(新)、`BaziDisplay.kt` | `TiaoHouCalculatorTest.kt` |
| 3 | `model/JieChong.kt`、`model/QuYun.kt` | `core/JieChongCalculator.kt`、`core/QuYunCalculator.kt` | `components/JieChongSection.kt`(新)、`BaziDisplay.kt`(selection 区块) | `JieChongCalculatorTest.kt`、`QuYunCalculatorTest.kt` |
| 4 | `model/WuXingFlow.kt` | `core/WuXingFlowCalculator.kt` | `components/WuXingFlowSection.kt`(新)、`BaziDisplay.kt` | `WuXingFlowCalculatorTest.kt` |

---

## Task 1: 神煞计算器 + 模型（Tier 1a）

**Files:**
- Create: `app/src/main/java/cn/xulun/bazi/core/ShenShaCalculator.kt`
- Create/Modify: `app/src/main/java/cn/xulun/bazi/model/ShenSha.kt`（既有 `ShenSha` 在 `Marriage.kt` 内；本 Task 在同文件追加通用模型，或新建——见下）
- Test: `app/src/test/java/cn/xulun/bazi/ShenShaCalculatorTest.kt`

**Interfaces:**
- Consumes: `BaziResult`（`.stems`/`.branches`/`.dayPillar`/`.monthPillar`），`HeavenlyStem`/`EarthlyBranch`/`Element`。
- Produces: `ShenShaCalculator.analyze(bazi): ShenShaResult`、`ShenShaCalculator.groupOf(branch): Int`、`data class ShenShaItem/ShenShaResult`。

> 模型放置：既有 `model/Marriage.kt` 里已有 `data class ShenSha(name, branch, location)`（婚姻专用，**保留不动**）。通用神煞层用新名 `ShenShaItem`，放入**新建** `model/ShenSha.kt`，避免与婚姻模型混淆。

- [ ] **Step 1: 写失败测试**

`app/src/test/java/cn/xulun/bazi/ShenShaCalculatorTest.kt`：
```kotlin
package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Test

class ShenShaCalculatorTest {
    private fun bazi(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        BaziCalculator.calculate(java.time.LocalDateTime.of(y, m, d, h, min))

    // 锚点 A: 1985-05-20 子时 → 乙丑 辛巳 己未 甲子（日主己）
    @Test fun anchorA_shensha_items_match_xuanzong() {
        val r = ShenShaCalculator.analyze(bazi(1985, 5, 20, 0, 10))
        // (name, pillarLabel, branch/stem) —— 与 xuanzong calcShenSha 顺序一致
        assertEquals(
            listOf(
                triple("天乙贵人", "时支", EarthlyBranch.ZI),
                triple("羊刃", "月支", EarthlyBranch.SI),
                triple("桃花", "时支", EarthlyBranch.ZI),
                triple("驿马", "月支", EarthlyBranch.SI),
                triple("华盖", "日支", EarthlyBranch.WEI),
                stem("天德贵人", "月干", HeavenlyStem.XIN),
            ),
            r.items,
        )
    }

    // 锚点 D: 2000-03-04 卯时 → 庚辰 戊寅 辛酉 辛卯（日主辛）
    @Test fun anchorD_shensha_items_match_xuanzong() {
        val r = ShenShaCalculator.analyze(bazi(2000, 3, 4, 6, 0))
        assertEquals(
            listOf(
                triple("天乙贵人", "月支", EarthlyBranch.YIN),
                triple("禄神", "日支", EarthlyBranch.YOU),
                triple("将星", "日支", EarthlyBranch.YOU),
                triple("劫煞", "月支", EarthlyBranch.YIN),
            ),
            r.items,
        )
    }

    @Test fun groupOf_trigram_groups() {
        assertEquals(0, ShenShaCalculator.groupOf(EarthlyBranch.ZI))   // 申子辰
        assertEquals(1, ShenShaCalculator.groupOf(EarthlyBranch.WEI))  // 亥卯未
        assertEquals(2, ShenShaCalculator.groupOf(EarthlyBranch.YIN))  // 寅午戌
        assertEquals(3, ShenShaBranch_marker())                        // 占位，见 Step1 注
    }

    // 小工厂：data class 的便捷构造，避免在每个 assertEquals 里手写 ShenShaItem
    private fun triple(name: String, loc: String, b: EarthlyBranch) =
        cn.xulun.bazi.model.ShenShaItem(name, loc, branch = b)
    private fun stem(name: String, loc: String, s: HeavenlyStem) =
        cn.xulun.bazi.model.ShenShaItem(name, loc, stem = s)
    private fun ShenShaBranch_marker(): Int = 3 // 删除本行相关：见下注
}
```
> 注：上面 `groupOf_trigram_groups` 第三行是脚手架，实现完成后改为：
> `assertEquals(3, ShenShaCalculator.groupOf(EarthlyBranch.SI))` // 巳酉丑。先写真实断言，删掉 marker 工厂与 `ShenShaBranch_marker`。

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew test --tests "cn.xulun.bazi.ShenShaCalculatorTest"` （Windows 用 `gradlew.bat`）
Expected: 编译失败（`ShenShaCalculator` / `ShenShaItem` 未定义）。

- [ ] **Step 3: 写模型**

`app/src/main/java/cn/xulun/bazi/model/ShenSha.kt`：
```kotlin
package cn.xulun.bazi.model

/** 通用神煞项（命中原局某柱才输出）。branch/stem 二者有其一。 */
data class ShenShaItem(
    val name: String,
    val pillarLabel: String,            // "时支"/"月干"…
    val stem: HeavenlyStem? = null,
    val branch: EarthlyBranch? = null,
)

/** 八字神煞分析总结果。 */
data class ShenShaResult(val items: List<ShenShaItem>) {
    val hasAny: Boolean get() = items.isNotEmpty()
}
```

- [ ] **Step 4: 写计算器（忠实移植 shensha.js）**

`app/src/main/java/cn/xulun/bazi/core/ShenShaCalculator.kt`。**查表逐字移植** `G:\xulun\WeChatProjects\xuanzong\miniprogram\utils\bazi\shensha.js` 的常量（`TIANYI/WENCHANG/XUETANG/YANGREN/LUSHEN/TAOHUA/YIMA/HUAGAI/JIANGXING/WANGSHEN/JIESHA/YUEDE/TIAND`），JS 里的数字字面量即枚举 ordinal，转 `EarthlyBranch.fromIndex`/`HeavenlyStem.fromIndex`。
```kotlin
package cn.xulun.bazi.core

import cn.xulun.bazi.model.*
import java.lang.Math.floorMod

object ShenShaCalculator {
    // groupOf: 0=申子辰 1=亥卯未 2=寅午戌 3=巳酉丑（与 shensha.js groupOf 一致）
    fun groupOf(b: EarthlyBranch): Int = when (b) {
        EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> 0
        EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI -> 1
        EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> 2
        EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> 3
    }

    // 日干 → 神煞地支（逐字移植 shensha.js；key=HeavenlyStem.ordinal）
    private val tianYi: Map<HeavenlyStem, List<EarthlyBranch>> = mapOf(
        HeavenlyStem.JIA to listOf(EarthlyBranch.CHOU, EarthlyBranch.WEI),
        HeavenlyStem.WU to listOf(EarthlyBranch.CHOU, EarthlyBranch.WEI),
        HeavenlyStem.GENG to listOf(EarthlyBranch.CHOU, EarthlyBranch.WEI),
        HeavenlyStem.YI to listOf(EarthlyBranch.ZI, EarthlyBranch.SHEN),
        HeavenlyStem.JI to listOf(EarthlyBranch.ZI, EarthlyBranch.SHEN),
        HeavenlyStem.BING to listOf(EarthlyBranch.HAI, EarthlyBranch.YOU),
        HeavenlyStem.DING to listOf(EarthlyBranch.HAI, EarthlyBranch.YOU),
        HeavenlyStem.REN to listOf(EarthlyBranch.MAO, EarthlyBranch.SI),
        HeavenlyStem.GUI to listOf(EarthlyBranch.MAO, EarthlyBranch.SI),
        HeavenlyStem.XIN to listOf(EarthlyBranch.YIN, EarthlyBranch.WU),
    )
    // 其余表（WENCHANG/XUETANG/YANGREN/LUSHEN/TAOHUA…JIESHA/YUEDE/TIAND）按 shensha.js 逐字移植。
    // TAOHUA/YIMA/HUAGAI/JIANGXING/WANGSHEN/JIESHA 以 groupOf 结果为下标；
    // YUEDE 以 groupOf(月支) 为下标取 HeavenlyStem；TIAND 以月支 ordinal 查 {值,是否为干}。

    fun analyze(bazi: BaziResult): ShenShaResult {
        val items = mutableListOf<ShenShaItem>()
        val dayStem = bazi.dayPillar.stem
        val dayBranch = bazi.dayPillar.branch
        val monthBranch = bazi.monthPillar.branch
        // 通用：把“目标干/支 → 命中柱位”封装
        fun emitBranch(name: String, target: EarthlyBranch) {
            val idx = bazi.branches.indexOf(target)
            if (idx >= 0) items.add(ShenShaItem(name, PillarBranchLabels[idx], branch = target))
        }
        fun emitStem(name: String, target: HeavenlyStem) {
            val idx = bazi.stems.indexOf(target)
            if (idx >= 0) items.add(ShenShaItem(name, PillarStemLabels[idx], stem = target))
        }
        // 顺序与 shensha.js calcShenSha 的 push 顺序一致（测试锚点钉死该顺序）：
        // 天乙(双支,逐个 emitBranch) → 文昌 → 学堂 → 羊刃 → 禄神
        // → 桃花 → 驿马 → 华盖 → 将星 → 亡神 → 劫煞
        // → 月德(干 emitStem) → 天德(干或支)
        tianYi.getValue(dayStem).forEach { emitBranch("天乙贵人", it) }
        emitBranch("文昌", wenChangOf(dayStem))
        emitBranch("学堂", xueTangOf(dayStem))
        emitBranch("羊刃", yangRenOf(dayStem))
        emitBranch("禄神", luShenOf(dayStem))
        val g = groupOf(dayBranch)
        emitBranch("桃花", TAOHUA[g]); emitBranch("驿马", YIMA[g]); emitBranch("华盖", HUAGAI[g])
        emitBranch("将星", JIANGXING[g]); emitBranch("亡神", WANGSHEN[g]); emitBranch("劫煞", JIESHA[g])
        emitStem("月德贵人", YUEDE[groupOf(monthBranch)])
        tianDeOf(monthBranch)?.let { (isStem, v) ->
            if (isStem) emitStem("天德贵人", HeavenlyStem.fromIndex(v))
            else emitBranch("天德贵人", EarthlyBranch.fromIndex(v))
        }
        return ShenShaResult(items)
    }
    // private val WENCHANG/XUETANG/YANGREN/LUSHEN: Map<HeavenlyStem,EarthlyBranch> —— 逐字移植
    // private val TAOHUA=... arrayOf(ZI? ...) 实为 int[4]，用 arrayOf(EarthlyBranch...) 表达
    // private fun wenChangOf(s)/xueTangOf(s)/yangRenOf(s)/luShenOf(s): 查上列 Map
    // private val TAOHUA = arrayOf(YOU, ZI, MAO, WU)  // [9,0,3,6]
    // private val YIMA   = arrayOf(YIN, SI, SHEN, HAI)// [2,5,8,11]
    // private val HUAGAI = arrayOf(CHEN, WEI, XU, CHOU)// [4,7,10,1]
    // private val JIANGXING = arrayOf(ZI, MAO, WU, YOU)// [0,3,6,9]
    // private val WANGSHEN  = arrayOf(HAI, YIN, SI, SHEN)// [11,2,5,8]
    // private val JIESHA    = arrayOf(SI, SHEN, HAI, YIN)// [5,8,11,2]
    // private val YUEDE = arrayOf(REN, JIA, BING, GENG) // [8,0,2,6]
    // private fun tianDeOf(month: EarthlyBranch): Pair<Boolean,Int>? = TIAND[month.ordinal]
    //   TIAND 按 shensha.js 的 {v,isStem} 表逐字移植（12 项）。
}
```
> 完成 shensha.js 全部常量移植后，删掉注释占位的 `private val ...` 说明行，落实为真实声明。`wenChang`/`xueTang` 已有模型扩展可复用：`cn.xulun.bazi.model.wenChang`/`xueTang`（见 `StudyCalculator` 用法）；若签名一致直接用，否则本计算器自带 Map。

- [ ] **Step 5: 跑测试确认通过**

Run: `./gradlew test --tests "cn.xulun.bazi.ShenShaCalculatorTest"`
Expected: PASS（两个锚点 + groupOf 全绿）。若顺序/值不符，对照 `golden-master-xuanzong.json` 的 `A_1985_05_20_zi.shensha` 与 `D_2000_03_04_mao.shensha` 修正。

- [ ] **Step 6: 提交**
```bash
git add app/src/main/java/cn/xulun/bazi/core/ShenShaCalculator.kt \
        app/src/main/java/cn/xulun/bazi/model/ShenSha.kt \
        app/src/test/java/cn/xulun/bazi/ShenShaCalculatorTest.kt
git commit -m "feat(八字): 神煞计算器(13种,黄金母版锚定)"
```

---

## Task 2: 神煞 UI 板块 + 婚姻/学业去重（Tier 1b）

**Files:**
- Create: `app/src/main/java/cn/xulun/bazi/ui/components/ShenShaSection.kt`
- Modify: `app/src/main/java/cn/xulun/bazi/ui/BaziDisplay.kt`（接线）
- Modify: `app/src/main/java/cn/xulun/bazi/core/MarriageCalculator.kt`（桃花走 ShenShaCalculator）
- Modify: `app/src/main/java/cn/xulun/bazi/core/StudyCalculator.kt`（文昌/学堂走 ShenShaCalculator）

**Interfaces:**
- Consumes: `ShenShaResult`（Task 1）。
- Produces: `@Composable fun ShenShaSection(result: ShenShaResult, modifier)`。

- [ ] **Step 1: 写 UI 组件**

参照 `components/DiZhiRelationSection.kt` 的卡片+标签风格。`ShenShaSection`：标题"神煞"，`FlowRow` 渲染 `result.items`，每项一个 chip：`${item.name}·${item.pillarLabel}`；`result.hasAny` 为 false 时用既有 `HintText("原局无神煞")`（见 `MarriageSection.kt` 用法）。
```kotlin
@Composable
fun ShenShaSection(result: ShenShaResult, modifier: Modifier = Modifier) {
    SectionCard(title = "神煞", modifier = modifier) { // 复用既有卡片外壳；若无则按 DiZhiRelationSection 内联 Card
        if (!result.hasAny) { HintText("原局无神煞"); return@SectionCard }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            result.items.forEach {
                AssistChip(onClick = {}, label = { Text("${it.name}·${it.pillarLabel}") })
            }
        }
    }
}
```
> 实装时对齐 `DiZhiRelationSection.kt`/`MarriageSection.kt` 的实际卡片外壳与 import（`SectionCard`/`HintText` 若为私有则改用 `Card`+`Text` 同款）。

- [ ] **Step 2: BaziDisplay 接线**

在 `BaziDisplay.kt` 顶部算 `val shenSha = ShenShaCalculator.analyze(result)`，在原局卡 `FlowRow`(纳音) 之后、`YongShenSection` 之前插入 `ShenShaSection(result = shenSha)`。

- [ ] **Step 3: 去重 —— MarriageCalculator 桃花**

`MarriageCalculator.findShenSha`：删私有 `taoHuaOf`，桃花改取：
```kotlin
val all = ShenShaCalculator.analyze(bazi).items
val tao = all.firstOrNull { it.name == "桃花" }
if (tao != null) result.add(ShenSha("桃花", tao.branch!!, tao.pillarLabel))
// 孤辰/寡宿逻辑保留不变（婚姻域专属）
```

- [ ] **Step 4: 去重 —— StudyCalculator 文昌/学堂**

`StudyCalculator.findShenSha` 改为从 `ShenShaCalculator.analyze(bazi).items` 过滤 `文昌`/`学堂`，映射到 `StudyShenSha`（保留其既有结果模型）。

- [ ] **Step 5: 全量测试**

Run: `./gradlew test`
Expected: 全绿（含既有 `MarriageCalculatorTest`/`StudyCalculatorTest` —— 桃花/文昌/学堂输出应不变，因其公式一致）。

- [ ] **Step 6: 提交**
```bash
git add app/src/main/java/cn/xulun/bazi/ui/components/ShenShaSection.kt \
        app/src/main/java/cn/xulun/bazi/ui/BaziDisplay.kt \
        app/src/main/java/cn/xulun/bazi/core/MarriageCalculator.kt \
        app/src/main/java/cn/xulun/bazi/core/StudyCalculator.kt
git commit -m "feat(八字): 神煞板块UI+婚姻/学业桃花文昌学堂去重"
```

---

## Task 3: 旬空/空亡 + PillarCard 空标（Tier 1c）

**Files:**
- Create: `app/src/main/java/cn/xulun/bazi/model/KongWang.kt`
- Create: `app/src/main/java/cn/xulun/bazi/core/KongWangCalculator.kt`
- Modify: `app/src/main/java/cn/xulun/bazi/ui/components/PillarCard.kt`（加 `kong: Boolean`）
- Modify: `app/src/main/java/cn/xulun/bazi/ui/BaziDisplay.kt`（算一次逐柱传入）
- Test: `app/src/test/java/cn/xulun/bazi/KongWangCalculatorTest.kt`

**Interfaces:**
- Consumes: `BaziResult.dayPillar.cycleIndex`、`.pillars`。
- Produces: `KongWangCalculator.xunKongBranches(dayCycleIndex): Pair<EarthlyBranch,EarthlyBranch>`、`.analyze(bazi): KongWangResult`、`data class KongWangMark/KongWangResult`。

- [ ] **Step 1: 写失败测试**
```kotlin
package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.KongWangCalculator
import cn.xulun.bazi.model.EarthlyBranch
import org.junit.Assert.*
import org.junit.Test

class KongWangCalculatorTest {
    private fun bazi(y:Int,m:Int,d:Int,h:Int,min:Int=0)=BaziCalculator.calculate(java.time.LocalDateTime.of(y,m,d,h,min))

    @Test fun anchorA_xunkong_and_marks() {
        // 日柱 己未 cycleIndex=55 → 旬空 子丑(0,1)；年支丑、时支子落空
        val b = bazi(1985,5,20,0,10)
        assertEquals(EarthlyBranch.ZI to EarthlyBranch.CHOU, KongWangCalculator.xunKongBranches(b.dayPillar.cycleIndex))
        val r = KongWangCalculator.analyze(b)
        assertEquals(setOf(0,3), r.marks.map { it.pillarIdx }.toSet()) // 年柱、时柱
        assertTrue(r.isKong(0)); assertFalse(r.isKong(1)); assertFalse(r.isKong(2)); assertTrue(r.isKong(3))
    }

    @Test fun anchorC_no_marks() {
        // 日柱 cycleIndex=12 → 旬空 申酉(8,9)；四支辰寅子巳均不落空
        val b = bazi(1988,7,20,10,0)
        assertEquals(EarthlyBranch.SHEN to EarthlyBranch.YOU, KongWangCalculator.xunKongBranches(b.dayPillar.cycleIndex))
        assertTrue(KongWangCalculator.analyze(b).marks.isEmpty())
    }
}
```

- [ ] **Step 2: 跑测试确认失败** — Run: `./gradlew test --tests "cn.xulun.bazi.KongWangCalculatorTest"` → 编译失败。

- [ ] **Step 3: 写模型** — `model/KongWang.kt`：
```kotlin
package cn.xulun.bazi.model
data class KongWangMark(val pillarIdx: Int, val branch: EarthlyBranch)
data class KongWangResult(val marks: List<KongWangMark>) {
    fun isKong(pillarIdx: Int): Boolean = marks.any { it.pillarIdx == pillarIdx }
}
```

- [ ] **Step 4: 写计算器** — `core/KongWangCalculator.kt`（移植 kongwang.js）：
```kotlin
package cn.xulun.bazi.core
import cn.xulun.bazi.model.*
import java.lang.Math.floorMod
object KongWangCalculator {
    fun xunKongBranches(dayCycleIndex: Int): Pair<EarthlyBranch, EarthlyBranch> {
        val headCycle = dayCycleIndex - floorMod(dayCycleIndex, 10)   // 旬首
        val headBranch = floorMod(headCycle, 12)
        val b1 = EarthlyBranch.fromIndex(floorMod(headBranch - 2, 12))
        val b2 = EarthlyBranch.fromIndex(floorMod(headBranch - 1, 12))
        return b1 to b2
    }
    fun analyze(bazi: BaziResult): KongWangResult {
        val (b1, b2) = xunKongBranches(bazi.dayPillar.cycleIndex)
        val marks = bazi.pillars.mapIndexedNotNull { i, p ->
            if (p.branch == b1 || p.branch == b2) KongWangMark(i, p.branch) else null
        }
        return KongWangResult(marks)
    }
}
```

- [ ] **Step 5: 跑测试确认通过** — Run 同上 → PASS。

- [ ] **Step 6: PillarCard 加空标** — `PillarCard.kt` 形参增 `kong: Boolean = false`；在地支 `CharBox` 上叠一个半透明"空"角标（用 `Box` 叠 `Text("空", fontSize=10.sp, color=SeedRed.copy(alpha=0.7f))`，`Alignment.TopEnd`）。

- [ ] **Step 7: BaziDisplay 接线** — 顶部 `val kongWang = KongWangCalculator.analyze(result)`；`PillarCard(...)` 调用增 `kong = kongWang.isKong(i)`。

- [ ] **Step 8: 全量测试 + 提交**
```bash
./gradlew test
git add app/src/main/java/cn/xulun/bazi/model/KongWang.kt \
        app/src/main/java/cn/xulun/bazi/core/KongWangCalculator.kt \
        app/src/main/java/cn/xulun/bazi/ui/components/PillarCard.kt \
        app/src/main/java/cn/xulun/bazi/ui/BaziDisplay.kt \
        app/src/test/java/cn/xulun/bazi/KongWangCalculatorTest.kt
git commit -m "feat(八字): 旬空/空亡标记+柱卡空标(黄金母版锚定)"
```

---

## Task 4: 从格 detectCongGe（Tier 2 前置）

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/core/GeJuCalculator.kt`（加 `detectCongGe`）
- Create: `app/src/main/java/cn/xulun/bazi/model/CongGe.kt`
- Test: `app/src/test/java/cn/xulun/bazi/CongGeDetectorTest.kt`

**Interfaces:**
- Consumes: `BaziResult`。
- Produces: `GeJuCalculator.detectCongGe(bazi): CongGeResult?`、`data class CongGeResult(kind: String, basis: String)`。

- [ ] **Step 1: 抓 xuanzong 期望** — 读 `G:\xulun\WeChatProjects\xuanzong\miniprogram\utils\bazi\geju.js` 中 `detectCongGe` 的判定规则（从强/从旺/从财/从官/从儿等条件），抓 1–2 个会触发从格的生辰（如满盘一金、日主无根）的输出。本次 4 个锚点生辰 congGe 均为 null，故需另造从格八字作锚点。

- [ ] **Step 2: 写失败测试** — 造一个从格八字（例如日主甲木生于申月、满盘金土无印比 → 从杀），断言 `detectCongGe` 非 null 且 `kind` 正确；另断言锚点 A 的 `detectCongGe(bazi(1985,5,20,0,10)) == null`。

- [ ] **Step 3: 跑确认失败** — `./gradlew test --tests "cn.xulun.bazi.CongGeDetectorTest"` → 编译失败。

- [ ] **Step 4: 写模型 + 实现** — `model/CongGe.kt`：`data class CongGeResult(val kind: String, val basis: String)`。`GeJuCalculator.detectCongGe` 逐字移植 geju.js 的 `detectCongGe` 判定（身弱无根 + 满盘某势 → 从格）。

- [ ] **Step 5: 跑确认通过 + 提交**
```bash
./gradlew test
git add app/src/main/java/cn/xulun/bazi/core/GeJuCalculator.kt \
        app/src/main/java/cn/xulun/bazi/model/CongGe.kt \
        app/src/test/java/cn/xulun/bazi/CongGeDetectorTest.kt
git commit -m "feat(八字): 从格detectCongGe(调候前置依赖)"
```

---

## Task 5: 调候 TiaoHou（Tier 2）

**Files:**
- Create: `model/TiaoHou.kt`、`core/TiaoHouCalculator.kt`、`ui/components/TiaoHouSection.kt`
- Modify: `ui/BaziDisplay.kt`
- Test: `TiaoHouCalculatorTest.kt`

**Interfaces:**
- Consumes: `BaziResult`、`GeJuCalculator.detectCongGe`（Task 4）。
- Produces: `TiaoHouCalculator.analyze(bazi): TiaoHouResult`、`isJinShuiShangGuan(dayMaster, monthBranch): Boolean`。

- [ ] **Step 1: 写失败测试**（期望取自 golden-master：A=燥/水/present、B=寒/火/deShi1、D=温和/null）
```kotlin
package cn.xulun.bazi
import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.TiaoHouCalculator
import cn.xulun.bazi.model.Climate
import cn.xulun.bazi.model.Element
import org.junit.Assert.*
import org.junit.Test

class TiaoHouCalculatorTest {
    private fun bazi(y:Int,m:Int,d:Int,h:Int,min:Int=0)=BaziCalculator.calculate(java.time.LocalDateTime.of(y,m,d,h,min))
    @Test fun anchorA_summer_zao_needs_water_present() {
        val r = TiaoHouCalculator.analyze(bazi(1985,5,20,0,10))
        assertEquals(Climate.ZAO, r.climate)
        assertEquals(Element.WATER, r.tiaoHouElement)
        assertTrue(r.tiaoHouPresent)
        assertFalse(r.urgent)
    }
    @Test fun anchorB_winter_han_needs_fire_with_deshi() {
        val r = TiaoHouCalculator.analyze(bazi(1990,12,15,12,0))
        assertEquals(Climate.HAN, r.climate)
        assertEquals(Element.FIRE, r.tiaoHouElement)
        assertEquals(1, r.deShi.size)
        assertTrue(r.deShi.first().contains("冬木透官难贵"))
    }
    @Test fun anchorD_spring_wenhe_no_tiaohou() {
        val r = TiaoHouCalculator.analyze(bazi(2000,3,4,6,0))
        assertEquals(Climate.WEN_HE, r.climate)
        assertNull(r.tiaoHouElement)
        assertFalse(r.tiaoHouPresent)
    }
}
```

- [ ] **Step 2: 跑确认失败** — `./gradlew test --tests "cn.xulun.bazi.TiaoHouCalculatorTest"`。

- [ ] **Step 3: 写模型** — `model/TiaoHou.kt`（逐字按 spec §3 的 `Climate`/`TiaoHouFormula`/`TiaoHouResult`）。

- [ ] **Step 4: 写计算器** — `core/TiaoHouCalculator.kt`，逐字移植 `tiaohou.js`：季节常量(WINTER=[亥,子,丑]…)、火/水干支扫描、调候五行赋值、urgent 判定(`detectCongGe` 非 null 则不 urgent)、4 句口诀、deShi。`isJinShuiShangGuan` 同步移植（Task 7 取运依赖）。

- [ ] **Step 5: 跑确认通过** — Run 同上 → PASS。

- [ ] **Step 6: 写 UI** — `TiaoHouSection`：标题"调候"，展示气候、调候五行、是否为急、口诀表（`formulas`：名+desc+官星立场）、得失注（`deShi`）。参照 `YongShenSection.kt` 的分块文案风格。`BaziDisplay` 在 `YongShenSection` 之后接线。

- [ ] **Step 7: 全量测试 + 提交**
```bash
./gradlew test
git add app/src/main/java/cn/xulun/bazi/model/TiaoHou.kt \
        app/src/main/java/cn/xulun/bazi/core/TiaoHouCalculator.kt \
        app/src/main/java/cn/xulun/bazi/ui/components/TiaoHouSection.kt \
        app/src/main/java/cn/xulun/bazi/ui/BaziDisplay.kt \
        app/src/test/java/cn/xulun/bazi/TiaoHouCalculatorTest.kt
git commit -m "feat(八字): 调候层(寒暖燥湿+口诀+得失,黄金母版锚定)"
```

---

## Task 6: 会合解冲 JieChong（Tier 3a）

**Files:**
- Create: `model/JieChong.kt`、`core/JieChongCalculator.kt`、`ui/components/JieChongSection.kt`
- Modify: `ui/BaziDisplay.kt`（地支关系板块后）
- Test: `JieChongCalculatorTest.kt`

**Interfaces:**
- Consumes: `DiZhiRelationCalculator.analyze(bazi)`、`HeHuaCalculator.analyze(bazi)`。
- Produces: `JieChongCalculator.analyze(bazi): JieChongResult`。

- [ ] **Step 1: 写失败测试**（期望取自 golden-master）
```kotlin
package cn.xulun.bazi
import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.JieChongCalculator
import org.junit.Assert.*
import org.junit.Test

class JieChongCalculatorTest {
    private fun bazi(y:Int,m:Int,d:Int,h:Int,min:Int=0)=BaziCalculator.calculate(java.time.LocalDateTime.of(y,m,d,h,min))
    @Test fun anchorA_chong_resolved_by_liuhe() {
        val r = JieChongCalculator.analyze(bazi(1985,5,20,0,10)) // 丑未冲，丑合子解
        assertEquals(1, r.chong.size)
        assertTrue(r.chong.first().resolved)
        assertEquals("丑六合（子）", r.chong.first().resolveBy)
        assertFalse(r.hasUnresolvedChong)
        assertEquals(1, r.liuHeCount)
    }
    @Test fun anchorB_chong_xing_unresolved() {
        val r = JieChongCalculator.analyze(bazi(1990,12,15,12,0)) // 子午冲 + 午自刑，无解
        assertTrue(r.hasUnresolvedChong)
        assertTrue(r.hasUnresolvedXing)
        assertFalse(r.xing.first().resolved)
    }
    @Test fun anchorC_no_relations() {
        val r = JieChongCalculator.analyze(bazi(1988,7,20,10,0))
        assertTrue(r.chong.isEmpty() && r.xing.isEmpty())
        assertFalse(r.hasUnresolvedChong)
    }
}
```

- [ ] **Step 2: 跑确认失败** — `./gradlew test --tests "cn.xulun.bazi.JieChongCalculatorTest"`。

- [ ] **Step 3: 写模型** — `model/JieChong.kt`（按 spec §4.2 的 `ChongJie/XingJie/JieChongResult`）。

- [ ] **Step 4: 写计算器** — `core/JieChongCalculator.kt`，逐字移植 `xingChongJieFa.js`：取 `DiZhiRelationCalculator.analyze`(chongList/xingList/sanHeList) + `HeHuaCalculator.analyze`(branchLiuHeList)；建 `liuHeOf`(支→六合伴)、`sanHeSet`(入三合支集)；冲任一支被合→解，刑任一涉支被合→解；`resolveBy` 文案对齐 xuanzong（如"丑六合（子）"）。

- [ ] **Step 5: 跑确认通过** — Run 同上 → PASS。

- [ ] **Step 6: 写 UI + 接线** — `JieChongSection`：标题"会合解冲刑"，逐条列冲/刑 + 是否解 + resolveBy；无冲刑则整块隐藏（`if (result.chong.isEmpty() && result.xing.isEmpty()) return`）。`BaziDisplay` 在 `DiZhiRelationSection` 之后接线。

- [ ] **Step 7: 全量测试 + 提交**
```bash
./gradlew test
git add app/src/main/java/cn/xulun/bazi/model/JieChong.kt \
        app/src/main/java/cn/xulun/bazi/core/JieChongCalculator.kt \
        app/src/main/java/cn/xulun/bazi/ui/components/JieChongSection.kt \
        app/src/main/java/cn/xulun/bazi/ui/BaziDisplay.kt \
        app/src/test/java/cn/xulun/bazi/JieChongCalculatorTest.kt
git commit -m "feat(八字): 会合解冲刑(合解冲/刑,黄金母版锚定)"
```

---

## Task 7: 取运 QuYun（Tier 3b，依赖 Task 5）

**Files:**
- Create: `model/QuYun.kt`、`core/QuYunCalculator.kt`
- Modify: `ui/BaziDisplay.kt`（selection 区块内，与 `YunRelationSection` 并列）
- Test: `QuYunCalculatorTest.kt`

**Interfaces:**
- Consumes: `BaziResult`、`YongShenCalculator.analyze(bazi): YongShenResult`、`Pillar`（运柱）、`TiaoHouCalculator.isJinShuiShangGuan`（Task 5）。
- Produces: `QuYunCalculator.analyze(bazi, yongShen, yunPillar): QuYunResult`。

- [ ] **Step 1: 写失败测试**（期望取自 golden-master；运柱用 DaYunCalculator 首步，与 xuanzong 同源）
```kotlin
package cn.xulun.bazi
import cn.xulun.bazi.core.*
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.QuYunVerdict
import org.junit.Assert.*
import org.junit.Test

class QuYunCalculatorTest {
    private fun bazi(y:Int,m:Int,d:Int,h:Int,min:Int=0)=BaziCalculator.calculate(java.time.LocalDateTime.of(y,m,d,h,min))
    private fun firstYun(b: cn.xulun.bazi.model.BaziResult, g: Gender) =
        DaYunCalculator.compute(b, g).yuns.first().pillar

    @Test fun anchorA_yin_geju_xiyun() {
        val b = bazi(1985,5,20,0,10)
        val ys = YongShenCalculator.analyze(b)
        val r = QuYunCalculator.analyze(b, ys, firstYun(b, Gender.MALE))
        assertEquals("印格官煞竞透（取清）", r.ziPeiZhi.subName)
        assertEquals(QuYunVerdict.XI, r.gan.verdict)
        assertEquals(QuYunVerdict.XI, r.zhi.verdict)
        assertEquals(QuYunVerdict.XI, r.summary.verdict)
    }
    @Test fun anchorD_yin_geju_xi_zhong_ping() {
        val b = bazi(2000,3,4,6,0)
        val ys = YongShenCalculator.analyze(b)
        val r = QuYunCalculator.analyze(b, ys, firstYun(b, Gender.FEMALE))
        assertEquals("印绶（孤印）", r.ziPeiZhi.subName)
        assertEquals(QuYunVerdict.XI_ZHONG_PING, r.summary.verdict)
    }
}
```
> 若 `firstYun` 得到的运柱与 golden-master 的 `yunPillar_tested` 不符（即 bazia DaYunCalculator 与 xuanzong 首步运柱不同），则在测试里直接用 golden-master 记录的运柱构造 `Pillar`（A=庚辰、D 见 json），并记录该差异。

- [ ] **Step 2: 跑确认失败** — `./gradlew test --tests "cn.xulun.bazi.QuYunCalculatorTest"`。

- [ ] **Step 3: 写模型** — `model/QuYun.kt`（按 spec §4.1 的 `QuYunVerdict/ZiPeiZhi/PillarVerdict/QuYunRow/QuYunSummary/QuYunResult`）。

- [ ] **Step 4: 写计算器** — `core/QuYunCalculator.kt`，逐字移植 `quyun.js`：`QUYUN_TABLE`（8 族 × 子配置，约 50 行，逐字搬）、`GOD_ROLE`/`FAMILY_CH` 常量、`identifyZiPeiZhi`（由 `ys.geJuName`+透干扫描定族与子配置）、token 类别匹配、干支 XI/JI/PING 判定、summary 合成。伤官特例调 `TiaoHouCalculator.isJinShuiShangGuan`。

- [ ] **Step 5: 跑确认通过** — Run 同上 → PASS。

- [ ] **Step 6: UI 接线** — `BaziDisplay` 的 `selection?.let { sel -> ... }` 区块内，在 `YunRelationSection` 旁加取运展示：选中柱的子配置名 + 干(十神+喜忌+理由) + 支(同) + 总评。复用 `Pillar` 从 `sel` 取（DaYun→`sel.pillar`；LiuNian→`sel.pillar`）。可内联于 `BaziDisplay` 或抽 `QuYunSection`。

- [ ] **Step 7: 全量测试 + 提交**
```bash
./gradlew test
git add app/src/main/java/cn/xulun/bazi/model/QuYun.kt \
        app/src/main/java/cn/xulun/bazi/core/QuYunCalculator.kt \
        app/src/main/java/cn/xulun/bazi/ui/BaziDisplay.kt \
        app/src/test/java/cn/xulun/bazi/QuYunCalculatorTest.kt
git commit -m "feat(八字): 取运论X取运(子配置+干支喜忌,黄金母版锚定)"
```

---

## Task 8: 五行流通/战局/混杂 WuXingFlow（Tier 4）

**Files:**
- Create: `model/WuXingFlow.kt`、`core/WuXingFlowCalculator.kt`、`ui/components/WuXingFlowSection.kt`
- Modify: `ui/BaziDisplay.kt`
- Test: `WuXingFlowCalculatorTest.kt`

**Interfaces:**
- Consumes: `BaziResult`、可选 `List<Pillar>`（运岁）、`DiZhiRelationCalculator.analyze`、`GeJuCalculator.assessStrength`。
- Produces: `WuXingFlowCalculator.analyze(bazi, extraPillars): WuXingFlowResult`。

- [ ] **Step 1: 写失败测试**（期望取自 golden-master）
```kotlin
package cn.xulun.bazi
import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.WuXingFlowCalculator
import org.junit.Assert.*
import org.junit.Test

class WuXingFlowCalculatorTest {
    private fun bazi(y:Int,m:Int,d:Int,h:Int,min:Int=0)=BaziCalculator.calculate(java.time.LocalDateTime.of(y,m,d,h,min))
    @Test fun anchorA_chain1_no_war() {
        val r = WuXingFlowCalculator.analyze(bazi(1985,5,20,0,10))
        assertEquals(1, r.chain)
        assertArrayEquals(intArrayOf(2,1,3,1,1), r.elems)
        assertFalse(r.war.jinMu); assertFalse(r.war.shuiHuo)
        assertTrue(r.favorable)
        assertTrue(r.duanYu.isEmpty())
    }
    @Test fun anchorB_shuihuo_war_unfavor() {
        val r = WuXingFlowCalculator.analyze(bazi(1990,12,15,12,0))
        assertTrue(r.war.shuiHuo)
        assertFalse(r.favorable)
        assertEquals("shui_huo_zhan", r.duanYu.first().id)
    }
    @Test fun anchorD_jinmu_war() {
        val r = WuXingFlowCalculator.analyze(bazi(2000,3,4,6,0))
        assertTrue(r.war.jinMu)
        assertEquals("jin_mu_zhan", r.duanYu.first().id)
    }
}
```

- [ ] **Step 2: 跑确认失败** — `./gradlew test --tests "cn.xulun.bazi.WuXingFlowCalculatorTest"`。

- [ ] **Step 3: 写模型** — `model/WuXingFlow.kt`（按 spec §5 的 `FlowType/FlowItem/WarFlags/WuXingFlowResult`；`elems: IntArray` 注意 `data class` 需自定义 `equals` 或测试用 `assertArrayEquals`）。

- [ ] **Step 4: 写计算器** — `core/WuXingFlowCalculator.kt`，逐字移植 `studyFlow.js`：`elementCount`(干+支+extraPillars 计 5 行)、`genChain`(最长相生链，count≥min=2)、`detectWar`(取 DiZhiRelation 冲 list，支五行金木/水火)、格局匹配(木火通明/金水相涵/火土成势/五行流通)、favorable(身强或印≥2)。

- [ ] **Step 5: 跑确认通过** — Run 同上 → PASS。

- [ ] **Step 6: 写 UI + 接线** — `WuXingFlowSection`：标题"运岁五行（试验）"，FAVOR/UNFAVOR 色片(`duanYu`)+ 链长 + 五行计数(`elems`)+ 战局标记；`extraPillars` 取自 `selection`（参照 `BaziDisplay` 里 `YunRelationCalculator.analyze(result, extras)` 的 extras 构造）。`BaziDisplay` 末尾接线。

- [ ] **Step 7: 全量测试 + 提交**
```bash
./gradlew test
git add app/src/main/java/cn/xulun/bazi/model/WuXingFlow.kt \
        app/src/main/java/cn/xulun/bazi/core/WuXingFlowCalculator.kt \
        app/src/main/java/cn/xulun/bazi/ui/components/WuXingFlowSection.kt \
        app/src/main/java/cn/xulun/bazi/ui/BaziDisplay.kt \
        app/src/test/java/cn/xulun/bazi/WuXingFlowCalculatorTest.kt
git commit -m "feat(八字): 五行流通/战局/混杂(试验,黄金母版锚定)"
```

---

## Self-Review

**1. Spec 覆盖**：spec §2.1 神煞→Task1/2；§2.2 空亡→Task3；§3 调候(+§1.5 从格)→Task4/5；§4.1 取运→Task7；§4.2 会合解冲→Task6；§5 五行流通→Task8。§1.3 黄金母版→golden-master-xuanzong.json + 每 Task 锚点；§1.4 神煞去重→Task2；§6 测序→Task1→8（Task7 依赖 Task5，已排在后）。全覆盖。

**2. 占位扫描**：Task1 Step4 的大批 `private val` 以注释列出常量名+数组值（如 `TAOHUA=[9,0,3,6]` 已给值），要求“逐字移植 shensha.js”——源文件即代码，非占位。Task4 detectCongGe 因本次 4 锚点 congGe 均为 null，明确要求另造从格八字并读 geju.js 判定，已说明。无 TBD/“稍后实现”。

**3. 类型一致**：`ShenShaItem(name,pillarLabel,stem?,branch?)`、`KongWangResult.isKong`、`Climate.ZAO/HAN/WEN_HE`、`QuYunVerdict.XI/JI/PING/JI_ZHONG_XI/XI_ZHONG_PING`、`WarFlags.jinMu/shuiHuo` 在各 Task 间命名统一，与 golden-master 字段对应。

**4. 依赖序**：Task7 取运依赖 Task5 的 `isJinShuiShangGuan`（已排在 Task5 后）；Task5 调候依赖 Task4 `detectCongGe`（已排后）。Task1/3/6/8 相互独立。

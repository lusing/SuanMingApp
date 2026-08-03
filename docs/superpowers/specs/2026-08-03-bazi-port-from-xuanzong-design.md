# 八字功能移植设计：xuanzong（微信小程序）→ bazia（Android/Kotlin）

- 日期：2026-08-03
- 来源工程：`G:\xulun\WeChatProjects\xuanzong\miniprogram\utils\bazi\`（纯 CommonJS，零 `wx.*`，可在 Node 跑）
- 目标工程：`G:\code\zhouyi\bazia\app\src\main\java\cn\xulun\bazi\`（Kotlin/Compose）
- 范围：xuanzong 已有、bazia 缺失的 6 项八字分析功能，全量移植（model + calculator + UI + 测试）

## 0. 背景与差距

两工程同源（`BaziCalculator`/`DaYunCalculator`/`GeJuCalculator`/`YongShenCalculator`/`HeHuaCalculator` 等同名同构），xuanzong 在分析层领先。已核实 bazia **缺失**而 xuanzong **已有**的 6 项：

1. 神煞层（13 种，独立）
2. 旬空/空亡（八字原局）
3. 调候（寒暖燥湿）
4. 取运（论 X 取运）
5. 会合解冲刑
6. 五行流通/战局/混杂（试验）

二者皆已具备（不移植）：排盘、大运流年、格局、身强弱、十神、藏干、纳音、用神相神成败、合化、三合/六冲/相刑、运岁关系、喜忌、财运、婚姻、学业。胎元/命宫/身宫：**两工程皆无**，不在范围。

## 1. 总体约定

### 1.1 文件模式（对齐 bazia 既有）
每项功能四件套：
- `model/Xxx.kt`：文档化 `data class`。
- `core/XxxCalculator.kt`：`object XxxCalculator { fun analyze(...): XxxResult }`。
- `ui/components/XxxSection.kt`：Material3 Compose 组件。
- `app/src/test/java/cn/xulun/bazi/XxxCalculatorTest.kt`：单元测试。

全部在 `ui/BaziDisplay.kt` 接线渲染（原局类置于原局卡/用神之后；行运类置于 `selection` 选中区块内，与 `YunRelationSection` 并列）。

### 1.2 编码风格
- 索引一律用既有 `HeavenlyStem` / `EarthlyBranch` enum 的 `.ordinal`，不散落裸 Int。
- 查表用 `Map` / `Array` / `when`；命名、注释密度、文档注释风格对齐 `HeHuaCalculator.kt`。
- 复用既有 `branchHiddenStems` / `tenGodOfStem` / `tenGodOfBranch` / `PillarStemLabels` / `PillarBranchLabels` / `elementControls` / `elementGenerates`。

### 1.3 验证策略：黄金母版（golden-master）
`F:\code\bazi` Dart 参考当前未挂载。这 6 项新功能改为以 **xuanzong 引擎为基准**：
- 锚点生辰：每项选 3–5 个（含已用既有锚点 `1985-05-20 子时 男` 等 + 边界：极寒/极燥、含冲刑、透干多见、从格）。
- 流程：Node 跑 xuanzong 对应 `.js` → 抓返回 JSON → 转成 Kotlin 断言（字段逐一 `assertEquals`）。
- 既有 18 个测试文件不受影响，全量保持绿。

### 1.4 神煞单一真源（重构）
- 新建统一 `ShenShaCalculator`。
- `MarriageCalculator.findShenSha` 的**桃花**改从 `ShenShaCalculator` 过滤取用，删除其私有 `taoHuaOf`。
- `StudyCalculator.findShenSha` 的**文昌/学堂**同理（复用既有 `dayMaster.wenChang`/`xueTang` 模型扩展，由 `ShenShaCalculator` 统一出口）。
- **孤辰/寡宿**按 xuanzong 惯例留在 `MarriageCalculator`（婚姻域专属，不进通用神煞层）。

### 1.5 从格前置依赖
bazia `GeJuCalculator` 明确只覆盖正格（见其 line 19 注释），而调候的 `urgent` 逻辑依赖 `detectCongGe`。故 Tier 2 先从 xuanzong `geju.js` 移植 `detectCongGe(bazi): CongGeResult?`（小型 helper，置于 `GeJuCalculator` 或独立 `CongGeDetector`）。

## 2. Tier 1 — 神煞层 + 旬空/空亡

### 2.1 神煞（13 种）
**新增文件**：`core/ShenShaCalculator.kt`、扩展 `model/ShenSha.kt`、`ui/components/ShenShaSection.kt`、`ShenShaCalculatorTest.kt`。

**主入口**：
```kotlin
object ShenShaCalculator {
    fun analyze(bazi: BaziResult): ShenShaResult
    fun groupOf(branch: EarthlyBranch): Int   // 0=申子辰 1=亥卯未 2=寅午戌 3=巳酉丑
}
```

**结果模型**：
```kotlin
data class ShenShaItem(
    val name: String,            // "天乙贵人" 等
    val pillarLabel: String,     // "年干"/"月支"/"日支"/"时干"…
    val stem: HeavenlyStem? = null,
    val branch: EarthlyBranch? = null,
)
data class ShenShaResult(val items: List<ShenShaItem>) { val hasAny: Boolean }
```
仅返回**目标干支实际落在四柱某柱**上的神煞项（与 xuanzong `calcShenSha` 一致：算出目标干/支 → 命中四柱才输出 `pillarLabel`）。

**13 张查表（原样移植，索引用 .ordinal）**：

| 神煞 | 锚点 | 规则 |
|---|---|---|
| 天乙贵人 | 日干 | 甲戊庚→丑未；乙己→子申；丙丁→亥酉；壬癸→卯巳；辛→寅午（双支） |
| 文昌 | 日干 | 甲巳 乙午 丙戊申 丁己酉 庚亥 辛子 壬寅 癸卯 |
| 学堂 | 日干 | 甲亥 乙午 丙戊寅 丁己酉 庚巳 辛子 壬申 癸卯 |
| 羊刃 | 日干 | 甲卯 乙寅 丙戊午 丁己巳 庚酉 辛申 壬子 癸亥 |
| 禄神 | 日干 | 甲寅 乙卯 丙戊巳 丁己午 庚申 辛酉 壬亥 癸子 |
| 桃花 | 日支(group) | 咸池：申子辰→酉 寅午戌→卯 巳酉丑→午 亥卯未→子 |
| 驿马 | 日支(group) | 申子辰→寅 亥卯未→巳 寅午戌→申 巳酉丑→亥 |
| 华盖 | 日支(group) | 申子辰→辰 亥卯未→未 寅午戌→戌 巳酉丑→丑 |
| 将星 | 日支(group) | 申子辰→子 亥卯未→卯 寅午戌→午 巳酉丑→酉 |
| 亡神 | 日支(group) | 申子辰→亥 亥卯未→寅 寅午戌→巳 巳酉丑→申 |
| 劫煞 | 日支(group) | 申子辰→巳 亥卯未→申 寅午戌→亥 巳酉丑→寅 |
| 月德贵人 | 月支(group) | 申子辰→壬 亥卯未→甲 寅午戌→丙 巳酉丑→庚（干） |
| 天德贵人 | 月支 | 月支→{值, 是否为干} 查表（值可为干或支） |

> 天乙贵人标准映射（以值为准，移植时核对源表）：甲戊庚→{丑,未}；乙己→{子,申}；丙丁→{亥,酉}；壬癸→{卯,巳}；辛→{寅,午}。

**重构接线**：`MarriageCalculator.findShenSha` 删 `taoHuaOf`，桃花取自 `ShenShaCalculator.analyze(bazi).items.filter { it.name == "桃花" }`；`StudyCalculator` 文昌/学堂同理。

**UI**：`ShenShaSection`（标签 chips：名 + 柱位），置于原局卡内纳音行下方。

### 2.2 旬空/空亡
**新增文件**：`core/KongWangCalculator.kt`、`model/KongWang.kt`、`KongWangCalculatorTest.kt`。

**主入口**：
```kotlin
object KongWangCalculator {
    fun xunKongBranches(dayCycleIndex: Int): Pair<EarthlyBranch, EarthlyBranch>
    fun analyze(bazi: BaziResult): KongWangResult
}
data class KongWangMark(val pillarIdx: Int, val branch: EarthlyBranch)
data class KongWangResult(val marks: List<KongWangMark>) {
    fun isKong(pillarIdx: Int): Boolean
}
```

**算法（以日柱 cycleIndex 为基准）**：
- 旬首 `headCycle = dayCycleIndex - floorMod(dayCycleIndex, 10)`
- 旬首地支 `headBranch = headCycle % 12`
- 空亡两支 = `floorMod(headBranch - 2, 12)` 与 `floorMod(headBranch - 1, 12)`
- 四柱中支落此两支者标记（含日柱本身）。

**UI**：`PillarCard` 增形参 `kong: Boolean`，地支 `CharBox` 叠加"空"字（半透明角标）；`BaziDisplay` 算一次 `KongWangCalculator.analyze(result)`，按 `pillarIdx` 逐柱传入 `kong`。

## 3. Tier 2 — 调候（TiaoHou）

**前置**：移植 `detectCongGe(bazi): CongGeResult?`（从 xuanzong `geju.js`）。

**新增文件**：`core/TiaoHouCalculator.kt`、`model/TiaoHou.kt`、`ui/components/TiaoHouSection.kt`、`TiaoHouCalculatorTest.kt`。

**主入口**：
```kotlin
object TiaoHouCalculator {
    fun analyze(bazi: BaziResult): TiaoHouResult
    fun isJinShuiShangGuan(dayMaster: HeavenlyStem, monthBranch: EarthlyBranch): Boolean
}
```

**结果模型**：
```kotlin
enum class Climate { HAN, ZAO, WEN_HE }            // 寒 / 燥 / 温和
data class TiaoHouFormula(val key: String, val name: String, val desc: String, val guanStance: String) // 官星立场: 喜/忌/无碍/—
data class TiaoHouResult(
    val climate: Climate,
    val climateBasis: String,
    val tiaoHouElement: Element?,        // 寒→FIRE, 燥→WATER, 温和→null
    val tiaoHouPresent: Boolean,
    val tiaoHouNote: String,
    val urgent: Boolean,                 // 极寒/燥且无解且非从格
    val urgentDesc: String?,
    val congGe: CongGeResult?,
    val formulas: List<TiaoHouFormula>,
    val deShi: List<String>,
)
```

**算法**：
1. 月支→季节：冬(亥子丑)→寒；夏(巳午未)→燥；春(寅卯辰)/秋(申酉戌)→温和。
2. 扫火(丙丁/巳午未)、水(壬癸/子亥)是否在干支。
3. 寒→需火，燥→需水；温和→null。
4. urgent：气候为寒/燥 **且** 所需五行缺席 **且** 非从格 → "寒极无火"/"燥极无水"。
5. 4 句口诀：金水伤官（金日冬月）/木火伤官（木日夏月）/木火通明（木日春且见火）/金水相涵（金日秋且见水）。
6. deShi：季节性得失注。

**依赖**：`GeJuCalculator.detectCongGe`、`elementControls`/`elementGenerates`。

**UI**：置于用神相神板块之后；展示气候、调候五行、是否为急、口诀表（含官星立场）、得失注。

## 4. Tier 3 — 取运 + 会合解冲

### 4.1 取运（QuYun，行运类）
**新增文件**：`core/QuYunCalculator.kt`、`model/QuYun.kt`、`QuYunCalculatorTest.kt`；UI 接入 `selection` 选中区块。

**主入口**：
```kotlin
object QuYunCalculator {
    fun analyze(bazi: BaziResult, yongShen: YongShenResult, yunPillar: Pillar): QuYunResult
    private fun identifyZiPeiZhi(bazi: BaziResult, yongShen: YongShenResult): ZiPeiZhi
}
```

**结果模型**：
```kotlin
enum class QuYunVerdict { XI, JI, PING, JI_ZHONG_XI, XI_ZHONG_PING }
data class ZiPeiZhi(val family: String?, val familyCh: String, val subKey: String, val subName: String, val note: String)
data class PillarVerdict(val godCh: String, val verdict: QuYunVerdict, val reason: String)   // 干/支各一
data class QuYunRow(val name: String, val xiText: String, val jiText: String, val note: String)
data class QuYunResult(
    val ziPeiZhi: ZiPeiZhi,
    val gan: PillarVerdict,
    val zhi: PillarVerdict,
    val summary: QuYunSummary,   // verdict + text（喜运/忌运/干支喜忌参半/一喜一平/平运）
    val row: QuYunRow?,
)
```

**算法**：
- `QUYUN_TABLE`：8 格局族（正官/财/印绶/食神/七杀/伤官/阳刃/建禄月劫）× 各 4–9 子配置，约 50 行；每行 `{name, xi[tokens], ji[tokens], xiText, jiText, note}`。
- token 类别：`BI_JIE`(比劫)/`SHI_SHANG`(食伤)/`CAI`(财)/`GUAN`(正官)/`SHA`(七杀)/`GUAN_SHA`(官杀)/`YIN`(印)/`SHEN_WANG`(身旺=比劫或印)。
- `identifyZiPeiZhi`：由 `yongShen.geJuName` + 透干扫描（年/月/时，跳日干 `TOU_GAN_IDX=[0,1,3]`）定族与子配置。
- 干、支各取十神 → 按 token 归类 → 命中 `xi`/`ji` → XI/JI/PING + 表中理由。
- summary 合成：双 XI→XI；双 JI→JI；一 JI 一 XI→JI_ZHONG_XI；一 XI 一 PING→XI_ZHONG_PING；余→PING。

**依赖**：`TiaoHouCalculator.isJinShuiShangGuan`（伤官特例）→ 故 Tier 3 在 Tier 2 后。

**UI**：`selection` 选中后渲染（与 `YunRelationSection` 并列）：子配置名 + 选中柱干（十神+喜/忌/平+理由）、支（同）+ 总评。

### 4.2 会合解冲（HuiHeJieFa）
**新增文件**：`core/JieChongCalculator.kt`、`model/JieChong.kt`、`ui/components/JieChongSection.kt`、`JieChongCalculatorTest.kt`。

**主入口**：
```kotlin
object JieChongCalculator {
    fun analyze(bazi: BaziResult): JieChongResult
    fun monthBranchUnresolvedChong(bazi: BaziResult): Boolean
}
```

**结果模型**：
```kotlin
data class ChongJie(val branch1: EarthlyBranch, val branch2: EarthlyBranch, val description: String,
                    val resolved: Boolean, val resolveBy: String?, val note: String)
data class XingJie(val description: String, val kind: String, val involved: String,
                   val resolved: Boolean, val resolveBy: String?, val note: String)
data class JieChongResult(
    val chong: List<ChongJie>, val xing: List<XingJie>,
    val hasUnresolvedChong: Boolean, val hasUnresolvedXing: Boolean,
    val liuHeCount: Int, val sanHeCount: Int,
)
```

**算法**：取 `DiZhiRelationCalculator.analyze(bazi)`（冲/刑/三合）+ `HeHuaCalculator.analyze(bazi)`（六合）。建 `liuHeOf`(支→六合伴)、`sanHeSet`(入三合支集)。冲：任一支有六合伴或入三合→解；刑：任一涉支被合→解。

**UI**：独立板块，置于地支关系之后；标注各冲/刑是否被合解、被何所解；无冲刑则隐藏。

## 5. Tier 4 — 五行流通/战局/混杂（试验）

**新增文件**：`core/WuXingFlowCalculator.kt`、`model/WuXingFlow.kt`、`ui/components/WuXingFlowSection.kt`、`WuXingFlowCalculatorTest.kt`。

**主入口**：
```kotlin
object WuXingFlowCalculator {
    fun analyze(bazi: BaziResult, extraPillars: List<Pillar> = emptyList()): WuXingFlowResult
}
```

**结果模型**：
```kotlin
enum class FlowType { FAVOR, UNFAVOR }
data class FlowItem(val type: FlowType, val id: String, val text: String)
data class WuXingFlowResult(
    val duanYu: List<FlowItem>,
    val chain: Int,                 // 最长相生链长 0–5
    val elems: IntArray,            // [木,火,土,金,水] 计数
    val war: WarFlags,              // jinMu / shuiHuo
    val favorable: Boolean,
)
data class WarFlags(val jinMu: Boolean, val shuiHuo: Boolean)
```

**算法**：
- 五行计数：四柱干+支（+ extraPillars）。
- 最长相生链：木→火→土→金→水（可环绕 水→木），count≥min(默认 2) 才计入；返回长度与中文链。
- 战局：取 `DiZhiRelationCalculator` 冲 list，支五行构成 金+木 或 火+水 → war。
- 格局匹配：木火通明（日木 + 火≥3）/金水相涵（日金 + 水≥3 + 火≥1）/火土成势（火≥2 土≥2 合≥5 余≤1）/五行流通（链≥3 + favorable）。
- favorable：日主身强 或 印≥2。

**UI**：标题"运岁五行（试验）"；FAVOR/UNFAVOR 色片 + 链 + 五行计数 + 战局标记；`extraPillars` 取自 `selection`。

## 6. 构建与测序

- 每 Tier 一个垂直切片：model+calculator+test（先转绿）→ UI 接线 → 小步提交。
- 顺序 **Tier 1 → 2 → 3 → 4**（Tier 3 取运依赖 Tier 2 调候，不可前移）。
- 每切片完成跑全量 `./gradlew test` 保持绿；沿用仓库小步 TDD 提交风格（见太乙 commit 史：feat→test 锚点）。
- 提交信息前缀：`feat(八字):` / `test(八字):` / `refactor(八字):`。

## 7. 范围外（本次不做）
- 胎元/命宫/身宫（两工程皆无）。
- 农历输入（xuanzong 支持，bazia 仅公历）——输入侧改造，与本次分析层移植无关，留待后续。
- `ganHe.js`（天干五合有效性/争合）——xuanzong 自身未接入主流程（仅 yongshen/marriage 注释提及），本次不移植；如后续 yongshen 增强再议。

# 太乙神数 排盘 + 分析 — 设计规格

> 日期：2026-07-25。状态：待评审。
> 算法依据：`.taiyi-reference.md`（已验证·五源交叉裁定 + 用户权威讲解确认）。
> 约定对齐：镜像现有 qimen（九宫+格局+逐宫分析）与 liuren（model/core/ui/test 拆分 + `.xxx-reference.md` + TDD）。

## 1. 目标与范围
- 为 App 增「太乙神数」排盘与分析，与八字/奇门/六壬 并列。
- **四计共用一盘**（岁/月/日/时，`TaiyiCountType`）。主入口默认**时计**（日用百事，与奇门/六壬同事件级定位、复用 datetime 输入流），UI 提供「时/年/月/日」计法切换。阴阳遁：岁/月/日恒阳遁；时计冬至后阳遁、夏至后阴遁。
- **v1 必做(P0)**：太乙落宫 → 文昌 → 计神 → 始击 → 主客算 → 主客大小将 → 八门(太乙盘) → 格局 → 数分类 → 主客断语。
- **v1 应做(P1)**：定计大将/参将（合神加太岁法）、太岁/太阴（辅助坐标）。
- **v1 不做**（参考文档 §16 标 TODO/非正传）：阴遁文昌起例（先阳遁上线）、蒙守乘凌废、运卦/变卦、卷四基星体系。

## 2. 架构（新增文件，沿用现有分层）
```
model/Taiyi.kt              数据模型 + 枚举（TaiyiPalace/SixteenGod/八门将门/Plate/Analysis）
core/TaiyiCalculator.kt     object，build(input,countType)->TaiyiPlate（纯计算）
ui/components/TaiyiGrid.kt  九宫盘（TaiyiPalace.gridOrder，分层 cell）
ui/components/Taiyi*Section.kt  局信息/主客算/八门/格局/断语 区块
ui/TaiyiPage.kt             页面（onBack 回调，同 QimenPage/LiurenPage）
test/.../TaiyiCalculatorTest.kt  TDD（4 个已验证实盘）
```
导航：`BaziApp.Screen` 加 `Taiyi`，`BaziPage` 加入口按钮 `onOpenTaiyi`（同 onOpenQimen/onOpenLiuren）。

## 3. 关键数据结构（model/Taiyi.kt）
- `TaiyiPalace(enum)`：太乙九宫 `1乾 2离 3艮 4震 6兑 7坤 8坎 9巽`（**差一宫，独立于 QiPalace，不复用**）。字段 `no/trigram/element/direction/region/yang`。companion：`ring=[1,2,3,4,6,7,8,9]`、`gridOrder=[[9,2,7],[4,5,6],[3,8,1]]`、`opposite(no)`（对宫：1↔9,8↔2,3↔7,4↔6）、`isInner(no)`（地内{1,3,4,8}/天外{2,6,7,9}）。
- `SixteenGod(enum)`：idx0..15（子地主…亥大义），字段 `position/name/isZheng/palaceNo?`。companion：`clockwiseNext`、`governingPalaceNo`（间辰→顺时针下一正宫宫数，格局用）。
- `TaiyiCountType(enum)`：YEAR/MONTH/DAY/HOUR。
- `TaiyiDun(enum)`：YANG/YIN。
- `TaiyiMen(enum)`：开休生伤杜景死惊 + `trigram/element/auspicious`（三吉=开休生）。
- `TaiyiJu(data)`：`countType/dun/suanYear(积年或积时)/jiShi(入纪实)/juNo/taiyiPalaceNo/inGongOffset/liTianDiRen(理天/地/人)`。
- `TaiyiPattern(data)`：`name/auspicious/basis/duanYu`。
- `TaiyiPalaceCell(data)`：`palace + occupants(Set<将/门标记>) + tags`（显示用，类比 qimen PalaceCell）。
- `TaiyiPlate(data)`：`ju + taiyiPalaceNo + wenChang/shiJi(SixteenGod) + jiShen(EarthlyBranch) + zhuSuan/keSuan + zhu/keDaJiang/CanJiang(宫号) + dingJi*(P1,可空) + taiSui/taiYin + zhiShiMen + menOnTaiyi(Map<宫号,TaiyiMen>) + patterns + suanClass(数分类)`。
- 分析复用 qigen 模式：`TaiyiTone{GOOD,BAD,NEUTRAL}`、`TaiyiAnalysisItem(label,text,tone)`、`TaiyiPalaceAnalysis`。

## 4. 计算器（core/TaiyiCalculator.kt）
`object TaiyiCalculator`：
- 入口 `build(input: TaiyiInput, type: TaiyiCountType): TaiyiPlate`。
- 步骤（对应参考文档 §2-§13）：
  1. `computeSuan(input,type)`：年计 `积年=10153917+公元年`；时计 `时实=(距冬至积日-1)×12+时辰序`，`入纪实=时实 mod 360`（冬至/夏至复用 `SolarTermTable`/`SolarTerm`；四柱复用 `BaziCalculator`）。
  2. `decideDun`：岁/月/日→YANG；时计→比冬至/夏至。
  3. `placeTaiyi`：`k=(ju-1)div3 mod8`；阳 `[1,2,3,4,6,7,8,9][k]`，阴 `[9,8,7,6,4,3,2,1][k]`；`offset=(ju-1)mod3`→理天/地/人。
  4. `placeWenChang`：阳遁起武德顺十六神、遇阴德(乾)/大武(坤)重留、18算周期（阴遁 TODO，先阳遁规则）。
  5. `placeJiShen`：寅起逆行十二辰。
  6. `placeShiJi`：`distance=(2-计神idx+16)mod16`；`始击idx=(文昌idx+distance)mod16`。
  7. `computeZhuKeSuan`：目→太乙(不含)正神宫数累加，间辰起点+1（已双例验证：主算16/38，客算25）。
  8. `computeDaCanJiang`：大将=算mod10(0→mod9)；参将=大将×3 mod10。客同法。
  9. `placeBaMen`：直使门（年计 `[开,休,生,伤,杜,景,死,惊][(积年mod240)div30]`；时计张良法）飞加太乙宫、顺时针顺布八门。
  10. `computePatterns`：掩/击/迫/囚/关/格/对/四郭固/四郭杜/提挟/执提 + 主客相得/门宫相和相克/文武同道。
  11. `classifySuan`：和数/长短/重杂/三才/不和/杜塞/宫数易和。
- 全用 `floorMod` 处理模运算（同 qimen 风格）。每处 `TODO`/取舍在代码注释 + UI 注明。

## 5. UI（ui/TaiyiPage.kt + components）
- `TaiyiGrid`：九宫盘，`TaiyiPalace.gridOrder`，每宫分层显示太乙/文昌/始击/大小将/门标记，描边色编码（太乙临宫=主色、主大将=红、客大将=蓝，类比 qimen 符/使）。中宫显示寄宫说明。
- 页面区块：①局信息（计法切换/遁/积年·入局/太乙宫·理天地人）②主客算+大小将+数分类（tone 着色）③八门（直使+落宫）④格局列表（吉绿/凶红）⑤主客断语（地内助主/天外助客）。
- 计法切换：默认时计；切年/月/日计重新排盘（共用 build，仅 type 不同）。

## 6. 测试（TDD，test/.../TaiyiCalculatorTest.kt）
先写测试再实现，断言用参考文档 §15 实盘：
- **724AD**：太乙1宫乾/理天。
- **金镜卷二原例**：太乙9宫巽、文昌亥(间)→主算16；卷一时计例（太乙9宫、天目大武、计神午、始击临卯、客算4→客大将4/参将2）。
- **2024甲辰**：积年10155941→阳遁53局→太乙2宫离/理地；文昌坤、计神戌、始击亥；主算38→大将8/参将4；客算25→大将5(中宫)/参将5；年计八门=伤门。
- **武备志1570庚午**（全盘）：太乙3宫艮/理天；文昌大炅(巽)、计神申、始击阴主(戌)；主算33→大将3/参将9；客算10→大将1/参将3。
- 八门锚年：1924开/1954休/1984生/2014伤/2164开。

## 7. 构建顺序（实现计划将细化）
1. model/Taiyi.kt（枚举+数据类）
2. core/TaiyiCalculator.kt + TaiyiCalculatorTest.kt（TDD，逐步骤）
3. TaiyiGrid + 各 Section 组件
4. TaiyiPage 组装
5. BaziApp 导航 + BaziPage 入口接入
6. 真机运行验证（参考 build-environment / device-driving 记忆）

## 8. 风险与取舍（已在参考文档固化）
- 太乙数宫数独立（不复用 QiPalace）——已确认（用户地盘图验证）。
- 大将 `mod10(0→mod9)`、参将 `×3 mod10`——修正参考文档早期 ÷3 讹传（武备志算例验证）。
- 阴遁文昌起例 OCR 残缺——先阳遁上线，UI 注明 TODO。
- 1宫地内归属、四间辰取哪一辰、无地/无人定义、次和36/32——按推荐值落地 + UI 注明。

# 太乙神数 · 日计入局 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现太乙四计之「日计」入局——`TaiyiCalculator.build(dt, DAY)` 排出完整日计盘（八将 + 卷四星宿），以 2026-07-26=阳遁50局 为相位锚。

**Architecture:** 日计与年计同法（积日 mod 360/72/24÷3），入局用 `suanBase = epochDay + 53`（等价原文农历差法，无需建农历层）。卷四星宿按群用不同积日锚（四神组/大游用 `suanBase`，三基组用 `suanBase+24`，五福用 `suanBase+190`），均六日实盘验证。八门原文阙，`zhiShiMen` 改可空。

**Tech Stack:** Kotlin、Jetpack Compose、JUnit4（既有 `TaiyiCalculatorTest`）。

## Global Constraints

- 四计共用 `TaiyiPlate`/`TaiyiCountType`，差异收口到 `computeJu` 入局与星宿锚两处。
- 日计**恒阳遁**；太岁=日支（`taiSuiBranch` 已支持 `DAY`）。
- 星宿锚（六日钉死）：四神组(四神/天乙/地乙/直符)+民基+大游 = `suanBase`；三基组(臣基/君基) = `suanBase+24`；五福 = `suanBase+190`；小游 = 太乙宫。
- 飞符（卷五无）、阳九百六（年灾厄）日计不排；八门日计不排（`zhiShiMen=null`）。
- `C = 53` 锚定使 `floorMod(epochDay(2026-07-26)+53, 72)=49 → juNo=50`。
- spec：`docs/superpowers/specs/2026-07-26-taiyi-riji-design.md`。

---

## File Structure

- **Modify** `app/src/main/java/cn/xulun/bazi/model/Taiyi.kt`：`TaiyiPlate.zhiShiMen` 改可空 `TaiyiMen?`。
- **Modify** `app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt`：
  - `computeJu` DAY 分支；`computeZhiShiMen` 返回 `TaiyiMen?`（DAY→null）；`computeMenOnTaiyi` 接受可空；`computePatterns` 参数可空（`==` 天然 null-safe）；`computeJiXing` 加 `suanBase/type`，日计分支按群锚；`build` 透传。
- **Modify** `app/src/main/java/cn/xulun/bazi/ui/TaiyiPage.kt`：计法切换加 `DAY`；`JuHeader` 直使行 null 守卫；提示文案更新。
- **Modify** `app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt`：六日锚点。

---

### Task 1: 日计入局 + 八门可空

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/model/Taiyi.kt:173`（`zhiShiMen` 字段）
- Modify: `app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt:97-115,133,383-409`（`computeZhiShiMen`/`computeMenOnTaiyi`/`computePatterns`/`computeJu`）；`build:65-69` 透传
- Modify: `app/src/main/java/cn/xulun/bazi/ui/TaiyiPage.kt:195`（直使 null 守卫）
- Test: `app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt`

**Interfaces:**
- Produces: `TaiyiCalculator.build(dt, TaiyiCountType.DAY)` 可用；`TaiyiPlate.zhiShiMen: TaiyiMen?`。

- [ ] **Step 1: 写失败测试（日计入局 + 八门将空）**

在 `TaiyiCalculatorTest.kt` 末尾追加：

```kotlin
    // ── 日计入局（P-日计）──
    @Test
    fun `日计 2026-07-26 阳遁50局 太乙1宫乾 理地 直使空`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2026, 7, 26, 12, 0), TaiyiCountType.DAY)
        assertEquals(TaiyiDun.YANG, p.ju.dun)
        assertEquals(50, p.ju.juNo)
        assertEquals(1, p.ju.taiyiPalaceNo)
        assertEquals("理地", p.ju.liTianDiRen)
        assertEquals("丑", p.taiSui.chinese)   // 辛丑日
        assertEquals("子", p.heShen.chinese)
        assertEquals("丑", p.jiShen.chinese)
        assertEquals("巳", p.wenChang.position)   // 大神
        assertEquals("午", p.shiJi.position)       // 大威
        assertEquals(16, p.zhuSuan)
        assertEquals(15, p.keSuan)
        assertEquals(6, p.zhuDaJiang); assertEquals(8, p.zhuCanJiang)
        assertEquals(5, p.keDaJiang); assertEquals(5, p.keCanJiang)  // 客大将中宫=杜塞
        assertNull(p.zhiShiMen)   // 日计八门原文阙
        assertTrue(p.menOnTaiyi.isEmpty())
    }

    @Test
    fun `日计 2026-07-28 太乙行宫至2宫离 52局`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2026, 7, 28, 12, 0), TaiyiCountType.DAY)
        assertEquals(52, p.ju.juNo)
        assertEquals(2, p.ju.taiyiPalaceNo)   // 1宫→2宫，3日一移
        assertEquals("理天", p.ju.liTianDiRen)
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest.日计 2026-07-26 阳遁50局 太乙1宫乾 理地 直使空"`
Expected: FAIL（`computeJu` 的 `DAY` 分支抛 `TODO`/`NotImplementedError`）。

- [ ] **Step 3: `computeJu` DAY 分支**

`TaiyiCalculator.kt` `computeJu` 的 `when(type)` 中，把 `MONTH, DAY -> TODO(...)` 拆开：

```kotlin
            TaiyiCountType.DAY ->
                TaiyiDun.YANG to (dt.toLocalDate().toEpochDay() + 53L)   // 锚 2026-07-26=50局；等价原文积日
            TaiyiCountType.MONTH ->
                TODO("月计入局待实现")
```

- [ ] **Step 4: `zhiShiMen` 改可空（模型 + 三处计算器方法）**

(a) `model/Taiyi.kt` `TaiyiPlate.zhiShiMen` 字段：
```kotlin
    val zhiShiMen: TaiyiMen?,             // 直使门；日计原文阙→null
```

(b) `TaiyiCalculator.computeZhiShiMen` 返回类型 `TaiyiMen?`，DAY 直接返回 null：
```kotlin
    private fun computeZhiShiMen(suanBase: Long, dun: TaiyiDun, type: TaiyiCountType): TaiyiMen? {
        if (type == TaiyiCountType.DAY) return null        // 卷一未载日计八门
        if (type == TaiyiCountType.YEAR) {
            val idx = floorMod(suanBase + 1, 240).toInt() / 30
            return TaiyiMen.order[idx]
        }
        val menShu = floorMod(suanBase, 120).toInt() / 30
        return (if (dun == TaiyiDun.YANG) TaiyiMen.poolYang else TaiyiMen.poolYin)[menShu]
    }
```

(c) `computeMenOnTaiyi` 接受可空、null 返回空 map：
```kotlin
    private fun computeMenOnTaiyi(zhiShi: TaiyiMen?, taiyiNo: Int): Map<Int, TaiyiMen> {
        if (zhiShi == null) return emptyMap()
        val startDoor = TaiyiMen.order.indexOf(zhiShi)
        val startGong = TaiyiPalace.ring.indexOf(taiyiNo)
        val map = HashMap<Int, TaiyiMen>()
        for (i in 0 until 8) {
            map[TaiyiPalace.ring[(startGong + i) % 8]] = TaiyiMen.order[(startDoor + i) % 8]
        }
        return map
    }
```

(d) `computePatterns` 形参 `zhiShiMen: TaiyiMen` → `zhiShiMen: TaiyiMen?`（签名改即可；体内 `zhiShiMen == TaiyiMen.KAI` 等比较 Kotlin 天然 null-safe，无需改逻辑）。

- [ ] **Step 5: `build` 透传（签名已兼容，确认无误）**

`build` 中 `zhiShiMen`/`menOnTaiyi`/`patterns` 三行无需改类型（`computeZhiShiMen` 现返回 `TaiyiMen?`，`computeMenOnTaiyi`/`computePatterns` 已接受可空）。确认编译通过。

- [ ] **Step 6: `TaiyiPage.JuHeader` 直使行 null 守卫**

`TaiyiPage.kt:195` 改为（抽局部变量）：

```kotlin
            val zhiShiText = p.zhiShiMen?.let { "直使${it.chinese}门" } ?: "日计八门阙载（卷一未载）"
            Text(
                "太岁${p.taiSui.chinese}　合神${p.heShen.chinese}　计神${p.jiShen.chinese}　$zhiShiText",
                fontSize = 12.sp, color = SeedRed.copy(alpha = 0.85f), modifier = Modifier.padding(top = 2.dp),
            )
```

- [ ] **Step 7: 运行测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest.日计 2026-07-26 阳遁50局 太乙1宫乾 理地 直使空" --tests "cn.xulun.bazi.TaiyiCalculatorTest.日计 2026-07-28 太乙行宫至2宫离 52局"`
Expected: PASS。

- [ ] **Step 8: 全量回归（年计/时计不受影响）**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全绿（既有测试 + 两个新日计测试）。`zhiShiMen` 改可空后，年计/时计仍非空，既有断言（如 `p.zhiShiMen` 相关）不受影响——若某处用 `!!` 或 `.chinese` 直接访问，编译期会报错，按提示补 null 守卫。

- [ ] **Step 9: 提交**

```bash
git add app/src/main/java/cn/xulun/bazi/model/Taiyi.kt app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt app/src/main/java/cn/xulun/bazi/ui/TaiyiPage.kt app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt
git commit -m "feat(太乙): 日计入局(suanBase=epochDay+53锚50局)+八门改可空(日计原文阙)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: 日计卷四星宿（分群锚定）

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt:196-283`（`computeJiXing`）；`build:69`（透传 `suanBase/type`）
- Test: `app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt`

**Interfaces:**
- Consumes: Task 1 的 `ju.suanBase`、`type`。
- Produces: `TaiyiPlate.jiXing` 日计下列出全部星宿（四神/天乙/地乙/直符/三基/大游/五福/小游；无阳九百六、无飞符）。

- [ ] **Step 1: 写失败测试（2026-07-26 全星宿）**

在 `TaiyiCalculatorTest.kt` 追加：

```kotlin
    @Test
    fun `日计 2026-07-26 星宿 民基亥 臣基戌 君基巳 地乙乾 五福中 小游乾`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2026, 7, 26, 12, 0), TaiyiCountType.DAY)
        val jx = p.jiXing.associateBy { it.name }
        // 三基组（suanBase+24，起戌）
        assertEquals("亥", jx["民基"]?.location)
        assertEquals("戌", jx["臣基"]?.location)
        assertEquals("巳", jx["君基"]?.location)
        // 四神组（suanBase）
        assertEquals("中5宫", jx["四神"]?.location)
        assertEquals("绛宫", jx["天乙"]?.location)
        assertEquals("乾1宫", jx["地乙"]?.location)
        assertEquals("巽9宫", jx["直符"]?.location)
        // 大游（suanBase）
        assertEquals("兑6宫", jx["大游"]?.location)
        // 五福（suanBase+190）
        assertEquals("中", jx["五福"]?.location)
        // 小游=太乙宫
        assertEquals("乾1宫", jx["小游"]?.location)
        // 日计不排阳九百六/飞符
        assertNull(jx["阳九"])
        assertNull(jx["百六"])
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest.日计 2026-07-26 星宿 民基亥 臣基戌 君基巳 地乙乾 五福中 小游乾"`
Expected: FAIL（日计星宿仍按 `year` 算，位置不符；阳九百六仍存在）。

- [ ] **Step 3: `computeJiXing` 加 `suanBase/type` 并按群分支**

改签名（`TaiyiCalculator.kt:196`）：
```kotlin
    private fun computeJiXing(
        year: Int, suanBase: Long, type: TaiyiCountType,
        taiyiPalaceNo: Int, inGongOffset: Int, taiSui: EarthlyBranch,
    ): List<TaiyiJiXing> {
```

方法体开头插入日计判定与各群 base（替换原 `ruZhou`/`jiNian1` 等单一来源）：
```kotlin
        val out = mutableListOf<TaiyiJiXing>()
        val isDay = type == TaiyiCountType.DAY
        val unit = if (isDay) "日" else "年"   // 周期文案单位

        // 三基：年计 ruZhou=(year-113)mod360；日计 ruZhou=(suanBase+24)mod360（起戌邦，六日钉死）
        val ruZhou = if (isDay) floorMod(suanBase + 24, 360) else floorMod(year - 113L, 360)
        // 四神等：年计 step=(year+10153916)mod36÷3；日计 step=suanBase mod36÷3
        val step = (if (isDay) floorMod(suanBase, 36) else floorMod(year + 10153916L, 36)).toInt() / 3
        val inFsYear = (if (isDay) floorMod(suanBase, 3) else floorMod(year + 10153916L, 3)).toInt() + 1
        val yuanJi = (if (isDay) floorMod(suanBase, 180) else floorMod(year + 10153916L, 180)).toInt()
```

把既有 `ruZhou = floorMod(year - 113, 360)` 行删除（已上移）。君基/臣基/民基的 `period` 文案中的 `年` 改用 `$unit`（如 `"360$unit/30$unit一邦"`），`入宫年/日`同理用 `$unit`。

五福与大游按 `isDay` 切换 base：
```kotlin
        // 五福：年计 (year+12607)mod225；日计 (suanBase+190)mod225（C5=190，12-30/31跨宫钉死）
        val wuFuRu = (if (isDay) floorMod(suanBase + 190, 225) else floorMod(year + 12607L, 225))
            .let { if (it == 0) 225 else it }
        // 大游：年计 (year-353)mod4320%288；日计 suanBase mod288
        val daYouXiao = if (isDay) floorMod(suanBase, 288) else (floorMod(year - 353L, 4320) % 288)
```
删除原 `val wuFuRu = floorMod(year + 12607, 225)...` 与 `val daYouXiao = floorMod(year - 353, 4320) % 288` 两行。

阳九百六段（卷七，`u = year + 12606`）整段用 `if (!isDay) { ... }` 包裹（日计不排）。

- [ ] **Step 4: `build` 透传 `suanBase/type`**

`build:69`：
```kotlin
        val jiXing = computeJiXing(dt.year, ju.suanBase, type, ju.taiyiPalaceNo, ju.inGongOffset, taiSui)
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest.日计 2026-07-26 星宿 民基亥 臣基戌 君基巳 地乙乾 五福中 小游乾"`
Expected: PASS。

- [ ] **Step 6: 年计星宿回归（确保未破坏）**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiModelTest" --tests "cn.xulun.bazi.TaiyiCalculatorTest"`
Expected: 全绿（年计星宿用 `year` 路径，结果不变）。

- [ ] **Step 7: 提交**

```bash
git add app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt
git commit -m "feat(太乙): 日计卷四星宿分群锚定(四神组suanBase/三基+24/大游suanBase/五福+190),跳过阳九百六

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: UI 计法切换加「日计」

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/ui/TaiyiPage.kt:124-135`（计法 InputCard）

**Interfaces:**
- Produces: 计法切换出现 `日计` 选项；提示文案不再写「月/日计待实现」。

- [ ] **Step 1: 计法 FilterChip 列表加 `DAY`**

`TaiyiPage.kt:126`：
```kotlin
                    listOf(TaiyiCountType.HOUR, TaiyiCountType.DAY, TaiyiCountType.YEAR).forEach { t ->
```

- [ ] **Step 2: 更新提示文案**

`TaiyiPage.kt:134`：
```kotlin
                Text("时计主日用百事；日计主日内选择/出行；年计主岁中大事。月计待实现", fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 6.dp))
```

- [ ] **Step 3: 手动验证（无自动化 UI 测试）**

在 `TaiyiPage` 选中「日计」、选 2026-07-26、起课排盘，确认：标题 `阳遁50局 · 日计 · 太乙乾1宫(西北) · 理地`；直使行显「日计八门阙载（卷一未载）」；星宿区出现民基亥/地乙乾1宫/五福中等；八门将区不渲染门（`menOnTaiyi` 空，grid 无门标）。确认无崩溃。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/cn/xulun/bazi/ui/TaiyiPage.kt
git commit -m "feat(太乙): 计法切换加日计选项+提示文案

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: 六日回归测试锚点（跨宫钉死）

**Files:**
- Test: `app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt`

**Interfaces:**
- Consumes: Task 1/2 的日计实现。
- Produces: 覆盖 07-27/08-18/10-29/12-30/12-31 的局/太乙宫/星宿跨宫断言。

- [ ] **Step 1: 追加邻日与跨宫锚点测试**

```kotlin
    @Test
    fun `日计 2026-07-27 51局 民基子 逐日推进`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2026, 7, 27, 12, 0), TaiyiCountType.DAY)
        assertEquals(51, p.ju.juNo)
        assertEquals(1, p.ju.taiyiPalaceNo)
        assertEquals("子", p.jiXing.associateBy { it.name }["民基"]?.location)
    }

    @Test
    fun `日计 2026-08-18 1局 大游大武7宫 五福乾`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2026, 8, 18, 12, 0), TaiyiCountType.DAY)
        assertEquals(1, p.ju.juNo)
        assertEquals(1, p.ju.taiyiPalaceNo)
        val jx = p.jiXing.associateBy { it.name }
        assertEquals("坤7宫", jx["大游"]?.location)
        assertEquals("乾", jx["五福"]?.location)
    }

    @Test
    fun `日计 2026-10-29 1局 大游大炅9宫 五福和德艮`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2026, 10, 29, 12, 0), TaiyiCountType.DAY)
        assertEquals(1, p.ju.juNo)
        val jx = p.jiXing.associateBy { it.name }
        assertEquals("巽9宫", jx["大游"]?.location)
        assertEquals("艮", jx["五福"]?.location)   // 和德=艮3宫
    }

    @Test
    fun `日计 2026-12-30/31 五福跨宫边界 C5=190`() {
        val p30 = TaiyiCalculator.build(LocalDateTime.of(2026, 12, 30, 12, 0), TaiyiCountType.DAY)
        val p31 = TaiyiCalculator.build(LocalDateTime.of(2026, 12, 31, 12, 0), TaiyiCountType.DAY)
        assertEquals(63, p30.ju.juNo)
        assertEquals(64, p31.ju.juNo)
        assertEquals("巽", p30.jiXing.associateBy { it.name }["五福"]?.location)   // 大炅，wuFuRu=135
        assertEquals("坤", p31.jiXing.associateBy { it.name }["五福"]?.location)   // 大武，wuFuRu=136
    }
```

- [ ] **Step 2: 运行全部日计测试**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest"`
Expected: 全绿（含 Task 1/2 的 07-26/28 与本任务的 07-27/08-18/10-29/12-30/31）。

- [ ] **Step 3: 全量回归**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全绿（116→122+ 测试，年计/时计/奇门/六壬/八字无回归）。

- [ ] **Step 4: 提交**

```bash
git add app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt
git commit -m "test(太乙): 日计六日回归锚点(邻日+大游五福跨宫钉死)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage：** §1 入局 → Task 1（computeJu DAY）；§3 八将 → Task 1 测试断言（复用既有逻辑）；§4 星宿分群 → Task 2；§5 八门不排 → Task 1（zhiShiMen 可空）；§6 显示 → Task 1 直使守卫 + Task 3 文案；§7 代码改动表逐项覆盖（computeJu/computeJiXing/build/computeZhiShiMen/computePatterns/TaiyiPlate/TaiyiPage/测试）；§8 六日锚点 → Task 1(07-26/28) + Task 2(07-26 星宿) + Task 4(07-27/08-18/10-29/12-30/31)。飞符/阳九百六不排 → Task 2（阳九百六 `if(!isDay)` 包裹、测试 assertNull）。✓ 无遗漏。

**Placeholder scan：** 无 TBD/TODO（除 `MONTH` 分支保留既有 `TODO("月计入局待实现")`，属另一功能范围）。步骤均含实际代码。✓

**Type consistency：** `zhiShiMen: TaiyiMen?` 在模型、`computeZhiShiMen`（返回 `TaiyiMen?`）、`computeMenOnTaiyi`/`computePatterns`（接受 `TaiyiMen?`）、UI（`?.let`）一致。`computeJiXing` 新签名 `(year, suanBase, type, taiyiPalaceNo, inGongOffset, taiSui)` 与 `build` 调用一致。✓

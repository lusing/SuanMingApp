# 太乙四神三元五纪立成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 四神/天乙/地乙/直符改按《太乙金镜式经》卷五「四神三元五纪立成」统一 12 位环公式，新增地乙，修正 P3 环序与相位两处偏差。

**Architecture:** 改动收口在 `TaiyiCalculator.computeJiXing` 尾部一块（fsSeq 模型删除，换统一公式）；`TaiyiJiXing` 模型与 UI 不动。依据 spec：`docs/superpowers/specs/2026-07-26-taiyi-sishen-licheng-design.md`。

**Tech Stack:** Kotlin / Android (Gradle)，JUnit4 单元测试。

## Global Constraints

- 测试命令：`./gradlew :app:testDebugUnitTest`（单类加 `--tests "cn.xulun.bazi.TaiyiCalculatorTest"`），工作目录 `G:\code\zhouyi\bazia`。
- 12 位环 `[1,2,3,4,5,6,7,8,9,绛,明,玉]`；offset：四神 0 / 天乙 5 / 地乙 8 / 直符 4。
- `step = floorMod(积年−1, 36) ÷ 3`；`入宫年 = floorMod(积年−1, 3) + 1`；积年 = `year + 10153917`（下同既有代码）。
- 元 = `floorMod(积年−1, 180) ÷ 60` → 上元/中元/下元；纪 = `÷ 36` → 甲子/庚子/丙子/壬子/戊子纪。
- 直符·中元列按统一公式（四库本立成表该列 20 格疑刻本讹，注释注明）。
- 三堂（绛宫/明堂/玉堂）palaceNo=null；落宫 location 格式 `"卦X宫"`（沿既有）。

---

### Task 1: 四神立成统一公式（测试 + 实现）

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt`（`computeJiXing` 尾部符神块，约 218-241 行；docstring 179-186 行中符神行）
- Test: `app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt`（234-280 行符神测试块）

**Interfaces:**
- Consumes: `TaiyiJiXing(name: String, location: String, palaceNo: Int?, cycle: String, inYear: Int, auspicious: Boolean, note: String)`（既有模型，不变）；`TaiyiPalace.of(no).trigram`；`java.lang.Math.floorMod`（文件已 import）。
- Produces: `p.jiXing` 列表含 `地乙`（10 神）；四神/天乙/直符位置按新公式；note 含 `"<占义>·<元>·<纪>纪·依卷五立成"`。

- [ ] **Step 1: Replace the failing tests**

`TaiyiCalculatorTest.kt` 234-280 行符神测试块（含 `── Task 8: 卷四符神/乙神 ……` 注释行起至文件尾 `1998 …` 测试）整体替换为：

```kotlin
    // ── 卷五符神/乙神：四神/天乙/地乙/直符（四神三元五纪立成：统一 12 位环 [1..9,绛,明,玉]，命起 1/6/9/5）──
    @Test
    fun `2024 卷五符神 四神6宫 天乙明堂 地乙2宫 直符绛宫 下元戊子纪`() {
        // 甲辰下元行(立成表)：六/明/二/絳；(积年−1) mod 3 = 1 → 入宫第2年
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = p.jiXing.associateBy { it.name }
        assertEquals("兑6宫", m["四神"]!!.location)
        assertEquals(6, m["四神"]!!.palaceNo)
        assertEquals("明堂", m["天乙"]!!.location)
        assertNull(m["天乙"]!!.palaceNo)
        assertEquals("离2宫", m["地乙"]!!.location)
        assertEquals(2, m["地乙"]!!.palaceNo)
        assertEquals("绛宫", m["直符"]!!.location)
        assertNull(m["直符"]!!.palaceNo)
        assertTrue(m["四神"]!!.note.contains("下元·戊子纪"))
        assertEquals(2, m["四神"]!!.inYear)
    }

    @Test
    fun `724AD 卷五符神 四神5宫 天乙绛宫 地乙1宫 直符9宫 入宫第1年`() {
        // 甲子下元行(立成表)：五/絳/一/九；甲子为三年组首年 → 入宫第1年
        val p = TaiyiCalculator.build(LocalDateTime.of(724, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = p.jiXing.associateBy { it.name }
        assertEquals("中5宫", m["四神"]!!.location)
        assertEquals(5, m["四神"]!!.palaceNo)
        assertEquals("绛宫", m["天乙"]!!.location)
        assertEquals("乾1宫", m["地乙"]!!.location)
        assertEquals("巽9宫", m["直符"]!!.location)
        // 四神同用入纪年→同 inYear
        val years = listOf("四神", "天乙", "地乙", "直符").map { m[it]!!.inYear }.toSet()
        assertEquals(setOf(1), years)
    }

    @Test
    fun `卷五十神齐备 飞符不列`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val names = p.jiXing.map { it.name }
        // P2 六星（君基/臣基/民基/五福/大游/小游）+ 卷五四神（四神/天乙/地乙/直符）＝十神；飞符为淘金歌概念，不列
        listOf("君基", "臣基", "民基", "五福", "大游", "小游", "四神", "天乙", "地乙", "直符").forEach {
            assertTrue("$it 缺失: $names", names.contains(it))
        }
        assertFalse("飞符为淘金歌概念不应出现", names.contains("飞符"))
    }

    @Test
    fun `1998 戊寅下元 四神9宫 修正积年相位`() {
        // 戊寅下元行(立成表)：九/二/五/一；step=(积年−1) mod 36 ÷3（P3 误用积年致绛宫，立成表证为9宫）
        val p = TaiyiCalculator.build(LocalDateTime.of(1998, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = p.jiXing.associateBy { it.name }
        assertEquals("巽9宫", m["四神"]!!.location)
        assertEquals(9, m["四神"]!!.palaceNo)
        assertEquals("离2宫", m["天乙"]!!.location)
        assertEquals("中5宫", m["地乙"]!!.location)
        assertEquals("乾1宫", m["直符"]!!.location)
    }

    @Test
    fun `2046 丙寅上元 四神1宫入宫第3年 相位锚`() {
        // 立成相位：甲子/乙丑/丙寅三年同宫（丙寅上元行 四神=一），丙寅为组内第3年
        val p = TaiyiCalculator.build(LocalDateTime.of(2046, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = p.jiXing.associateBy { it.name }
        assertEquals("乾1宫", m["四神"]!!.location)
        assertEquals(3, m["四神"]!!.inYear)
        assertTrue(m["四神"]!!.note.contains("上元·甲子纪"))
    }

    @Test
    fun `1960 庚子中元 直符1宫 立成表中元列刻本疑讹按推法公式`() {
        // 统一公式：step=8 → 直符 cycle[(4+8)%12]=1宫；四库本立成表直符中元列此格讹作「四」
        // 庚子中元行(立成表)四神/天乙/地乙：九/二/五 ✓
        val p = TaiyiCalculator.build(LocalDateTime.of(1960, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = p.jiXing.associateBy { it.name }
        assertEquals("乾1宫", m["直符"]!!.location)
        assertEquals("巽9宫", m["四神"]!!.location)
        assertEquals("离2宫", m["天乙"]!!.location)
        assertEquals("中5宫", m["地乙"]!!.location)
        assertTrue(m["直符"]!!.note.contains("中元·丙子纪"))
    }
}
```

（注意保留被替换块之后的类结束 `}`；原 280 行 `}` 为文件末行，替换内容末行自带 `}`。）

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest"`
Expected: FAIL——新断言与旧实现不符（如 2024 天乙旧值得"离2宫"而非"明堂"；`m["地乙"]` 为 null 抛 NPE）。

- [ ] **Step 3: Implement**

`TaiyiCalculator.kt` 中 `computeJiXing` 尾部——自注释行 `// ── 卷四符神/乙神（四神/天乙/直符；地乙/飞符卷四无独立推法，故不列）──` 起至 `val (loc3, pno3) = fsLoc(...)` 及其后 `out += TaiyiJiXing("直符", ...)` 整段（含 `ruJiNian`/`gongShu`/`inFsYear`/`sanTang`/`fsSeq`/`fsLoc` 定义与三个 `out +=`）——替换为：

```kotlin
        // ── 卷五符神/乙神：四神/天乙/地乙/直符（四神三元五纪立成，KR3g0047_005 逐格核定）──
        // 统一 12 位环 [1..9,绛,明,玉]，命起 四神1/天乙6/地乙9/直符5；step=(积年−1) mod 36 ÷3。
        // 立成表 180 格中 160 格与公式全合；直符·中元列 20 格四库本印作 1..9,1..9,绛,明
        // （与推法正文及其余 11 列块矛盾，疑刻本讹），按推法统一公式（用户裁定 2026-07-26）。
        val jiNian1 = year + 10153916L                       // 积年−1（立成相位：甲子/乙丑/丙寅三年同宫）
        val step = floorMod(jiNian1, 36).toInt() / 3         // 0..11
        val inFsYear = floorMod(jiNian1, 3).toInt() + 1      // 入宫第N年
        val yuanJi = floorMod(jiNian1, 180).toInt()          // 0..179
        val yuan = listOf("上元", "中元", "下元")[yuanJi / 60]
        val ji = listOf("甲子", "庚子", "丙子", "壬子", "戊子")[yuanJi / 36]
        val sanTang = listOf("绛宫", "明堂", "玉堂")
        fun fsLoc(offset: Int): Pair<String, Int?> {
            val pos = (offset + step) % 12                   // 0..8=宫1..9，9..11=绛/明/玉
            return if (pos < 9) "${TaiyiPalace.of(pos + 1).trigram}${pos + 1}宫" to (pos + 1)
            else sanTang[pos - 9] to null
        }
        fun fsGod(name: String, offset: Int, ausp: Boolean, yi: String): TaiyiJiXing {
            val (loc, pno) = fsLoc(offset)
            return TaiyiJiXing(name, loc, pno, "36年/3年一宫·九宫含中五+三堂12位", inFsYear, ausp,
                "$yi·$yuan·${ji}纪·依卷五立成")
        }
        out += fsGod("四神", 0, true, "水神纪纲有道则昌")
        out += fsGod("天乙", 5, false, "金神主兵革")
        out += fsGod("地乙", 8, false, "孤立无援(推地乙正文四库本佚,占义从通行)")
        out += fsGod("直符", 4, false, "火神值日巡察")
```

同时把 `computeJiXing` docstring（179-186 行）中符神行：

```
     * - 四神/天乙/直符（卷四 Image36/39/42）：演纪积年，宫数＝入纪年÷3，命起 1/6/5 宫顺尽九宫(含中五)＋绛宫/明堂/玉堂三堂＝12位。
     *   （卷四原文模型，非 §17/淘金歌「十二支特殊序」；地乙/飞符卷四无独立推法，不列。）
```

替换为：

```
     * - 四神/天乙/地乙/直符（卷五 四神三元五纪立成）：统一 12 位环[1..9,绛,明,玉]，命起 1/6/9/5，step=(积年−1) mod 36 ÷3。
     *   （立成表逐格核定；直符中元列刻本疑讹按推法公式；飞符为淘金歌概念，不列。）
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "cn.xulun.bazi.TaiyiCalculatorTest"`
Expected: PASS（30 个；其余既有测试不受影响）。

- [ ] **Step 5: Full regression**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全量 PASS。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/cn/xulun/bazi/core/TaiyiCalculator.kt app/src/test/java/cn/xulun/bazi/TaiyiCalculatorTest.kt
git commit -m "feat(太乙): 四神/天乙/地乙/直符改卷五四神三元五纪立成(统一12位环+新增地乙)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: 文档同步（参考文档 §17 + memory）

**Files:**
- Modify: `.taiyi-reference.md`（§17「四神/飞符/天乙/地乙/直符/同构归纳」六行，187-192 行）
- Modify: `G:\xulun\.claude\projects\G--code-zhouyi-bazia\memory\taiyi-feature.md`

**Interfaces:**
- Consumes: Task 1 已合入实现（公式、offset、注记格式）。
- Produces: 文档与代码口径一致；无代码接口。

- [ ] **Step 1: 改写 `.taiyi-reference.md` §17 符神段**

将 §17 中以下六行：

```
- **四神**（半吉半凶，多变莫测）：3年/宫，36年/周期，只走12支，**特殊序**[亥,午,寅,卯,辰,酉,申,子,巳,戌,未,丑]（非星非绕圈）。`r=积年mod36; 步=(r-1)div3; 四神支=SEQ4[步]`。2024 r17→步5→**酉** ✓。
- **飞符**（凶，虚实变化）：同四神跳法、起点不同，序[辰,酉,申,子,巳,戌,未,丑,亥,午,寅,卯]。`步=(r-1)div3`。2024→**戌** ✓。（飞符序=四神序轮转4位）
- **天乙**（金·凶，主战伐、定胜负）：3年/宫，36年/周期，**入中5**，序（起酉）[酉6,坤7,子8,巽9,巽9,坤7,乾1,乾1,午2,艮3,卯4,中5]。2024 步5→**坤7** ✓。
- **地乙**（凶，孤立无援）：起巽9，序[巽9,巽9,坤7,乾1,乾1,午2,艮3,卯4,中5,酉6,坤7,子8]。2024 步5→**午2** ✓。
- **直符**（火，值日巡察）：起中5，序[中5,酉6,坤7,子8,巽9,巽9,坤7,乾1,乾1,午2,艮3,卯4]。2024 步5→**巽9** ✓。
- 同构归纳：天乙/地乙/直符 = 36年/3年宫、入中5、9宫+3重复位凑12、唯起点异；四神/飞符 = 36年/3年宫、12支特殊序、唯起点异。`步=(积年mod36-1)div3` 通用。
```

替换为：

```
- **四神/天乙/地乙/直符**（卷五「四神三元五纪立成」，2026-07-26 逐格核定，淘金歌十二支特殊序通行版作废）：四神共用**统一 12 位环** `[1,2,3,4,5,6,7,8,9,绛,明,玉]`（九宫含中五+绛宫/明堂/玉堂三堂），唯起点异——四神起1（水神·半吉）/天乙起6（金神·凶·主兵革）/地乙起9（凶·孤立无援；推地乙正文四库本佚，起例自立成表）/直符起5（火神·凶·值日巡察）。
  - 公式：`step=(积年−1) mod 36 ÷ 3`；`pos=环[(offset+step) mod 12]`；`入宫年=(积年−1) mod 3 +1`。三元=`(积年−1) mod 180 ÷60`（上/中/下）；五纪=`÷36`（甲子/庚子/丙子/壬子/戊子纪）。180=三元×60=五纪×36。
  - 相位：立成表甲子/乙丑/丙寅三年同宫 → 用积年−1（P3 曾误用积年，1998 四神误作绛宫，实为9宫）。
  - 天乙/直符环序：三堂接在 9 宫之后再绕回 1（如天乙 6,7,8,9,绛,明,玉,1..5；P3 曾误作 6,7,8,9,1..5,绛,明,玉）。
  - 验证：立成表 180 格中 160 格与公式全合。**直符·中元列 20 格**四库本印作 1..9,1..9,绛,明（与推法正文及其余 11 列块矛盾，疑刻本讹）→ 按推法统一公式（用户裁定）。锚：2024甲辰下元→四神6/天乙明/地乙2/直符绛；724甲子下元→5/绛/1/9；1998戊寅下元→9/2/5/1；2046丙寅上元→四神1宫入宫3年；1960庚子中元→直符1宫（表讹作4）。
- **飞符**：淘金歌概念，卷五无，不列。
```

- [ ] **Step 2: 更新 memory `taiyi-feature.md`**

- frontmatter description 中 `P4-a(时计八门王希明法)已实现` 后补 `+P4-b(卷五四神三元五纪立成:统一12位环+地乙)`。
- P3 段的「以卷四原文模型为准…」表述不动（历史记录），在其后追加一段：

```markdown
**P4-b 卷五四神三元五纪立成已完成(2026-07-26)**：四神/天乙/地乙/直符改统一 12 位环[1..9,绛,明,玉]，offset 0/5/8/4，step=(积年−1) mod 36 ÷3，入宫年=(积年−1) mod 3 +1；注记含三元五纪(元=(积年−1) mod 180 ÷60、纪=÷36)。新增地乙(起9宫,推地乙正文四库本佚,占义从通行)。修正 P3 两处:天乙/直符环序(三堂接9宫后再绕回1,2024 天乙→明堂/直符→绛宫)、相位 off-by-one(1998 四神 绛→9宫)。直符中元列 20 格四库本疑刻本讹(印作 1..9,1..9,绛,明),按推法统一公式(用户裁定)。出处更正:四神诸法在卷五 KR3g0047_005(P3 误记卷四)。测试 30 全绿(锚:2024/724/1998/2046/1960 立成表行)。
```

- [ ] **Step 3: Commit**

```bash
git add .taiyi-reference.md
git commit -m "docs(太乙): §17 符神段改卷五立成口径(统一12位环+直符中元疑讹)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

（memory 目录在仓库外，不入库。）

---

## Self-Review 记录

- **Spec coverage**：spec §2 算法→Task 1 Step 3（step/入宫年/元/纪/offset/三堂 null 全覆盖）；§3 两处修正→实现注释+测试名与注释均点明；§4 代码改动→Task 1（computeJiXing 尾部+docstring）；§5 六测试锚点→Task 1 Step 1（改 4 增 2 全在）；§6 文档→Task 2；§7 不做项→无对应任务 ✓。
- **Placeholder scan**：无 TBD；测试与实现代码完整可粘贴。
- **Type consistency**：`TaiyiJiXing(name, location, palaceNo, cycle, inYear, auspicious, note)` 七参顺序与既有构造调用一致；`fsLoc` 返回 `Pair<String, Int?>` 与解构使用一致；测试中 `note`/`inYear`/`palaceNo` 字段名与模型一致。
- **锚点数值复核**（J=year+10153917，环=[1..9,绛,明,玉]）：
  - 2024：J−1 mod 180=160→下元/戊子纪；step=16/3=5；四神[5]=6、天乙[(5+5)%12=10]=明、地乙[(8+5)%12=1]=2、直符[(4+5)%12=9]=绛；入宫 16%3+1=2 ✓（立成甲辰下元：六/明/二/絳 ✓）
  - 724：mod 180=120→下元/壬子纪；step=4；5/绛/1/9；入宫 12%3+1=1 ✓（甲子下元：五/絳/一/九 ✓）
  - 1998：mod 180=134；step=26/3=8；四神[8]=9、天乙[13%12=1]=2、地乙[16%12=4]=5、直符[12%12=0]=1 ✓（戊寅下元：九/二/五/一 ✓）
  - 2046：mod 180=2→上元/甲子纪；step=0；四神=1；入宫 2%3+1=3 ✓（丙寅上元 四神=一 ✓；2046−4=2042，mod 60=2→丙寅 ✓）
  - 1960：mod 180=96→中元/丙子纪(96/36=2)；step=24/3=8；直符[(4+8)%12=0]=1；四神=9、天乙=2、地乙=5 ✓（庚子中元 九/二/五 ✓，直符表讹作四；1960−4=1956，mod 60=36→庚子 ✓）

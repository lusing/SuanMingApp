package cn.xulun.bazi

import cn.xulun.bazi.core.TaiyiCalculator
import cn.xulun.bazi.model.TaiyiCountType
import cn.xulun.bazi.model.TaiyiDun
import cn.xulun.bazi.model.TaiyiMen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TaiyiCalculatorTest {

    @Test
    fun `年计 724AD 太乙1宫乾 理天 阳遁`() {
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

    // ── Task 3: 太岁/合神/计神/文昌/始击 ──
    @Test
    fun `2024 太岁辰 合神酉 计神戌 文昌坤大武 始击亥大义`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertEquals("辰", p.taiSui.chinese)
        assertEquals("酉", p.heShen.chinese)
        assertEquals("戌", p.jiShen.chinese)
        assertEquals("坤", p.wenChang.position)
        assertEquals("大武", p.wenChang.title)
        assertEquals("亥", p.shiJi.position)
        assertEquals("大义", p.shiJi.title)
    }

    @Test
    fun `2025 太岁巳 合神申 计神酉 始击子`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2025, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertEquals("巳", p.taiSui.chinese)
        assertEquals("申", p.heShen.chinese)
        assertEquals("酉", p.jiShen.chinese)
        assertEquals("子", p.shiJi.position)
    }

    @Test
    fun `时计阴遁 2024-10-09 太岁子 合神丑 计神申 文昌寅吕申 始击坤`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 10, 9, 0, 30), TaiyiCountType.HOUR)
        assertEquals("子", p.taiSui.chinese)
        assertEquals("丑", p.heShen.chinese)
        assertEquals("申", p.jiShen.chinese)
        assertEquals("寅", p.wenChang.position)
        assertEquals("吕申", p.wenChang.title)
        assertEquals("坤", p.shiJi.position)
    }

    // ── Task 4: 主/客/定算 + 大小将 ──
    @Test
    fun `2024 主算38 客算25 定算14 大小将`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertEquals(38, p.zhuSuan)
        assertEquals(25, p.keSuan)
        assertEquals(14, p.dingSuan)
        assertEquals(8, p.zhuDaJiang); assertEquals(4, p.zhuCanJiang)
        assertEquals(5, p.keDaJiang); assertEquals(5, p.keCanJiang)   // 客大将中宫(杜塞)
        assertEquals(4, p.dingDaJiang); assertEquals(2, p.dingCanJiang)
        assertEquals("寅", p.dingMu.position)
    }

    @Test
    fun `2023 主算39 间辰加一 大将9 参将7`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2023, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertEquals(39, p.zhuSuan)
        assertEquals(9, p.zhuDaJiang)
        assertEquals(7, p.zhuCanJiang)
    }

    @Test
    fun `1570武备志 主算33客算10 大将参将`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(1570, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertEquals(33, p.zhuSuan)
        assertEquals(3, p.zhuDaJiang)
        assertEquals(9, p.zhuCanJiang)
        assertEquals(10, p.keSuan)
        assertEquals(1, p.keDaJiang)   // 10 → mod 9 = 1
        assertEquals(3, p.keCanJiang)
    }

    @Test
    fun `时计阴遁 主算5客算29定算7 大小将 定目艮`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 10, 9, 0, 30), TaiyiCountType.HOUR)
        assertEquals(5, p.zhuSuan)
        assertEquals(5, p.zhuDaJiang); assertEquals(5, p.zhuCanJiang)  // 杜塞
        assertEquals(29, p.keSuan)
        assertEquals(9, p.keDaJiang); assertEquals(7, p.keCanJiang)
        assertEquals(7, p.dingSuan)
        assertEquals(7, p.dingDaJiang); assertEquals(1, p.dingCanJiang)
        assertEquals("艮", p.dingMu.position)
    }

    // ── Task 5: 八门 + 格局 + 数分类 ──
    @Test
    fun `2024 年计八门 直使为伤门`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertEquals(TaiyiMen.SHANG, p.zhiShiMen)
        assertEquals(TaiyiMen.SHANG, p.menOnTaiyi[p.ju.taiyiPalaceNo]) // 直使落太乙宫
    }

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

    @Test
    fun `2024 格局 外宫迫 格 杜塞`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val names = p.patterns.map { it.name }
        assertTrue("迫: $names", names.any { it.contains("迫") })
        assertTrue("格: $names", names.any { it == "格" })
        assertTrue("杜塞: $names", names.any { it == "杜塞" })
    }

    @Test
    fun `2025 格局 关（客大将与主参将同宫）`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2025, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val names = p.patterns.map { it.name }
        assertTrue("关: $names", names.any { it == "关" })
    }

    @Test
    fun `2024 数分类 含长数或不和`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        assertTrue(p.suanClass.any { it.contains("长") || it.contains("不和") })
    }

    // ── Task 6: 卷四星宿（依金镜卷四原文核定，非 §17 通行版）──
    @Test
    fun `2024 卷四星宿 君基丑臣基亥民基丑 五福乾第6年 大游4宫 小游2宫`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = p.jiXing.associateBy { it.name }
        assertEquals("丑", m["君基"]!!.location)
        assertEquals("亥", m["臣基"]!!.location)
        assertEquals("丑", m["民基"]!!.location)
        assertEquals("乾", m["五福"]!!.location)
        assertEquals(6, m["五福"]!!.inYear)
        assertEquals(4, m["大游"]!!.palaceNo)
        assertEquals(2, m["小游"]!!.palaceNo)
    }

    @Test
    fun `724AD 五福在艮第11年 卷四原例硬锚点`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(724, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val w = p.jiXing.first { it.name == "五福" }
        assertEquals("艮", w.location)
        assertEquals(11, w.inYear)
    }

    @Test
    fun `2025 卷四星宿 民基寅 五福仍乾 大游4宫 小游2宫`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2025, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = p.jiXing.associateBy { it.name }
        assertEquals("寅", m["民基"]!!.location)
        assertEquals("乾", m["五福"]!!.location)
        assertEquals(4, m["大游"]!!.palaceNo)
        assertEquals(2, m["小游"]!!.palaceNo)
    }

    // ── Task 7: 复合格局（依卷三/淘金歌，触发稀少；断言不崩溃 + 2024 不误触发）──
    @Test
    fun `复合格局不崩溃且2024不误触发`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val names = p.patterns.map { it.name }
        assertTrue(p.patterns.isNotEmpty())
        assertFalse(names.any { it in setOf("四郭固", "四郭杜", "提挟", "执提") })
    }

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
    fun `卷五卷七诸神齐备 飞符不列`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val names = p.jiXing.map { it.name }
        // P2 六星 + 卷五四神 + 卷七阳九百六；飞符为淘金歌概念，不列
        listOf("君基", "臣基", "民基", "五福", "大游", "小游", "四神", "天乙", "地乙", "直符", "阳九", "百六").forEach {
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

    // ── 日计入局（P-日计；suanBase=epochDay+53 锚 2026-07-26=阳遁50局）──
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
}

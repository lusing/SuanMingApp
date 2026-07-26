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

    // ── Task 8: 卷四符神/乙神（四神/天乙/直符；卷四 9宫含中五+绛宫明堂玉堂三堂 12位模型，非 §17 淘金歌十二支序）──
    @Test
    fun `2024 卷四符神 四神兑6宫 天乙离2宫 直符乾1宫`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = p.jiXing.associateBy { it.name }
        assertEquals("兑6宫", m["四神"]!!.location)
        assertEquals(6, m["四神"]!!.palaceNo)
        assertEquals("离2宫", m["天乙"]!!.location)
        assertEquals(2, m["天乙"]!!.palaceNo)
        assertEquals("乾1宫", m["直符"]!!.location)
        assertEquals(1, m["直符"]!!.palaceNo)
    }

    @Test
    fun `724AD 卷四符神 四神中5宫 天乙乾1宫 直符巽9宫 同入纪年`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(724, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = p.jiXing.associateBy { it.name }
        assertEquals("中5宫", m["四神"]!!.location)
        assertEquals(5, m["四神"]!!.palaceNo)
        assertEquals("乾1宫", m["天乙"]!!.location)
        assertEquals("巽9宫", m["直符"]!!.location)
        // 三神同用入纪年→同 inYear（724 入纪年13→第2年）
        val years = listOf("四神", "天乙", "直符").map { m[it]!!.inYear }.toSet()
        assertEquals(setOf(2), years)
    }

    @Test
    fun `卷四十神齐备 地乙飞符不列`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val names = p.jiXing.map { it.name }
        // P2 六星（君基/臣基/民基/五福/大游/小游）+ P3 三神（四神/天乙/直符）＝九神；地乙/飞符卷四无，不列
        listOf("君基", "臣基", "民基", "五福", "大游", "小游", "四神", "天乙", "直符").forEach {
            assertTrue("$it 缺失: $names", names.contains(it))
        }
        assertFalse("地乙卷四无独立推法不应出现", names.contains("地乙"))
        assertFalse("飞符为淘金歌概念不应出现", names.contains("飞符"))
    }

    @Test
    fun `1998 四神落三堂绛宫 卷四未给坐标故palaceNo为空`() {
        // 1998 入纪年27→宫数9→绛宫（三堂首位；九宫1..9之外的三堂，卷四未给空间坐标）
        val p = TaiyiCalculator.build(LocalDateTime.of(1998, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = p.jiXing.associateBy { it.name }
        assertEquals("绛宫", m["四神"]!!.location)
        assertNull(m["四神"]!!.palaceNo)
    }
}

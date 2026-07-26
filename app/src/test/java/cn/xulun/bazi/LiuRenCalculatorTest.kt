package cn.xulun.bazi

import cn.xulun.bazi.core.LiuRenCalculator
import cn.xulun.bazi.core.LiuRenTables
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.TianJiang
import cn.xulun.bazi.model.YueJiang
import cn.xulun.bazi.model.ZongMen
import cn.xulun.bazi.ui.components.LIUREN_HEAVEN_PLATE_GRID
import cn.xulun.bazi.ui.components.liurenHeavenPlateCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class LiuRenCalculatorTest {

    // ── 月将（中气换将）──
    @Test
    fun `月将 冬至后为大吉丑`() {
        // 2025-12-25 在冬至(12-21)后、大寒(2026-1-20)前 → 大吉(丑)
        val yj = LiuRenCalculator.determineYueJiang(LocalDateTime.of(2025, 12, 25, 12, 0))
        assertEquals(YueJiang.DA_JI, yj)
    }

    @Test
    fun `月将 春分后为河魁戌`() {
        // 2026-3-25 在春分(3-20)后 → 河魁(戌)
        val yj = LiuRenCalculator.determineYueJiang(LocalDateTime.of(2026, 3, 25, 12, 0))
        assertEquals(YueJiang.HE_KUI, yj)
    }

    @Test
    fun `月将 夏至后为小吉未`() {
        // 夏至后→太阳在未宫→小吉(未)；大暑后才换胜光(午)
        val yj = LiuRenCalculator.determineYueJiang(LocalDateTime.of(2026, 7, 1, 12, 0))
        assertEquals(YueJiang.XIAO_JI, yj)
    }

    // ── 天盘（月将加占时）──
    @Test
    fun `天盘 月将丑加占时午`() {
        // offset = (丑1 - 午6 + 12) % 12 = 7
        val p = LiuRenCalculator.compute(HeavenlyStem.JIA, EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.CHOU)
        assertEquals(7, p.tianPanOffset)
        assertEquals(EarthlyBranch.WEI, p.heavenAt(EarthlyBranch.ZI))  // 子上→未
        assertEquals(EarthlyBranch.CHOU, p.heavenAt(EarthlyBranch.WU)) // 午上→丑(月将)
    }

    // ── 天地盘方盘映射 ──
    @Test
    fun `天地盘方位 上南下北且十二地支无缺漏`() {
        assertEquals(
            listOf(
                listOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI, EarthlyBranch.SHEN),
                listOf(EarthlyBranch.CHEN, null, null, EarthlyBranch.YOU),
                listOf(EarthlyBranch.MAO, null, null, EarthlyBranch.XU),
                listOf(EarthlyBranch.YIN, EarthlyBranch.CHOU, EarthlyBranch.ZI, EarthlyBranch.HAI),
            ),
            LIUREN_HEAVEN_PLATE_GRID,
        )
        val branches = LIUREN_HEAVEN_PLATE_GRID.flatten().filterNotNull()
        assertEquals(12, branches.size)
        assertEquals(EarthlyBranch.entries.toSet(), branches.toSet())
    }

    @Test
    fun `天地盘宫位 按地盘支映射天盘支与天将`() {
        val p = LiuRenCalculator.compute(
            HeavenlyStem.JIA,
            EarthlyBranch.ZI,
            EarthlyBranch.WU,
            EarthlyBranch.CHOU,
        )

        val noon = liurenHeavenPlateCell(p, EarthlyBranch.WU)

        assertEquals(EarthlyBranch.WU, noon.earthBranch)
        assertEquals(EarthlyBranch.CHOU, noon.heavenBranch)
        assertEquals(TianJiang.GUI_REN, noon.tianJiang)
    }

    @Test
    fun `天地盘方盘 伏吟时天地支逐宫重合`() {
        val p = LiuRenCalculator.compute(
            HeavenlyStem.JIA,
            EarthlyBranch.ZI,
            EarthlyBranch.CHOU,
            EarthlyBranch.CHOU,
        )
        val branches = LIUREN_HEAVEN_PLATE_GRID.flatten().filterNotNull()

        assertTrue(branches.all { branch ->
            liurenHeavenPlateCell(p, branch).heavenBranch == branch
        })
    }

    @Test
    fun `天地盘方盘 返吟时天地支逐宫相冲`() {
        val p = LiuRenCalculator.compute(
            HeavenlyStem.JIA,
            EarthlyBranch.ZI,
            EarthlyBranch.WEI,
            EarthlyBranch.CHOU,
        )
        val branches = LIUREN_HEAVEN_PLATE_GRID.flatten().filterNotNull()

        assertTrue(branches.all { branch ->
            val expectedOpposite = EarthlyBranch.fromIndex((branch.ordinal + 6) % 12)
            liurenHeavenPlateCell(p, branch).heavenBranch == expectedOpposite
        })
    }

    // ── 伏吟（月将==占时）查表 ──
    @Test
    fun `伏吟 甲子日 寅巳申`() {
        val p = LiuRenCalculator.compute(HeavenlyStem.JIA, EarthlyBranch.ZI, EarthlyBranch.CHOU, EarthlyBranch.CHOU)
        assertEquals(ZongMen.FU_YIN, p.threeChuan.zongMen)
        assertEquals(EarthlyBranch.YIN, p.threeChuan.initial)
        assertEquals(EarthlyBranch.SI, p.threeChuan.middle)
        assertEquals(EarthlyBranch.SHEN, p.threeChuan.last)
        assertTrue(p.threeChuan.keMing.contains("伏吟"))
    }

    @Test
    fun `伏吟 乙丑日 杜传 辰丑戌`() {
        val p = LiuRenCalculator.compute(HeavenlyStem.YI, EarthlyBranch.CHOU, EarthlyBranch.CHEN, EarthlyBranch.CHEN)
        assertEquals(ZongMen.FU_YIN, p.threeChuan.zongMen)
        assertEquals(EarthlyBranch.CHEN, p.threeChuan.initial)
        assertEquals(EarthlyBranch.CHOU, p.threeChuan.middle)
        assertEquals(EarthlyBranch.XU, p.threeChuan.last)
    }

    @Test
    fun `伏吟 壬辰日 亥辰戌（杜传自刑取冲）`() {
        val p = LiuRenCalculator.compute(HeavenlyStem.REN, EarthlyBranch.CHEN, EarthlyBranch.HAI, EarthlyBranch.HAI)
        assertEquals(ZongMen.FU_YIN, p.threeChuan.zongMen)
        assertEquals(EarthlyBranch.HAI, p.threeChuan.initial)
        assertEquals(EarthlyBranch.CHEN, p.threeChuan.middle)
        assertEquals(EarthlyBranch.XU, p.threeChuan.last)
    }

    // ── 返吟（月将==占时冲）查表 ──
    @Test
    fun `返吟 甲子日 无依 寅申寅`() {
        // 月将=丑(1)，占时=未(7)，(1-7+12)%12=6 → 返吟
        val p = LiuRenCalculator.compute(HeavenlyStem.JIA, EarthlyBranch.ZI, EarthlyBranch.WEI, EarthlyBranch.CHOU)
        assertEquals(ZongMen.FAN_YIN, p.threeChuan.zongMen)
        assertEquals(EarthlyBranch.YIN, p.threeChuan.initial)
        assertEquals(EarthlyBranch.SHEN, p.threeChuan.middle)
        assertEquals(EarthlyBranch.YIN, p.threeChuan.last)
        assertTrue(p.threeChuan.keMing.contains("无依"))
    }

    @Test
    fun `返吟 乙丑日 戌辰戌（修正本）`() {
        val p = LiuRenCalculator.compute(HeavenlyStem.YI, EarthlyBranch.CHOU, EarthlyBranch.WEI, EarthlyBranch.CHOU)
        assertEquals(ZongMen.FAN_YIN, p.threeChuan.zongMen)
        assertEquals(EarthlyBranch.XU, p.threeChuan.initial)
        assertEquals(EarthlyBranch.CHEN, p.threeChuan.middle)
        assertEquals(EarthlyBranch.XU, p.threeChuan.last)
    }

    @Test
    fun `返吟 辛丑日 井栏射 亥未辰`() {
        val p = LiuRenCalculator.compute(HeavenlyStem.XIN, EarthlyBranch.CHOU, EarthlyBranch.WEI, EarthlyBranch.CHOU)
        assertEquals(ZongMen.FAN_YIN, p.threeChuan.zongMen)
        assertEquals(EarthlyBranch.HAI, p.threeChuan.initial)
        assertEquals(EarthlyBranch.WEI, p.threeChuan.middle)
        assertEquals(EarthlyBranch.CHEN, p.threeChuan.last)
        assertTrue(p.threeChuan.keMing.contains("井栏射"))
    }

    // ── 四课结构 ──
    @Test
    fun `四课 一课下神为日干 且 取干五行`() {
        // 乙寄辰：一课 bottomBranch=辰(土) 但 bottomElement 应取乙(木)
        val p = LiuRenCalculator.compute(HeavenlyStem.YI, EarthlyBranch.CHOU, EarthlyBranch.WU, EarthlyBranch.CHOU)
        val l1 = p.lesson(1)
        assertEquals(EarthlyBranch.CHEN, l1.bottomBranch)
        assertEquals(HeavenlyStem.YI, l1.bottomStem)
        // 乙木，非辰土
        assertEquals(cn.xulun.bazi.model.Element.WOOD, l1.bottomElement)
    }

    @Test
    fun `四课 二课下神为一课上神`() {
        val p = LiuRenCalculator.compute(HeavenlyStem.JIA, EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.CHOU)
        val l1 = p.lesson(1)
        val l2 = p.lesson(2)
        assertEquals(l1.top, l2.bottomBranch)
    }

    // ── 中末传通则（非伏吟返吟：中=初之上神）──
    @Test
    fun `三传 中末为初之上神`() {
        // 取一个克贼课（非伏吟非返吟）的盘：甲子日、午时、丑将
        val p = LiuRenCalculator.compute(HeavenlyStem.JIA, EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.CHOU)
        val c1 = p.threeChuan.initial
        assertEquals(p.heavenAt(c1), p.threeChuan.middle)
        assertEquals(p.heavenAt(p.threeChuan.middle), p.threeChuan.last)
    }

    // ── 十二天将：贵人落位 ──
    @Test
    fun `天将 甲日昼贵 丑`() {
        // 甲日 昼贵=丑。月将=丑、占时=午(offset=7)，贵人天盘支=丑落在地盘午(逆)
        val p = LiuRenCalculator.compute(HeavenlyStem.JIA, EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.CHOU)
        assertTrue(p.isDay)
        val gui = p.tianJiang.first { it.jiang == TianJiang.GUI_REN }
        assertEquals(EarthlyBranch.CHOU, gui.heavenBranch) // 贵人所乘天盘支=丑
    }

    @Test
    fun `天将 十二将齐全`() {
        val p = LiuRenCalculator.compute(HeavenlyStem.JIA, EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.CHOU)
        assertEquals(12, p.tianJiang.size)
        assertEquals(TianJiang.entries, p.tianJiang.map { it.jiang })
    }

    @Test
    fun `天将 顺逆 贵人落地盘亥子丑寅卯辰则顺`() {
        // 甲日昼贵=丑；月将=午(胜光)、占时=午 → offset=0(伏吟式天盘)，贵人天盘支=丑落在地盘丑(顺)
        val p = LiuRenCalculator.compute(HeavenlyStem.JIA, EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.WU)
        val gui = p.tianJiang.first { it.jiang == TianJiang.GUI_REN }
        assertEquals(EarthlyBranch.CHOU, gui.earthBranch) // 贵人落地盘丑∈顺布集
        // 顺布：腾蛇应在丑+1=寅的地盘位
        val she = p.tianJiang.first { it.jiang == TianJiang.TENG_SHE }
        assertEquals(EarthlyBranch.YIN, she.earthBranch)
    }

    // ── 旬空（公式法）──
    @Test
    fun `旬空 甲子旬为戌亥`() {
        // 甲子 cycleIndex=0
        assertEquals(setOf(EarthlyBranch.XU, EarthlyBranch.HAI), LiuRenTables.xunKong(0))
    }

    @Test
    fun `旬空 甲辰旬为寅卯（非辰巳）`() {
        // 甲辰 cycleIndex=40
        assertEquals(setOf(EarthlyBranch.YIN, EarthlyBranch.MAO), LiuRenTables.xunKong(40))
    }

    @Test
    fun `旬丁 甲子旬为卯`() {
        assertEquals(EarthlyBranch.MAO, LiuRenTables.xunDingBranch(0))
    }

    // ── 行年 ──
    @Test
    fun `行年 男30岁未 女同龄卯`() {
        val male = LiuRenCalculator.buildPlate(
            LocalDateTime.of(2026, 7, 1, 12, 0), Gender.MALE, 30,
        )
        assertEquals(EarthlyBranch.WEI, male.nianMing.xingNian) // 寅2+29=31%12=7=未
        val female = LiuRenCalculator.buildPlate(
            LocalDateTime.of(2026, 7, 1, 12, 0), Gender.FEMALE, 30,
        )
        assertEquals(EarthlyBranch.MAO, female.nianMing.xingNian) // 申8-29=-21%12=3=卯
    }
}

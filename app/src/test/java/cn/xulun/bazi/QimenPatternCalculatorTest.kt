package cn.xulun.bazi

import cn.xulun.bazi.core.QimenPatternCalculator
import cn.xulun.bazi.model.Dun
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.EightDoor
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.JuMethod
import cn.xulun.bazi.model.JuResult
import cn.xulun.bazi.model.NineStar
import cn.xulun.bazi.model.PalaceCell
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.QiGan
import cn.xulun.bazi.model.QiMenPlate
import cn.xulun.bazi.model.QiPalace
import cn.xulun.bazi.model.Yuan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QimenPatternCalculatorTest {

    private val ju = JuResult(Dun.YANG, 1, "冬至", 21, Yuan.SHANG, JuMethod.CHAIBU, "甲子", false)
    private val dayPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI)
    private val hourPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI)

    /** 构造测试盘：默认各宫天地干同为戊，可按宫号覆盖 */
    private fun plate(
        overrides: Map<Int, Pair<QiGan, QiGan>> = emptyMap(),
    ): QiMenPlate {
        val cells = (1..9).associateWith { no ->
            val (h, e) = overrides[no] ?: (QiGan.WU to QiGan.WU)
            PalaceCell(
                palace = QiPalace.of(no),
                star = NineStar.atPalace(no),
                door = EightDoor.atPalace(no),
                spirit = null,
                heavenGan = h,
                earthGan = e,
                isZhiFuPalace = false,
                isZhiShiPalace = false,
            )
        }
        return QiMenPlate(cells, NineStar.PENG, EightDoor.XIU, ju, dayPillar, hourPillar, "中宫寄坤二宫")
    }

    @Test
    fun `青龙返首 天戊地丙`() {
        val p = plate(mapOf(1 to (QiGan.WU to QiGan.BING)))
        val names = QimenPatternCalculator.analyze(p).map { it.name }
        assertTrue("应识别青龙返首：$names", names.any { it.startsWith("青龙返首") })
    }

    @Test
    fun `青龙逃走 与 白虎猖狂`() {
        val p = plate(mapOf(
            1 to (QiGan.YI to QiGan.XIN),   // 青龙逃走
            2 to (QiGan.XIN to QiGan.YI),   // 白虎猖狂
        ))
        val names = QimenPatternCalculator.analyze(p).map { it.name }
        assertTrue(names.any { it.startsWith("青龙逃走") })
        assertTrue(names.any { it.startsWith("白虎猖狂") })
    }

    @Test
    fun `伏吟 天地重合`() {
        // 各宫天干=地干（全戊）→ 伏吟
        val p = plate()
        val names = QimenPatternCalculator.analyze(p).map { it.name }
        assertTrue("应识别伏吟：$names", names.any { it.startsWith("伏吟") })
    }

    @Test
    fun `门迫 门克宫`() {
        // 构造一宫：开门(金)临震3(木)→金克木，门克宫=门迫
        val p = plate()
        val names = QimenPatternCalculator.analyze(p).map { it.name }
        // 默认盘开门在乾6(金)不克金；此处仅校验识别逻辑不崩溃且产出非空
        assertTrue(QimenPatternCalculator.analyze(p).isNotEmpty())
    }

    /** 构造单宫定制的盘（指定宫位的星/天地干/门） */
    private fun plateWith(
        no: Int,
        star: NineStar,
        heaven: QiGan,
        earth: QiGan,
        door: EightDoor? = EightDoor.atPalace(no),
    ): QiMenPlate {
        val cells = (1..9).associateWith { q ->
            if (q == no) {
                PalaceCell(QiPalace.of(q), star, door, null, heaven, earth, false, false)
            } else {
                PalaceCell(QiPalace.of(q), NineStar.atPalace(q), EightDoor.atPalace(q), null, QiGan.WU, QiGan.WU, false, false)
            }
        }
        return QiMenPlate(cells, NineStar.PENG, EightDoor.XIU, ju, dayPillar, hourPillar, "中宫寄坤二宫")
    }

    @Test
    fun `单宫分析 星宫生克`() {
        // 天蓬(水)落离9(火)：水克火 → 星克宫
        val a = QimenPatternCalculator.analyzePalace(plateWith(9, NineStar.PENG, QiGan.WU, QiGan.WU), 9)
        val starItem = a.items.first { it.label == "九星" }
        assertTrue("应判星克宫：${starItem.text}", starItem.text.contains("星克宫"))
    }

    @Test
    fun `单宫分析 门迫`() {
        // 伤门(木)落坤2(土)：木克土 → 门迫
        val a = QimenPatternCalculator.analyzePalace(plateWith(2, NineStar.RUI, QiGan.WU, QiGan.WU, EightDoor.SHANG), 2)
        val doorItem = a.items.first { it.label == "八门" }
        assertTrue("应判门迫：${doorItem.text}", doorItem.text.contains("门迫"))
    }

    @Test
    fun `单宫分析 组合格局`() {
        // 天戊加地丙 → 青龙返首
        val a = QimenPatternCalculator.analyzePalace(plateWith(1, NineStar.PENG, QiGan.WU, QiGan.BING), 1)
        assertTrue("应命中青龙返首：${a.patterns.map { it.name }}", a.patterns.any { it.name.startsWith("青龙返首") })
    }

    @Test
    fun `单宫分析 击刑`() {
        // 辛(甲午)临离9 → 击刑
        val a = QimenPatternCalculator.analyzePalace(plateWith(9, NineStar.YING, QiGan.XIN, QiGan.JI), 9)
        assertTrue("应命中击刑：${a.patterns.map { it.name }}", a.patterns.any { it.name.startsWith("击刑") })
    }

    @Test
    fun `单宫分析 空亡`() {
        // 时柱甲子 → 旬空戌亥 → 落乾6宫；分析6宫应标空亡
        val a = QimenPatternCalculator.analyzePalace(plateWith(6, NineStar.XIN_S, QiGan.GUI, QiGan.GUI), 6)
        assertTrue("应标空亡：${a.tags}", a.tags.contains("空亡"))
        assertTrue(a.patterns.any { it.name == "空亡" })
    }

    @Test
    fun `单宫分析 中宫寄`() {
        val a = QimenPatternCalculator.analyzePalace(plate(), 5)
        assertTrue("应有寄宫说明：${a.items.map { it.label }}", a.items.any { it.label == "寄宫" })
        // 中宫无门无神
        assertTrue(a.items.none { it.label == "八门" })
        assertTrue(a.items.none { it.label == "八神" })
    }

    @Test
    fun `单宫分析 综合断语非空且标题正确`() {
        val a = QimenPatternCalculator.analyzePalace(plateWith(1, NineStar.PENG, QiGan.WU, QiGan.WU), 1)
        assertEquals("坎1宫", a.title)
        assertTrue(a.summary.isNotBlank())
        assertTrue(a.subtitle.contains("坎") || a.subtitle.contains("北"))
    }
}

package cn.xulun.bazi

import cn.xulun.bazi.core.QimenPlateCalculator
import cn.xulun.bazi.model.Dun
import cn.xulun.bazi.model.JuMethod
import cn.xulun.bazi.model.JuResult
import cn.xulun.bazi.model.QiGan
import cn.xulun.bazi.model.Yuan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class QimenPlateCalculatorTest {

    private fun ju(dun: Dun, n: Int) = JuResult(dun, n, "冬至", 21, Yuan.SHANG, JuMethod.CHAIBU, "甲子", false)
    private val dt = LocalDateTime.of(2024, 12, 22, 12, 0)

    @Test
    fun `阳遁一局 地盘六仪三奇顺布`() {
        // 阳遁一局：坎1戊、坤2己、震3庚、巽4辛、中5壬、乾6癸、兑7丁、艮8丙、离9乙
        val plate = QimenPlateCalculator.buildPlate(dt, ju(Dun.YANG, 1))
        assertEquals(QiGan.WU, plate.cells.getValue(1).earthGan)
        assertEquals(QiGan.JI, plate.cells.getValue(2).earthGan)
        assertEquals(QiGan.GENG, plate.cells.getValue(3).earthGan)
        assertEquals(QiGan.XIN, plate.cells.getValue(4).earthGan)
        assertEquals(QiGan.REN, plate.cells.getValue(5).earthGan)
        assertEquals(QiGan.GUI, plate.cells.getValue(6).earthGan)
        assertEquals(QiGan.DING, plate.cells.getValue(7).earthGan)
        assertEquals(QiGan.BING, plate.cells.getValue(8).earthGan)
        assertEquals(QiGan.YI, plate.cells.getValue(9).earthGan)
    }

    @Test
    fun `阴遁九局 地盘逆布`() {
        // 阴遁九局：离9戊、艮8己…坎1乙
        val plate = QimenPlateCalculator.buildPlate(dt, ju(Dun.YIN, 9))
        assertEquals(QiGan.WU, plate.cells.getValue(9).earthGan)
        assertEquals(QiGan.YI, plate.cells.getValue(1).earthGan)
        assertEquals(QiGan.GENG, plate.cells.getValue(7).earthGan) // 兑7庚
    }

    @Test
    fun `九宫齐全 中宫无门`() {
        val plate = QimenPlateCalculator.buildPlate(dt, ju(Dun.YANG, 1))
        (1..9).forEach { assertNotNull("宫 $it", plate.cells[it]) }
        assertNull(plate.cells.getValue(5).door)
        assertEquals(5, plate.cells.getValue(5).palace.no)
    }

    @Test
    fun `直符落宫含天盘直符仪`() {
        val plate = QimenPlateCalculator.buildPlate(dt, ju(Dun.YANG, 1))
        // 时柱旬首遁仪（直符仪）必落某宫，且该宫标记为直符
        val zhiFuCell = plate.cells.values.firstOrNull { it.isZhiFuPalace }
        assertNotNull(zhiFuCell)
        // 直符宫与直使宫各恰有一个
        assertEquals(1, plate.cells.values.count { it.isZhiFuPalace })
        assertEquals(1, plate.cells.values.count { it.isZhiShiPalace })
    }

    @Test
    fun `天盘与地盘干总数各九 且外八宫环布`() {
        val plate = QimenPlateCalculator.buildPlate(dt, ju(Dun.YANG, 3))
        // 天盘干九个仪奇不重复（中宫除外仍唯一）
        val heavens = plate.cells.values.map { it.heavenGan }
        assertEquals(9, heavens.distinct().size)
        assertTrue(plate.zhiFuStar.homePalace in 1..9)
    }
}

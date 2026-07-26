package cn.xulun.bazi

import cn.xulun.bazi.core.SolarTerm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** 节气查表解码 + Meeus 回退 */
class SolarTermTest {

    @Test
    fun table_decode_1899_lichun() {
        // 1899 立春打包值 2040807 → 1899-02-04 08:07
        assertEquals(
            LocalDateTime.of(1899, 2, 4, 8, 7),
            SolarTerm.getSolarTerm(1899, 0),
        )
    }

    @Test
    fun table_decode_1899_xiaohan_inNextYear() {
        // index 22 小寒 = 次年(1900)1月；打包值 1060204 → 1900-01-06 02:04
        assertEquals(
            LocalDateTime.of(1900, 1, 6, 2, 4),
            SolarTerm.getSolarTerm(1899, 22),
        )
    }

    @Test
    fun table_2024_lichun() {
        // 2024 立春 = 2024-02-04 16:27
        val lichun = SolarTerm.getSolarTerm(2024, 0)
        assertEquals(LocalDateTime.of(2024, 2, 4, 16, 27), lichun)
    }

    @Test
    fun yearTerms_areAscending_and24() {
        for (year in intArrayOf(1900, 1985, 2024, 2100)) {
            val terms = SolarTerm.getYearSolarTerms(year)
            assertEquals(24, terms.size)
            for (i in 1 until terms.size) {
                assertTrue("year $year term $i not ascending", !terms[i].isBefore(terms[i - 1]))
            }
        }
    }

    @Test
    fun meeus_fallback_returns24Ascending() {
        // 2200 在表外，走 Meeus 回退
        val terms = SolarTerm.computeWithMeeus(2200)
        assertEquals(24, terms.size)
        for (i in 1 until terms.size) {
            assertTrue("meeus term $i not ascending", !terms[i].isBefore(terms[i - 1]))
        }
        // 立春约 2/4，春分约 3/20
        assertEquals(2, terms[0].monthValue)
        assertEquals(3, terms[3].monthValue)
    }
}

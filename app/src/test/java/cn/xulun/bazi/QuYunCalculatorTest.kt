package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.QuYunCalculator
import cn.xulun.bazi.core.YongShenCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.QuYunVerdict
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class QuYunCalculatorTest {
    private fun bazi(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        BaziCalculator.calculate(LocalDateTime.of(y, m, d, h, min))

    // 锚点 A：正印格·印格官煞竞透（取清），庚辰运 → 干支皆喜
    @Test fun anchorA_yin_geju_guan_sha_jing_xiyun() {
        val b = bazi(1985, 5, 20, 0, 10)
        val ys = YongShenCalculator.analyze(b)
        val r = QuYunCalculator.analyze(b, ys, Pillar(HeavenlyStem.GENG, EarthlyBranch.CHEN)) // 庚辰运
        assertEquals("印格官煞竞透（取清）", r.ziPeiZhi.subName)
        assertEquals(QuYunVerdict.XI, r.gan.verdict)
        assertEquals(QuYunVerdict.XI, r.zhi.verdict)
        assertEquals(QuYunVerdict.XI, r.summary.verdict)
        assertEquals("喜运", r.summary.text)
    }

    // 锚点 D：正印格·印绶（孤印），丁丑运 → 干喜支平，一喜一平
    @Test fun anchorD_yin_geji_gu_yin_xi_zhong_ping() {
        val b = bazi(2000, 3, 4, 6, 0)
        val ys = YongShenCalculator.analyze(b)
        val r = QuYunCalculator.analyze(b, ys, Pillar(HeavenlyStem.DING, EarthlyBranch.CHOU)) // 丁丑运
        assertEquals("印绶（孤印）", r.ziPeiZhi.subName)
        assertEquals(QuYunVerdict.XI, r.gan.verdict)
        assertEquals(QuYunVerdict.PING, r.zhi.verdict)
        assertEquals(QuYunVerdict.XI_ZHONG_PING, r.summary.verdict)
    }
}

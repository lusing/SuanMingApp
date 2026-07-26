package cn.xulun.bazi

import cn.xulun.bazi.core.YongShenCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.GeJuChengBai
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.YongShenSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 子平真诠用神/相神/成败分析器测试。
 * 用合成的四柱（直接构造 Pillar，birthDateTime 不参与用神判定）锁定：
 * 用神取法（透干/本气兜底）、相神识别（财生官/食制杀/杀印相生）、成败（成/败）、建禄他柱寻用。
 */
class YongShenCalculatorTest {

    private fun bazi(
        yStem: HeavenlyStem, yBranch: EarthlyBranch,
        mStem: HeavenlyStem, mBranch: EarthlyBranch,
        dStem: HeavenlyStem, dBranch: EarthlyBranch,
        hStem: HeavenlyStem, hBranch: EarthlyBranch,
    ): BaziResult = BaziResult(
        yearPillar = Pillar(yStem, yBranch),
        monthPillar = Pillar(mStem, mBranch),
        dayPillar = Pillar(dStem, dBranch),
        hourPillar = Pillar(hStem, hBranch),
        birthDateTime = LocalDateTime.of(2000, 1, 1, 12, 0),
    )

    @Test
    fun `正官格 财透生官 为相 成格`() {
        // 甲辰 辛酉 甲子 戊辰 —— 月令酉藏辛(正官)透月干；时干戊(偏财)为相
        val r = YongShenCalculator.analyze(bazi(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
        ))
        assertEquals("正官格", r.geJuName)
        assertEquals(YongShenSource.TOU_GAN, r.source)
        assertEquals(TenGod.ZHENG_GUAN, r.yongShen?.tenGod)
        assertEquals(HeavenlyStem.XIN, r.yongShen?.stem)
        assertEquals(TenGod.PIAN_CAI, r.xiangShen?.tenGod)
        assertEquals("财生官", r.xiangShenRole)
        assertEquals(GeJuChengBai.CHENG, r.chengBai)
    }

    @Test
    fun `七杀格 食神制杀 为相 成格`() {
        // 甲辰 庚申 甲子 丙巳 —— 月令申藏庚(七杀)透月干；时干丙(食神)为相
        val r = YongShenCalculator.analyze(bazi(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.SI,
        ))
        assertEquals("七杀格", r.geJuName)
        assertEquals(TenGod.QI_SHA, r.yongShen?.tenGod)
        assertEquals(TenGod.SHI_SHEN, r.xiangShen?.tenGod)
        assertEquals("食神制杀", r.xiangShenRole)
        assertEquals(GeJuChengBai.CHENG, r.chengBai)
    }

    @Test
    fun `偏财格 比劫夺财 败格`() {
        // 甲辰 戊辰 甲子 甲辰 —— 月令辰本气戊(偏财)透月干；时干甲(比肩)夺财，无食伤官杀救应
        val r = YongShenCalculator.analyze(bazi(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
        ))
        assertEquals("偏财格", r.geJuName)
        assertEquals(TenGod.PIAN_CAI, r.yongShen?.tenGod)
        assertEquals(GeJuChengBai.BAI, r.chengBai)
        assertTrue(r.chengBaiReason.contains("比劫夺财"))
    }

    @Test
    fun `偏印格 杀印相生 为相 成格`() {
        // 甲亥 壬亥 甲子 庚申 —— 月令亥藏壬(偏印)透月干；时干庚(七杀)杀印相生为相
        val r = YongShenCalculator.analyze(bazi(
            HeavenlyStem.JIA, EarthlyBranch.HAI,
            HeavenlyStem.REN, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
        ))
        assertEquals("偏印格", r.geJuName)
        assertEquals(TenGod.PIAN_YIN, r.yongShen?.tenGod)
        assertEquals(TenGod.QI_SHA, r.xiangShen?.tenGod)
        assertTrue(r.xiangShenRole.contains("杀印相生"))
        assertEquals(GeJuChengBai.CHENG, r.chengBai)
    }

    @Test
    fun `建禄格 月令比劫 于他柱寻官为用`() {
        // 丙寅 甲寅 甲寅 辛酉 —— 月令寅本气甲为比肩(建禄)；他柱时干辛(正官)透，取为用
        val r = YongShenCalculator.analyze(bazi(
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
        ))
        assertEquals("建禄格", r.geJuName)
        assertEquals(YongShenSource.OTHER, r.source)
        assertEquals(TenGod.ZHENG_GUAN, r.yongShen?.tenGod)
        assertEquals(HeavenlyStem.XIN, r.yongShen?.stem)
    }

    @Test
    fun `藏干俱不透 以月令本气为用`() {
        // 丙辰 丁辰 甲辰 庚辰 —— 月令辰藏戊乙癸俱不透，以本气戊(偏财)定格
        val r = YongShenCalculator.analyze(bazi(
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
        ))
        assertEquals(YongShenSource.BEN_QI, r.source)
        assertEquals(TenGod.PIAN_CAI, r.yongShen?.tenGod)
        assertEquals(HeavenlyStem.WU, r.yongShen?.stem)
        assertEquals("偏财格", r.geJuName)
    }

    @Test
    fun `正官见伤官 败格`() {
        // 甲酉 辛酉 甲酉 丁酉 —— 月令酉(正官)，时干丁(伤官)见官，无印解救
        val r = YongShenCalculator.analyze(bazi(
            HeavenlyStem.JIA, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.YOU,
            HeavenlyStem.DING, EarthlyBranch.YOU,
        ))
        assertEquals("正官格", r.geJuName)
        assertEquals(GeJuChengBai.BAI, r.chengBai)
        assertTrue(r.chengBaiReason.contains("伤官见官"))
    }
}

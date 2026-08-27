package cn.xulun.bazi

import cn.xulun.bazi.core.DiZhiRelationCalculator
import cn.xulun.bazi.core.HeHuaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 关系补全锚点测试：天干相冲/三会土局/半合/拱局/六害/刑名/暗合。
 * 理论见 .shengke-reference.md 第八/十一/十二/十四/十五/十六节；命例尽量取自教材。
 */
class RelationCompletionTest {

    // ── 天干相冲（第八节：同性相斥方位对冲，戊己居中不冲） ──

    @Test fun `天干冲 甲庚相冲`() {
        // 强三例：庚寅 癸未 甲寅 壬申——年干庚与时…年支? 甲(日干)与庚(年干)相冲
        val r = HeHuaCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        ))
        assertTrue("应检出甲庚冲：${r.stemChongList}", r.stemChongList.any { it.description == "甲庚相冲" })
    }

    @Test fun `天干冲 乙辛相冲`() {
        // 扶官杀病二例：乙巳 辛巳 辛未 庚寅——年干乙与月干辛相冲
        val r = HeHuaCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
        ))
        assertTrue(r.stemChongList.any { it.description == "乙辛相冲" })
    }

    @Test fun `天干冲 戊己不冲`() {
        // 戊午 己未 壬申 庚子：戊己与他干无冲；壬丙?无丙——stemChongList 应为空
        val r = HeHuaCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
        ))
        assertTrue("戊己居中不冲：${r.stemChongList}", r.stemChongList.isEmpty())
    }

    // ── 三会土局（第十二节：辰戌丑未会四方土局） ──

    @Test fun `三会土局 四库全`() {
        // 戊辰 壬戌 乙丑 癸未——辰戌丑未四支齐
        val r = HeHuaCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
        ))
        assertTrue("四库全应会土局：${r.branchSanHuiList}", r.branchSanHuiList.any { it.description.contains("四方土局") })
    }

    // ── 半合（第十一节：生地半合>墓地半合，金局酉丑例外） ──

    @Test fun `半合 生地 寅午半合火`() {
        // 强旺二例：甲子 丙寅 丙午 己丑——寅(生地)+午(中神)，无戌
        val r = HeHuaCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
        ))
        assertTrue("应检出寅午生地半合：${r.branchBanHeList}",
            r.branchBanHeList.any { it.description.contains("寅午") && it.description.contains("生地") })
    }

    @Test fun `半合 墓地 午戌半合火`() {
        // 扶食伤病例：己丑 庚午 丙子 戊戌——午(中神)+戌(墓地)，无寅
        val r = HeHuaCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.XU,
        ))
        assertTrue("应检出午戌墓地半合：${r.branchBanHeList}",
            r.branchBanHeList.any { it.description.contains("午戌") && it.description.contains("墓地") })
    }

    @Test fun `半合 全三合时不报`() {
        // 弱二例：丁丑 乙巳 丁酉 辛丑——巳酉丑三支齐备成三合，不应再报半合
        val r = HeHuaCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
        ))
        assertTrue("全三合不报半合：${r.branchBanHeList}", r.branchBanHeList.isEmpty())
    }

    // ── 拱局（第十一节：三合缺中神为拱，如申辰拱子） ──

    @Test fun `拱局 亥未拱木`() {
        // 辛亥 乙未 壬申 辛亥——亥(生地)+未(墓地)缺卯(中神)
        val r = HeHuaCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
        ))
        assertTrue("应检出亥未拱木局：${r.branchGongList}",
            r.branchGongList.any { it.description.contains("亥未") && it.description.contains("拱") })
    }

    // ── 六害（第十四节） ──

    @Test fun `六害 子未相害`() {
        // 通关一例：戊午 己未 壬申 庚子——月支未与时支子相害
        val r = DiZhiRelationCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
        ))
        assertTrue("应检出子未害：${r.haiList}", r.haiList.any { it.description == "子未相害" })
    }

    @Test fun `六害 丑午相害`() {
        // 调候冲突例：庚午 己丑 己丑 壬申——年支午与月支丑相害
        val r = DiZhiRelationCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        ))
        assertTrue("应检出丑午害：${r.haiList}", r.haiList.any { it.description == "丑午相害" })
    }

    // ── 刑名（第十五节：无礼/恃势/无恩） ──

    @Test fun `刑名 子卯无礼之刑`() {
        val r = DiZhiRelationCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
        ))
        assertTrue("子卯刑应带刑名：${r.xingList}",
            r.xingList.any { it.description.contains("子卯") && it.description.contains("无礼之刑") })
    }

    // ── 暗合（第十六节用例：卯申暗合，化解冲财） ──

    @Test fun `暗合 卯申暗合`() {
        // 癸卯 庚申 己卯 壬申——卯申两见
        val r = DiZhiRelationCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        ))
        assertTrue("应检出卯申暗合：${r.anHeList}",
            r.anHeList.isNotEmpty() && r.anHeList.all { it.description == "卯申暗合" })
    }

    @Test fun `暗合 无暗合则空`() {
        // 戊午 己未 壬申 庚子——无卯申
        val r = DiZhiRelationCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
        ))
        assertTrue(r.anHeList.isEmpty())
    }
}

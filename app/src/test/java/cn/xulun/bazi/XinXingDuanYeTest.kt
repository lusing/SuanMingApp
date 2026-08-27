package cn.xulun.bazi

import cn.xulun.bazi.core.HangYeCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 十神心性断行业锚点（十一例九档）。理论见 .shengke-reference.md 第六四节三。
 */
class XinXingDuanYeTest {

    private fun its(y: HeavenlyStem, yb: EarthlyBranch, m: HeavenlyStem, mb: EarthlyBranch,
                    d: HeavenlyStem, db: EarthlyBranch, h: HeavenlyStem, hb: EarthlyBranch) =
        HangYeCalculator.analyze(chartOf(y, yb, m, mb, d, db, h, hb)).items

    @Test fun `例一 丙申庚寅丁未壬寅 正官行政（市长）`() {
        val r = its(HeavenlyStem.BING, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.WEI, HeavenlyStem.REN, EarthlyBranch.YIN)
        assertTrue("正官→行政：${r.map { it.field }}", r.any { it.field.contains("行政") })
    }

    @Test fun `例二 甲辰丁卯戊辰甲寅 双杀公安（法院科长）`() {
        val r = its(HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JIA, EarthlyBranch.YIN)
        assertTrue("双杀→公安法制：${r.map { it.field }}", r.any { it.field.contains("公安") })
    }

    @Test fun `例三 癸巳乙卯辛巳辛卯 从财官企业官员（银行厅级）`() {
        val r = its(HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.XIN, EarthlyBranch.SI, HeavenlyStem.XIN, EarthlyBranch.MAO)
        assertTrue("从财官→企业官员：${r.map { it.field }}", r.any { it.field.contains("企业") || it.field.contains("金融") })
    }

    @Test fun `例四 丙午癸巳壬申辛亥 身旺财当令经商`() {
        val r = its(HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.HAI)
        assertTrue("身旺胜财→经商：${r.map { it.field }}", r.any { it.field.contains("经商") })
    }

    @Test fun `例五 甲寅丙寅癸未壬戌 伤官投机智慧（证券操盘）`() {
        val r = its(HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.REN, EarthlyBranch.XU)
        assertTrue("伤官→投机智慧：${r.map { it.field }}", r.any { it.field.contains("投机") })
    }

    @Test fun `例七 丁未戊申丙寅乙未 财令食透商人（餐饮老板）`() {
        val r = its(HeavenlyStem.DING, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.WEI)
        assertTrue("财令食透→经商：${r.map { it.field }}", r.any { it.field.contains("经商") })
    }

    @Test fun `例八 壬辰辛亥乙酉己卯 杀印相生技术（外科医生）`() {
        val r = its(HeavenlyStem.REN, EarthlyBranch.CHEN, HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.JI, EarthlyBranch.MAO)
        assertTrue("杀印相生→技术：${r.map { it.field }}", r.any { it.field.contains("技术") })
    }

    @Test fun `例九 戊戌辛酉戊戌戊午 食伤泄秀艺术（音乐教师）`() {
        val r = its(HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.WU, EarthlyBranch.WU)
        assertTrue("泄秀→艺术：${r.map { it.field }}", r.any { it.field.contains("艺术") || it.field.contains("演讲") })
    }

    @Test fun `例十 癸卯辛酉甲申甲子 身弱官清受人尊重（医生）`() {
        val r = its(HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.JIA, EarthlyBranch.ZI)
        assertTrue("官清→清闲尊重：${r.map { it.field }}", r.any { it.field.contains("受人尊重") || it.field.contains("行政") })
    }

    @Test fun `例十一 丁亥壬子丙子癸巳 身弱官重手艺（铝厂工人）`() {
        val r = its(HeavenlyStem.DING, EarthlyBranch.HAI, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.SI)
        assertTrue("官重→手艺糊口：${r.map { it.field }}", r.any { it.field.contains("手艺") })
    }
}

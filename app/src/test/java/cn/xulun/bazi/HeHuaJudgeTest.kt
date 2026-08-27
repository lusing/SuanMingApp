package cn.xulun.bazi

import cn.xulun.bazi.core.HeHuaJudge
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 天干合化成败黄金母版（第五六〇节四正化条件——35例）。
 * 化成=true（真化），不化=false（假化/合而不化）。
 */
class HeHuaJudgeTest {

    data class C(val y: HeavenlyStem, val yb: EarthlyBranch, val m: HeavenlyStem, val mb: EarthlyBranch,
                 val d: HeavenlyStem, val db: EarthlyBranch, val h: HeavenlyStem, val hb: EarthlyBranch,
                 val cheng: Boolean, val note: String, val hs: Element? = null)

    private val cases = listOf(
        // 甲己合化土
        C(HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.YIN, true, "丑月土旺寅午半合丙透", Element.EARTH),
        C(HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.YI, EarthlyBranch.HAI, false, "亥子丑水局水旺于土"),
        C(HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.ZI, true, "土旺克住水局（不彻底）", Element.EARTH),
        C(HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.JI, EarthlyBranch.HAI, HeavenlyStem.JIA, EarthlyBranch.ZI, false, "三合木局但土当令——不化"),
        C(HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.GUI, EarthlyBranch.CHOU, HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.JIA, EarthlyBranch.ZI, true, "丑令丁巳火生土燥土", Element.EARTH),
        C(HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.MAO, HeavenlyStem.YI, EarthlyBranch.HAI, true, "木局众木成林土光杆——反化木", Element.WOOD),
        C(HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.YOU, HeavenlyStem.JIA, EarthlyBranch.ZI, false, "支会金局己土失根"),
        C(HeavenlyStem.BING, EarthlyBranch.CHEN, HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.YI, EarthlyBranch.HAI, true, "午月火催化剂土强根", Element.EARTH),
        C(HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.JI, EarthlyBranch.HAI, HeavenlyStem.JIA, EarthlyBranch.ZI, false, "午火被众水围克"),
        C(HeavenlyStem.DING, EarthlyBranch.WEI, HeavenlyStem.YI, EarthlyBranch.SI, HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.JI, EarthlyBranch.SI, false, "巳午未会火多土焦"),
        C(HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.SI, false, "寅午戌火太旺土不受生"),
        C(HeavenlyStem.BING, EarthlyBranch.SHEN, HeavenlyStem.JI, EarthlyBranch.HAI, HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.GUI, EarthlyBranch.YOU, false, "三会金局水当令金水两强"),
        C(HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.SI, true, "木火土流通受益者土", Element.EARTH),
        C(HeavenlyStem.JI, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.JI, EarthlyBranch.SI, false, "一片火海无水火多土焦"),
        // 乙庚合化金
        C(HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.WEI, HeavenlyStem.BING, EarthlyBranch.ZI, true, "申月金旺得土生", Element.METAL),
        C(HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.JIA, EarthlyBranch.SHEN, true, "申酉戌会金火炼秋金", Element.METAL),
        C(HeavenlyStem.DING, EarthlyBranch.CHOU, HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.JI, EarthlyBranch.MAO, true, "燥湿得宜诸多利金", Element.METAL),
        C(HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.SI, HeavenlyStem.GUI, EarthlyBranch.YOU, true, "辰酉合金己生金", Element.METAL),
        C(HeavenlyStem.DING, EarthlyBranch.WEI, HeavenlyStem.DING, EarthlyBranch.WEI, HeavenlyStem.GENG, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.YOU, false, "燥土不生金庚受克无生"),
        C(HeavenlyStem.DING, EarthlyBranch.WEI, HeavenlyStem.GUI, EarthlyBranch.CHOU, HeavenlyStem.GENG, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.YOU, true, "丑湿土晦火水火既济", Element.METAL),
        C(HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.HAI, HeavenlyStem.GENG, EarthlyBranch.CHEN, false, "三合水局金泄太重"),
        C(HeavenlyStem.GENG, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.JIA, EarthlyBranch.WU, false, "三午克一酉丙透克化神"),
        C(HeavenlyStem.GENG, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.WU, EarthlyBranch.XU, false, "三合火局克当令金"),
        C(HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.YIN, false, "木火克耗丙隔乙庚"),
        C(HeavenlyStem.YI, EarthlyBranch.WEI, HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.CHEN, false, "土重埋金生多为克"),
        C(HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.CHEN, true, "群金结党寅独木难支", Element.METAL),
        // 丙辛合化水
        C(HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.XIN, EarthlyBranch.CHOU, HeavenlyStem.BING, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.SI, true, "丑助水支会水局癸透", Element.WATER),
        C(HeavenlyStem.JI, EarthlyBranch.HAI, HeavenlyStem.XIN, EarthlyBranch.WEI, HeavenlyStem.BING, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.SI, false, "未月火炎土燥水残兵败将"),
        C(HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.REN, EarthlyBranch.XU, HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.BING, EarthlyBranch.SHEN, true, "戌被金泄壬癸透引通", Element.WATER),
        C(HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.BING, EarthlyBranch.SHEN, true, "三水滋润庚泄土癸纳金", Element.WATER),
        // 丁壬合化木
        C(HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.DING, EarthlyBranch.HAI, HeavenlyStem.REN, EarthlyBranch.YIN, true, "寅月余寒得暖甲透", Element.WOOD),
        C(HeavenlyStem.JI, EarthlyBranch.YOU, HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.CHOU, false, "卯遭申酉夹克"),
        C(HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.GUI, EarthlyBranch.MAO, true, "木势成林火难泄尽", Element.WOOD),
        C(HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.GUI, EarthlyBranch.MAO, true, "申被水泄甲卯结党", Element.WOOD),
        C(HeavenlyStem.BING, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.HAI, HeavenlyStem.REN, EarthlyBranch.XU, HeavenlyStem.DING, EarthlyBranch.WEI, false, "亥被土围克土重木折"),
        C(HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.YIN, true, "两卯合未化木土被克", Element.WOOD),
        C(HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.YIN, true, "卯戌反化木甲双透", Element.WOOD),
        // 戊癸合化火
        C(HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.DING, EarthlyBranch.SI, true, "天合地合旺火卯戌合火", Element.FIRE),
        C(HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.YI, EarthlyBranch.MAO, false, "木太旺火无从引发"),
        C(HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.WU, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.HAI, false, "午被水围克化神被伤"),
        C(HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.JI, EarthlyBranch.YOU, HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.WU, EarthlyBranch.WU, true, "地支火海化神旺透", Element.FIRE),
        C(HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.WU, false, "辛酉双体火局不成无火透"),
        C(HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.REN, EarthlyBranch.XU, false, "土方局晦火无光"),
    )

    @Test fun `三十五例黄金母版`() {
        var pass = 0
        val fails = mutableListOf<String>()
        for ((i, c) in cases.withIndex()) {
            val b = chartOf(c.y, c.yb, c.m, c.mb, c.d, c.db, c.h, c.hb)
            val v = HeHuaJudge.judge(b)
            assertNotNull("例${i + 1} ${c.note}：未检出合干", v)
            val ok = v != null && v.cheng == c.cheng && (c.hs == null || !c.cheng || v.huaShen == c.hs)
            if (ok) pass++ else fails += "例${i + 1} [${c.note}] 期望${if (c.cheng) "化${c.hs?.chinese ?: ""}" else "不化"} 实际${if (v!!.cheng) "化${v.huaShen.chinese}" else "不化"} | ${v.basis}"
        }
        assertEquals("失败${fails.size}/${cases.size}：\n" + fails.joinToString("\n"), cases.size, pass)
    }
}

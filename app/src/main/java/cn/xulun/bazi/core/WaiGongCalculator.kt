package cn.xulun.bazi.core

import cn.xulun.bazi.core.LiuQinXiJi.category
import cn.xulun.bazi.core.LiuQinXiJi.elementOf
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.LiuQinItem
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.WaiGongResult
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.hiddenStemGods
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 外公家族断语引擎（.liuqin-reference.md 第二节）。
 *
 * 理论链：命主与外祖父家以母亲为纽带——母亲即印星，生于外祖父家；生印者官杀为
 * 外祖父；制官杀者以伤官之力最大（比食神负面心性强）。外祖父宫位在年月柱。
 * 归纳：**伤官（伤食）居年月柱且旺而为忌，外祖父家败绝**。
 *
 * 细化：
 * - 生伤官之行有力（≥2.5）——伤官愈肆，外公家或舅家有绝房或残疾之人（例一 寅卯木生之）
 * - 伤食得日支根——不利外祖父家，绝嗣（例二 得日支子水为根）
 * - 印星透干坐年月墓支——舅姨中有早亡之人（例三 甲木坐未墓）
 *
 * 喜忌由 [LiuQinXiJi] 教材口径派生。
 */
object WaiGongCalculator {

    /** 五行之墓（水土同墓于辰）。 */
    private val tombOf = mapOf(
        Element.WOOD to EarthlyBranch.WEI,
        Element.FIRE to EarthlyBranch.XU,
        Element.METAL to EarthlyBranch.CHOU,
        Element.WATER to EarthlyBranch.CHEN,
        Element.EARTH to EarthlyBranch.CHEN,
    )

    fun analyze(bazi: BaziResult): WaiGongResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val d = LiuQinXiJi.derive(bazi)
        val forces = BingYaoCalculator.forces(bazi)

        // 年/月柱十神（干透 + 支藏）
        val yearGods: Set<TenGod> = setOf(tenGodOfStem(day, bazi.yearPillar.stem)) +
            hiddenStemGods(day, bazi.yearPillar.branch).map { it.second }
        val monthGods: Set<TenGod> = setOf(tenGodOfStem(day, bazi.monthPillar.stem)) +
            hiddenStemGods(day, bazi.monthPillar.branch).map { it.second }
        val shiEl = elementOf(GodCategory.SHI, dayEl)

        // 伤食在年月柱：伤官为主，食神辅（例二年月伤食并透）
        val shangGuan = TenGod.SHANG_GUAN in yearGods || TenGod.SHANG_GUAN in monthGods
        val shiShen = TenGod.SHI_SHEN in yearGods || TenGod.SHI_SHEN in monthGods
        // 旺：透年月干 / 月支本气当令 / 力≥3（重重）
        val touGan = bazi.stems.take(2).any { it.element == shiEl }
        val dangLing = branchHiddenStems.getValue(bazi.monthPillar.branch).first().element == shiEl
        val heavy = forces[shiEl]!! >= 3.0

        val items = mutableListOf<LiuQinItem>()
        // 主判：伤官（伤食）居年月柱且旺——机理是伤官制官杀（外祖父星），
        // 不以对日主之喜忌为条件（教材三例均以身强伤食泄秀论，照样败绝）
        val mainFire = (shangGuan || shiShen) && (touGan || dangLing || heavy)
        if (mainFire) {
            items += LiuQinItem(
                if (shangGuan) "伤官年月旺" else "伤食年月旺",
                "母亲为印星，生于外祖父家（生印者官杀为外祖父），伤官制官杀之力最狠——" +
                    (if (shangGuan) "伤官" else "伤食") + "居年月柱（外祖父宫位）而旺，制外祖父星，外祖父家败绝。",
            )
        }

        if (mainFire) {
            // 细化一：生伤官之行有力——绝房残疾
            val shengEl = Element.entries.first { it.generates == shiEl }
            if (forces[shengEl]!! >= 2.5) {
                items += LiuQinItem(
                    "伤官得${shengEl.chinese}生·绝房残疾",
                    "生伤官之${shengEl.chinese}有力，伤官愈肆，外公家或舅家有绝房或残疾之人。",
                )
            }
            // 细化二：伤食得日支根——绝嗣
            if (bazi.dayPillar.branch.element == shiEl) {
                items += LiuQinItem(
                    "伤食得日支根·绝嗣",
                    "伤食得日支${bazi.dayPillar.branch.chinese}为根，不利外祖父家，绝嗣。",
                )
            }
            // 细化三：印星透干坐年月墓支——舅姨早亡
            val yinEl = elementOf(GodCategory.YIN, dayEl)
            val yinTou = bazi.stems.any { it.element == yinEl }
            val tomb = tombOf[yinEl]!!
            if (yinTou && (bazi.yearPillar.branch == tomb || bazi.monthPillar.branch == tomb)) {
                items += LiuQinItem(
                    "印星坐墓·舅姨早亡",
                    "印星（母亲）${yinEl.chinese}透干坐${tomb.chinese}墓地，舅姨中有早亡之人。",
                )
            }
        }

        val summary = if (items.isEmpty()) {
            "年月柱无伤官（伤食）旺而为忌之象，外祖父家无异讯。"
        } else {
            "母亲（印星）为纽带：生印者官杀为外祖父，伤官制之；外祖父宫位在年月柱。" +
                "喜忌口径：${d.basis}。断语${items.size}条。"
        }
        return WaiGongResult(items = items, summary = summary)
    }
}

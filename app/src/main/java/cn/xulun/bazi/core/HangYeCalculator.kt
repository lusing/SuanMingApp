package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HangYeResult
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 行业选择计算器（以性格+用神断行业）。理论见 `.shengke-reference.md` 第六四节。
 *
 * 性格线（引擎信号→性格→行业）：
 * - 比劫党众有力+官弱 → 倔强不服管 → 自由职业（癸未己未戊子癸亥式）
 * - 食伤旺透+身强 → 创新开拓 → 自由职业/技术发明（戊辰辛酉戊辰庚申式）
 * - 从格+刑害多 → 孤傲清高 → 自由职业（丁卯壬子辛卯壬辰式）
 * - 官印相生/身弱用印 → 正直循规 → 公职（己丑辛未壬子庚子式）
 * - 印重身强+食神吐秀 → 奉献淡泊 → 公职/教育（乙卯己卯丙辰甲午式）
 *
 * 用神线（第六四节二）：用神行→行业五行（水木=文教创意/木=园林/火土=工商）
 * + 用神性质（食伤=口才创意开拓——律师/作家；财=工商金融；官印=公职）。
 */
object HangYeCalculator {

    // ── 五行行业表 ──
    private val elFields = mapOf(
        Element.WOOD to "园林、木材、纺织、文教、出版、中医药、设计",
        Element.FIRE to "能源、电力、餐饮、光学、演艺、化工",
        Element.EARTH to "房地产、建筑、农业、仓储、陶瓷、管理",
        Element.METAL to "金融、机械、五金、司法、军警、矿业",
        Element.WATER to "航运、水产、贸易、流动、口才（律师/演讲）、传播",
    )

    fun analyze(bazi: BaziResult): HangYeResult {
        val items = mutableListOf<HangYeResult.HangYeItem>()
        val dayEl = bazi.dayPillar.stem.element
        val f = BingYaoCalculator.forces(bazi)
        val total = Element.entries.sumOf { f[it]!! }.coerceAtLeast(0.001)
        val ts = TeShuGeJuCalculator.analyze(bazi)
        val dayMaster = bazi.dayPillar.stem

        // ── 性格线 ──
        val biDang = bazi.branches.count { it.element == dayEl } + bazi.stems.count { it.element == dayEl } - 1
        val guanEl = Element.entries.first { it.controls == dayEl }
        val biLi = f[dayEl]!!
        val guanLi = f[guanEl]!!
        if (biDang >= 3 && guanLi < 2.0) {
            items += HangYeResult.HangYeItem("倔强不服管束（比劫党众${biDang}而官弱）", "自由职业",
                "比劫重重而有力——讲信用不服输不服管，与领导难合，自谋职业可大展宏图（癸未己未戊子癸亥式）")
        }
        // 食伤创新：食伤行力≥2.5 且透干
        val shangEl = dayEl.generates
        val shangStems = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem).count { it.element == shangEl }
        if (f[shangEl]!! >= 2.5 && shangStems >= 1) {
            items += HangYeResult.HangYeItem("创新开拓（食伤旺透）", "自由职业/技术发明",
                "思维敏捷善于动脑——科技发明/创意行业（戊辰辛酉戊辰庚申式）")
        }
        // 从格孤傲：从格+支刑害≥2组
        if (ts != null) {
            val xingHai = run {
                val bs = bazi.branches
                var n = 0
                for (i in 0..3) for (j in i + 1..3) {
                    val pair = setOf(bs[i], bs[j])
                    if (pair == setOf(cn.xulun.bazi.model.EarthlyBranch.ZI, cn.xulun.bazi.model.EarthlyBranch.MAO) ||
                        pair == setOf(cn.xulun.bazi.model.EarthlyBranch.YIN, cn.xulun.bazi.model.EarthlyBranch.SI) ||
                        pair == setOf(cn.xulun.bazi.model.EarthlyBranch.SI, cn.xulun.bazi.model.EarthlyBranch.SHEN) ||
                        pair == setOf(cn.xulun.bazi.model.EarthlyBranch.CHOU, cn.xulun.bazi.model.EarthlyBranch.XU) ||
                        pair == setOf(cn.xulun.bazi.model.EarthlyBranch.XU, cn.xulun.bazi.model.EarthlyBranch.WEI) ||
                        pair == setOf(cn.xulun.bazi.model.EarthlyBranch.CHOU, cn.xulun.bazi.model.EarthlyBranch.WEI)) n++
                }
                n
            }
            if (xingHai >= 2) {
                items += HangYeResult.HangYeItem("孤傲清高缺乏协作（${ts.name}+刑害${xingHai}组）", "自由职业",
                    "自认人才有志难伸瞧不起别人，集体中不合群（丁卯壬子辛卯壬辰式）")
            }
        }
        // 正直公职：官印相生（官透+印透+身弱用印）
        val yinEl = Element.entries.first { it.generates == dayEl }
        val guanTou = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem).any { it.element == guanEl }
        val yinTou = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem).any { it.element == yinEl }
        if (guanTou && yinTou && biLi < 0.4 * total) {
            items += HangYeResult.HangYeItem("正直循规蹈矩（官印相生身弱）", "公职",
                "做事讲原则一丝不苟贪欲不强（己丑辛未壬子庚子式——公务员）")
        }
        // 奉献公职：印重身强（印主文书教育——卯月印重教育工作者式）
        if (f[yinEl]!! >= 2.5 && biLi >= 2.5) {
            items += HangYeResult.HangYeItem("乐于奉献淡泊名利（印重身强食神吐秀）", "公职/教育",
                "甘愿奉献任劳任怨（乙卯己卯丙辰甲午式——教育工作者）")
        }

        // ── 用神线 ──
        val by = BingYaoCalculator.analyze(bazi)
        val yongEl = by.yongShenElement
        val godName = when (by.yongShen) {
            cn.xulun.bazi.model.GodCategory.BI -> "比劫（帮身）"
            cn.xulun.bazi.model.GodCategory.YIN -> "印绶（文书权力）"
            cn.xulun.bazi.model.GodCategory.SHI -> "食伤（口才创意开拓）"
            cn.xulun.bazi.model.GodCategory.CAI -> "财星（工商金融）"
            cn.xulun.bazi.model.GodCategory.GUAN -> "官杀（管理公职）"
        }
        val fieldNote = when (by.yongShen) {
            cn.xulun.bazi.model.GodCategory.SHI -> "——食伤主口才，表现为创意、开拓（律师/作家/演艺）"
            cn.xulun.bazi.model.GodCategory.CAI -> "——财星主经营，工商金融贸易"
            cn.xulun.bazi.model.GodCategory.GUAN -> "——官星主管理，国家公务员/行政干部"
            cn.xulun.bazi.model.GodCategory.YIN -> "——印星主文书学术，教育/文职"
            cn.xulun.bazi.model.GodCategory.BI -> "——比劫主自立，独自闯天下"
        }
        items += HangYeResult.HangYeItem(
            trait = "用神${godName}（${yongEl.chinese}行）",
            field = elFields[yongEl] ?: "",
            basis = "用神${yongEl.chinese}行——对命主有益的行业（若未进入此行业可参考调整）$fieldNote",
        )

        return HangYeResult(items)
    }
}

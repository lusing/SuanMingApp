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
        if (f[yinEl]!! >= 2.0 && biLi >= 2.0) {
            items += HangYeResult.HangYeItem("乐于奉献淡泊名利（印重身强食神吐秀）", "公职/教育",
                "甘愿奉献任劳任怨（乙卯己卯丙辰甲午式——教育工作者）")
        }

        // ── 十神心性线（第六四节三：正官行政/七杀公安/财经商/伤官投机/食伤艺术/杀印技术/官重手艺）──
        val touGan = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
        val dayGods = touGan.map { tenGodOfStem(dayMaster, it) }
        val shenSha = ShenShaCalculator.analyze(bazi).items.map { it.name }.toSet()
        // ①正官透+无伤克→行政（例一市长式）
        if (dayGods.any { it == TenGod.ZHENG_GUAN } && dayGods.none { it == TenGod.SHANG_GUAN } && guanLi < 1.5 * biLi) {
            items += HangYeResult.HangYeItem("心性正直甘于奉献（正官透干无伤克）", "行政工作（从政）",
                "四柱组合得当天干露正官——为人正直贪欲不强，适宜行政管理工作（丙申庚寅丁未壬寅——某市市长式）")
        }
        // ②七杀双透→公安法制（例二法院科长式）
        val shaCount = dayGods.count { it == TenGod.QI_SHA } + bazi.branches.count { it.element == guanEl && bazi.stems.count { st -> st.element == guanEl } >= 2 }
        if (dayGods.count { it == TenGod.QI_SHA } >= 2) {
            items += HangYeResult.HangYeItem("做事有威信攻击性强意志坚强（七杀双透）", "公安法制部门（从政）",
                "双杀透天干——适合公安法制纪检军警（甲辰丁卯戊辰甲寅——法院科长式）")
        }
        // ③从财官+财官喜用→企业官员（例三银行厅级式）
        if (ts != null && ts.yongShen.any { it == guanEl } && ts.yongShen.any { it == dayEl.controls }) {
            items += HangYeResult.HangYeItem("财旺生官官能受益（从财官格）", "企业方面的官员",
                "假从财官财官两旺——财官为喜用神（癸巳乙卯辛巳辛卯——银行厅级干部式）")
        }
        // ④身旺+财当令/偏财透→经商（例四五式）
        val caiEl = dayEl.controls
        val caiTou = dayGods.filter { it == TenGod.PIAN_CAI || it == TenGod.ZHENG_CAI }
        if (biLi >= 2.0 && (f[caiEl]!! >= 3.0 || caiTou.isNotEmpty()) ) {
            items += HangYeResult.HangYeItem("身旺能胜财（财当令或偏财透有力）", "经商",
                "身旺财当令偏财透干有力——商人命造（丙午癸巳壬申辛亥式）" +
                    (if (shangStems >= 1) "；食伤生财敢于冒险（丁未戊申丙寅乙未——餐饮女老板式）" else ""))
        }
        // ⑤伤官当令而旺+财贴→投机智慧行业（例五证券操盘手式）
        val shangGan = tenGodOfStem(dayMaster, bazi.monthPillar.stem)
        if (f[shangEl]!! >= 3.5 && (shangGan == TenGod.SHANG_GUAN || bazi.monthPillar.branch.element == shangEl)) {
            items += HangYeResult.HangYeItem("有经济头脑富于开拓冒险（伤官当令而旺）", "投机性和需用智慧的行业",
                "伤官当令而旺+财星紧贴日干，逢喜用神当旺之运能发财（甲寅丙寅癸未壬戌——证券操盘手式）；" +
                    "伤官无制易走偏门有财难守，得制转向经商（乙巳庚辰甲午己巳式）")
        }
        // ⑥杀印相生→专业技术（例八外科医生式）
        if (yinTou && (guanTou || bazi.branches.any { it.element == guanEl }) && f[guanEl]!! >= 2.0 && f[yinEl]!! >= 2.0) {
            items += HangYeResult.HangYeItem("杀印相生格局清纯（名气远扬）", "专业技术职业",
                "杀靠印化格局清纯名气远扬——技术方面职业（壬辰辛亥乙酉己卯——大医院知名外科医生式）")
        }
        // ⑦身旺食伤泄秀→演讲艺术（例九音乐教师式）
        if (biLi >= 3.0 && f[shangEl]!! >= 2.5 && shangStems >= 1) {
            items += HangYeResult.HangYeItem("食伤泄秀主艺术口才（身旺泄秀）", "演讲歌舞艺术/教育行业",
                "身旺伤食泄秀——食神指艺术口才，宜演讲歌舞艺术行业（戊戌辛酉戊戌戊午——中学音乐教师式）")
        }
        // ⑧身弱官杀重+微根→手艺糊口（例十一铝厂工人式）
        if (f[guanEl]!! >= 3.0 && guanLi >= 1.5 * biLi && bazi.branches.count { it.element == guanEl } >= 3) {
            items += HangYeResult.HangYeItem("奔波操劳性格内向（身弱官杀制身过重）", "手艺技术糊口",
                "身弱官杀制身过重奔波操劳性格内向懦弱，以手艺糊口（丁亥壬子丙子癸巳——铝制品厂工人式）")
        }
        // ⑨身弱官星清透无伤→清闲受人尊重（例十医生式）
        if (biLi < 3.5 && dayGods.any { it == TenGod.ZHENG_GUAN } && dayGods.none { it == TenGod.SHANG_GUAN } &&
            guanLi < 2.0 * biLi && bazi.branches.count { it.element == guanEl } < 3) {
            items += HangYeResult.HangYeItem("做事循规蹈矩安于现状（身弱官星清透）", "清闲受人尊重的职业",
                "身弱官星清透——做事循规蹈矩安于现状，宜清闲受人尊重的职业（癸卯辛酉甲申甲子——医生式）")
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

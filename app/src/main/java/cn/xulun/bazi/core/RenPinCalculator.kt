package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.RenPinItem
import cn.xulun.bazi.model.RenPinResult
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 人品推断引擎（十神心性与五行特性复合——正负面分判，方法同性格推断而侧重人品）。
 *
 * 正面：正印透干无财克（正直仁慈）；偏印有力不遭克（精明能干）；伤官藏支不透（聪明内秀沉稳）；
 * 伤官佩印（能干不狂妄精明无邪念——德才兼备）；七杀强旺透干（威信魄力原则性强）；
 * 土强旺（守信用沉稳）；金多（豪爽）；火强（热情有礼）。
 * 负面：食神太强（原则性传统观念差——随心所欲不计后果一意孤行）；食伤多而官印弱（放荡不羁我行我素
 * 喜说人是非）；官星被制无印（原则性纪律性不强）；偏印过旺（精明怪异鬼点子多）；
 * 天干一片食伤水（放荡不羁目无法纪狡猾善变）；木多克土土弱（缺乏信用）；土过旺（倔强顽固）。
 * 岁运改变：官杀旺运原则性增强；食伤运先天本性暴露；官杀过旺+印星入墓运——仁慈淡化霸气加重（例六）。
 */
object RenPinCalculator {

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?): RenPinResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val f = BingYaoCalculator.forces(bazi)
        val shiEl = dayEl.generates
        val caiEl = dayEl.controls
        val guanEl = Element.entries.first { it.controls == dayEl }
        val yinEl = Element.entries.first { it.generates == dayEl }

        fun tou(target: TenGod) = bazi.stems.indices.filter { it != 2 }.any { tenGodOfStem(day, bazi.stems[it]) == target }
        fun benQiZhi(target: TenGod) = bazi.branches.any {
            branchHiddenStems.getValue(it).firstOrNull()?.let { tenGodOfStem(day, it) == target } == true
        }
        val zhengYinTou = tou(TenGod.ZHENG_YIN)
        val caiTou = tou(TenGod.ZHENG_CAI) || tou(TenGod.PIAN_CAI)
        val shaTou = tou(TenGod.QI_SHA) || tou(TenGod.ZHENG_GUAN)
        val pianCaiTou = tou(TenGod.PIAN_CAI)
        val shangBenQi = benQiZhi(TenGod.SHANG_GUAN)
        val shangTou = tou(TenGod.SHANG_GUAN)
        // 水食伤一片（天干透≥3 而食伤行为水）
        val shuiShiShangYiPian = shiEl == Element.WATER &&
            bazi.stems.indices.filter { it != 2 }
                .count { tenGodOfStem(day, bazi.stems[it]) in setOf(TenGod.SHI_SHEN, TenGod.SHANG_GUAN) } >= 3

        val items = mutableListOf<RenPinItem>()
        var good = 0
        var bad = 0
        fun add(title: String, text: String, fav: Boolean) {
            items += RenPinItem(title, text, fav)
            if (fav) good++ else bad++
        }

        if (zhengYinTou && !caiTou) {
            add("正印透干无财克", "正印（${yinEl.chinese}）透干而天干无财克印——正直而仁慈，人品端正（例一孙先生式）。", true)
        }
        if (f[yinEl]!! >= 4.5 && !zhengYinTou) {
            add("偏印过旺", "偏印强旺（${yinEl.chinese}力${f[yinEl]}）——精明而怪异，鬼点子多（例五蒋先生式）。", false)
        }
        if (shangBenQi && !shangTou && zhengYinTou) {
            add("伤官佩印·德才兼备", "伤官藏支不透（聪明内秀${shiEl.chinese}主沉稳）与正印同构伤官佩印贵格——能干而不狂妄，精明而无邪念，热情有礼而又正直仁慈——德才兼备（例一孙先生式，官至厅级）。", true)
        }
        if (shaTou && f[guanEl]!! >= 3.0) {
            add("七杀强旺·威信魄力", "官杀（${guanEl.chinese}力${f[guanEl]}）透干强旺——有威信和魄力，原则性强，具领导气质与号召力（例六鸿先生式）。", true)
        }
        if (f[Element.EARTH]!! >= 5.0) {
            add("土过旺", "土太多太强（力${f[Element.EARTH]}）——脾气倔强，顽固不化（例五式）。", false)
        } else if (f[Element.EARTH]!! >= 3.0) {
            add("土强·守信沉稳", "土强旺（力${f[Element.EARTH]}）——守信用，做事沉稳（例六式）。", true)
        }
        if (f[Element.METAL]!! >= 3.0) {
            add("金多·豪爽", "金多——为人豪爽（例六式）。", true)
        }
        if (f[Element.FIRE]!! >= 2.0) {
            add("火强·热情有礼", "火强（${dayEl.chinese}日主尤显）——对人热情而礼貌（例一/例三式）。", true)
        }
        if (f[Element.WOOD]!! >= 3.0 && f[Element.EARTH]!! <= 0.5) {
            add("木多克土·缺信用", "木多克土而土太弱（木${f[Element.WOOD]}土${f[Element.EARTH]}）——缺乏信用（例四庆女士式）。", false)
        }
        if (shuiShiShangYiPian) {
            add("水食伤一片", "天干一片食伤（水）——行为放荡不羁，目无法纪，狡猾善变（例五蒋先生式）。", false)
        }
        if (f[shiEl]!! >= 4.0) {
            add("食神太强·负面", "食伤（${shiEl.chinese}力${f[shiEl]}）太强显现负面心性——原则性和传统观念很差，凡事随心所欲不计后果，不听劝告一意孤行；表面随和，关键时为自身利益可出卖朋友（例二科先生式）。", false)
        }
        if (f[shiEl]!! >= 3.0 && f[guanEl]!! <= 1.0 && f[yinEl]!! <= 1.0) {
            add("食伤多而官印弱", "食伤多而强（力${f[shiEl]}）而官印太弱——聪明想法多、渴望精神满足；行为放荡不羁我行我素出人意料，喜说人是非长短" +
                (if (pianCaiTou) "；食神生财偏财透干——求财之心强烈（例四庆女士式）" else "") + "。", false)
        }
        if (shaTou && pianCaiTou && (tou(TenGod.SHI_SHEN) || shangTou || benQiZhi(TenGod.SHI_SHEN))) {
            add("杀财食皆透", "七杀、偏财与食神皆透有力——为钱财会以各种手段去挣且不计后果、任性放纵；惟火热情礼土耿直义气——豪爽慷慨重情谊，乐于帮朋友但自律差原则性不强（例三文先生式）。", false)
        }

        val verdict = when {
            bad == 0 && good >= 2 -> "人品优秀（德才兼备——正直仁慈，能干而不狂妄）"
            good > bad -> "人品良好"
            bad == 0 && good < 2 -> "人品一般"
            bad >= 3 || (bad >= 2 && shuiShiShangYiPian) -> "人品很差（先天品质很坏——如不加强教育引导，容易步入歧途）"
            bad > good -> "人品较差"
            else -> "人品一般（正负面并存）"
        }
        val basis = items.joinToString("；") { it.title }

        // 岁运改变
        val yunParts = mutableListOf<String>()
        daYun?.yuns?.forEach { yun ->
            val yEl = yun.pillar.branch.element
            val guanWang = yEl == guanEl && f[guanEl]!! >= 3.0
            val yinMu = mapOf(
                Element.METAL to EarthlyBranch.CHOU, Element.WOOD to EarthlyBranch.WEI,
                Element.WATER to EarthlyBranch.CHEN, Element.FIRE to EarthlyBranch.XU,
                Element.EARTH to EarthlyBranch.CHEN,
            )[yinEl] == yun.pillar.branch
            val shiYun = yun.pillar.branch.element == shiEl
            when {
                guanWang && yinMu ->
                    yunParts += "${yun.pillar.fullName}运官杀过旺而印星入墓——仁慈之心被淡化，霸气加重，正气下降邪气上升，容易步入歧途称王称霸；过此运则改（例六乙丑式）"
                yEl == guanEl ->
                    yunParts += "${yun.pillar.fullName}运官星旺——原则性、纪律性增强（例二式）"
                shiYun && f[shiEl]!! >= 3.0 ->
                    yunParts += "${yun.pillar.fullName}运食伤居主导地位——先天本性充分暴露，劣根性显（例二癸未式）"
            }
        }

        val advice = when {
            items.any { it.title.contains("杀财食") } ->
                "不宜当领导或从事管理、财务、司法等原则性很强的工作，只宜作普通工作人员；交朋友有难必帮、尽己所能，但托其办事千万不可有违原则纪律，否则为帮你而违法违纪反害之（例三式）"
            verdict.contains("优秀") || verdict.contains("良好") ->
                "正直之士与善良之人都乐于亲近——仕途较顺能做出政绩；与之交友不吃亏，遇事他会帮你且很守信用（例六式）" +
                    (if (yunParts.any { it.contains("霸气") }) "；惟霸气重之运不可助长其霸气、更不可与之一起以强欺弱，否则惹火烧身（例六乙丑运式）" else "")
            verdict.contains("很差") ->
                "宜保持足够距离、坚持原则，千万不能同流合污；不能说服则敬而远之（例五式）"
            verdict.contains("较差") && items.any { it.title.contains("食神太强") } ->
                "不能拜托他做重要的事，更不能委以重任；作朋友随和，但危险关键时不可靠——会为自身利益出卖朋友（例二式）"
            verdict.contains("较差") ->
                "不宜深交——尤其不可托付钱财与机要（例四式：娶之须有足够钱财供花用且要浪漫，否则易生外遇离异）"
            else -> "常人相处之道——观其言行而信其常"
        }

        return RenPinResult(
            verdict = verdict,
            basis = basis,
            items = items,
            yunNote = yunParts.joinToString("；"),
            advice = advice,
        )
    }
}

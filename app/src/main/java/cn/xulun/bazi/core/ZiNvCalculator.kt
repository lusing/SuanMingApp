package cn.xulun.bazi.core

import cn.xulun.bazi.core.LiuQinXiJi.category
import cn.xulun.bazi.core.LiuQinXiJi.elementOf
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.LiuQinItem
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.ZiNvResult
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 子女断语引擎（.liuqin-reference.md 第五节）。子女星：男七杀=子/正官=女，
 * 女伤官=子/食神=女；**子女宫=时柱（时支）**。
 *
 * 关键技法：
 * - **头胎性别**——时干与日干阴阳：男命同性=子/异性=女，女命反是
 * - **宫位阴阳**——时支本气与日干同性→儿女双全之象；异性→多生女（男）/多生男（女）
 * - **数目**——星数（干透+本气支）×临月令加倍×得生之源（力≥2）再加倍（卯官1×2×2=4女）
 * - **从象**——子女星衰弱（力≤1.5）无本气根→从得真，从则吉，子女有出息、享儿女之福
 * - **宫星刑冲空**——时支落空遭冲→堕胎流产信息；子星透干被克星透旺→克子；时支临驿马逢冲刑→远离父母
 *
 * 岁运应期（早子/晚子/得子时机/亡子年份）属动态批断，不在本引擎范围。
 * 喜忌由 [LiuQinXiJi] 教材口径派生。
 */
object ZiNvCalculator {

    fun analyze(bazi: BaziResult, gender: Gender): ZiNvResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val d = LiuQinXiJi.derive(bazi)
        val forces = BingYaoCalculator.forces(bazi)
        val bing = BingYaoCalculator.analyze(bazi)
        val diZhiRel = DiZhiRelationCalculator.analyze(bazi)
        val kong = KongWangCalculator.analyze(bazi)
        val ssItems = ShenShaCalculator.analyze(bazi).items

        val male = gender == Gender.MALE
        val sonGod = if (male) TenGod.QI_SHA else TenGod.SHANG_GUAN
        val dauGod = if (male) TenGod.ZHENG_GUAN else TenGod.SHI_SHEN
        val starCat = if (male) GodCategory.GUAN else GodCategory.SHI
        val starEl = elementOf(starCat, dayEl)
        val starForce = forces[starEl]!!

        /** 星透干柱位（跳日干）。 */
        fun stemPillars(god: TenGod): List<Int> =
            bazi.stems.indices.filter { it != 2 && tenGodOfStem(day, bazi.stems[it]) == god }

        /** 星本气根柱位。 */
        fun benQiPillars(god: TenGod): List<Int> =
            bazi.branches.indices.filter {
                branchHiddenStems.getValue(bazi.branches[it]).firstOrNull()
                    ?.let { h -> tenGodOfStem(day, h) } == god
            }

        // ── 头胎性别：时干与日干阴阳 ──
        val hourStem = bazi.stems[3]
        val samePolarity = hourStem.yang == day.yang
        val headFirst = if (samePolarity) {
            if (male) "头胎生子（时干${hourStem.chinese}与日干${day.chinese}阴阳同性）"
            else "头胎生女（时干${hourStem.chinese}与日干${day.chinese}阴阳同性）"
        } else {
            if (male) "头胎生女（时干${hourStem.chinese}与日干${day.chinese}阴阳异性）"
            else "头胎生男（时干${hourStem.chinese}与日干${day.chinese}阴阳异性）"
        }

        // ── 宫位阴阳：时支本气与日干 ──
        val hourBenQi = branchHiddenStems.getValue(bazi.branches[3]).first()
        val palaceSame = hourBenQi.yang == day.yang
        val palaceNote = if (palaceSame) {
            "子女宫（时支${bazi.branches[3].chinese}本气${hourBenQi.chinese}）与日干阴阳同性——儿女双全之象"
        } else {
            if (male) "子女宫（时支${bazi.branches[3].chinese}）与日干阴阳异性——多生女儿"
            else "子女宫（时支${bazi.branches[3].chinese}）与日干阴阳异性——多生男儿"
        }

        // ── 子女数目 ──
        val occurrences = stemPillars(sonGod).size + stemPillars(dauGod).size +
            benQiPillars(sonGod).size + benQiPillars(dauGod).size
        val countBasis = StringBuilder("星数${occurrences}（干透${stemPillars(sonGod).size + stemPillars(dauGod).size}+本气支${benQiPillars(sonGod).size + benQiPillars(dauGod).size}）")
        var n = occurrences
        if (occurrences > 0) {
            if (bazi.monthPillar.branch.element == starEl) {
                n *= 2
                countBasis.append("；临月令（${bazi.monthPillar.branch.chinese}）强旺加倍→${n}")
            }
            val shengEl = Element.entries.first { it.generates == starEl }
            if (forces[shengEl]!! >= 2.0) {
                n *= 2
                countBasis.append("；得${shengEl.chinese}生之源（力${forces[shengEl]}）再加倍→${n}")
            }
        } else {
            countBasis.append("；子女星不现——须岁运助星方得子")
        }

        // ── 断语 ──
        val fav = mutableListOf<LiuQinItem>()
        val unfav = mutableListOf<LiuQinItem>()
        val hourStemGod = tenGodOfStem(day, hourStem)
        val hourBenGod = tenGodOfStem(day, hourBenQi)
        val hourBranch = bazi.branches[3]
        val hourChong = diZhiRel.chongList.any { it.location.contains("时支") }
        val hourXing = diZhiRel.xingList.any { it.location.contains("时支") }
        val hourKong = kong.isKong(3)

        // 从象：衰弱无本气根→从得真，从则吉
        val starBenQiRoot = bazi.branches.any { br ->
            branchHiddenStems.getValue(br).firstOrNull()?.element == starEl
        }
        val cong = starForce <= 1.5 && !starBenQiRoot
        if (cong) {
            fav += LiuQinItem(
                "子女星从旺",
                "子女星（${starEl.chinese}）衰弱无本气根，从得真——从则吉：" +
                    if (male) "女儿聪明伶俐必有出息，能享女儿之福；子星同从亦不为忌" else "子女有出息，能享儿女之福",
            )
        }

        // 子女星旺而为用
        if (starForce >= 2.5 && starCat in d.fav) {
            fav += LiuQinItem("子息星旺为用", "子息星旺而为用——子女聪明俊秀，长大成才。")
        }
        // 用神居时柱
        if (hourStemGod.category() in d.fav || hourBenGod.category() in d.fav) {
            fav += LiuQinItem("用神居时柱", "用神居时柱（子女宫）——可享儿女之福。")
        }
        // 贵人临时
        val guiAtHour = ssItems.any { it.name in setOf("天乙贵人", "天德贵人", "月德贵人") && it.pillarLabel.startsWith("时") }
        if (guiAtHour && hourStemGod.category() !in d.unf) {
            fav += LiuQinItem("贵人居时柱", "天乙/天德/月德贵人居时上为喜用——子女成龙成凤，出入近贵，光宗耀祖。")
        }
        // 马星临时柱
        val maAtHour = ssItems.any { it.name == "驿马" && it.pillarLabel.startsWith("时") }
        if (maAtHour) {
            fav += LiuQinItem("马星临时柱", "马星临时柱——子女出外创业，多远行发展。")
        }
        // 时临官印/伤财喜用
        if ((hourStemGod.category() == GodCategory.GUAN || hourStemGod.category() == GodCategory.YIN ||
                hourBenGod.category() == GodCategory.GUAN || hourBenGod.category() == GodCategory.YIN) &&
            (hourStemGod.category() in d.fav || hourBenGod.category() in d.fav)
        ) {
            fav += LiuQinItem("时临官印·喜用", "时临官印为喜用——子女多贵。")
        }
        if ((hourStemGod.category() == GodCategory.SHI || hourStemGod.category() == GodCategory.CAI ||
                hourBenGod.category() == GodCategory.SHI || hourBenGod.category() == GodCategory.CAI) &&
            (hourStemGod.category() in d.fav || hourBenGod.category() in d.fav)
        ) {
            fav += LiuQinItem("时临伤财·喜用", "时临伤财为喜用——子女多富。")
        }
        // 时临桃花伤食喜用
        val taoAtHour = ssItems.any { it.name == "桃花" && it.pillarLabel.startsWith("时") }
        if (taoAtHour && GodCategory.SHI in d.fav) {
            fav += LiuQinItem("时临桃花伤食", "时临桃花伤食为喜用——子女聪明多艺，秀气貌美。")
        }
        // 杀旺得食制 / 伤旺得财化——子多且贤（男）
        if (male && starForce >= 2.5 && forces[elementOf(GodCategory.SHI, dayEl)]!! >= 1.5) {
            fav += LiuQinItem("杀旺得食制", "杀旺得食制而为用——子多且贤；身旺七杀有制，当生麒麟之子。")
        }
        // 时上偏官得制
        if (hourStemGod == TenGod.QI_SHA && forces[elementOf(GodCategory.SHI, dayEl)]!! >= 1.5) {
            fav += LiuQinItem("时上偏官得制", "时上偏官（七杀）得制——晚生聪俊之子。")
        }
        // 女伤食旺喜用——子多孝顺
        if (!male && starForce >= 2.5 && GodCategory.SHI in d.fav) {
            fav += LiuQinItem("女命伤食旺喜用", "女命伤食旺而为喜用——子多而孝顺。")
        }

        // ══ 不利 ══

        // 伤食叠叠旺忌——难有子息（喜忌路径，或子星无气被伤食克路径——丁未甲辰丁巳甲辰土旺水绝式）
        val shiEl = elementOf(GodCategory.SHI, dayEl)
        if (forces[shiEl]!! >= 3.0 && (GodCategory.SHI in d.unf || starForce <= 0.5)) {
            unfav += LiuQinItem(
                "伤食叠叠旺",
                "伤官食神叠叠旺（力${forces[shiEl]}）" +
                    (if (starForce <= 0.5) "而子女星（${starEl.chinese}）无气遭克" else "而为忌狂泄日主") +
                    "——难有子息，纵得亦难养。",
            )
        }
        // 子星透干被克星透旺——克子（朱女士 枭印双透克乙）
        val keStarEl = Element.entries.first { it.controls == starEl }
        if (stemPillars(sonGod).isNotEmpty() && forces[keStarEl]!! >= 2.0 * starForce) {
            unfav += LiuQinItem(
                "子星被克",
                "子星${bazi.stems[stemPillars(sonGod).first()].chinese}透干而克星（${keStarEl.chinese}行力${forces[keStarEl]}）透旺克之" +
                    if (!male) "（枭印夺食）——必主克子，子息难成" else "——克子，子息有损",
            )
        }
        // 子女宫落空遭冲——堕胎流产
        if (hourKong && hourChong) {
            unfav += LiuQinItem("宫空遭冲", "时支（子女宫）落空亡又逢冲——原局有不利子息信息，防堕胎流产。")
        } else if (hourKong) {
            unfav += LiuQinItem("宫落空亡", "时柱（子女宫）落空亡——子女缘薄，有子难留（有刑克）。")
        }
        // 子星衰处死墓绝
        val starTomb = LiuQinXiJi.tombOf[starEl]!!
        val starJue = LiuQinXiJi.jueOf[starEl]!!
        if (starForce <= 1.0 && !cong && (bazi.branches.contains(starTomb) || bazi.branches.contains(starJue))) {
            unfav += LiuQinItem("子星衰处死墓绝", "子星衰而处墓绝之地——有子难养或子息少。")
        }
        // 子女星衰入墓时支
        if (hourBranch == starTomb && starForce <= 1.5 && !cong) {
            unfav += LiuQinItem("星衰入墓时支", "子女星衰而入墓于时支——子女多病难养。")
        }
        // 时带伤官旺忌
        if ((hourStemGod == TenGod.SHANG_GUAN || hourBenGod == TenGod.SHANG_GUAN) &&
            GodCategory.SHI in d.unf && forces[shiEl]!! >= 2.5
        ) {
            unfav += LiuQinItem("时带伤官旺忌", "时带伤官旺而为忌——子女凶顽不肖。")
        }
        // 女身旺印旺忌
        if (!male && forces[elementOf(GodCategory.YIN, dayEl)]!! >= 3.0 &&
            GodCategory.YIN in d.unf && bing.strength == DayStrength.STRONG
        ) {
            unfav += LiuQinItem("女身旺印旺忌", "女命身旺印旺为忌——不利子息（枭印夺食）。")
        }
        // 男七杀居年时旺克身忌——至老无儿
        if (male && (stemPillars(TenGod.QI_SHA).contains(0) || stemPillars(TenGod.QI_SHA).contains(3)) &&
            starForce >= 2.5 && starCat in d.unf
        ) {
            unfav += LiuQinItem("七杀居年时旺忌", "男命七杀居年/时柱旺而克身为忌——至老无儿之象。")
        }
        // 时坐凶星为忌
        if (hourStemGod == TenGod.QI_SHA && starCat in d.unf) {
            unfav += LiuQinItem("时坐七杀忌", "时坐七杀为忌——子女凶狠性暴，且多不孝。")
        }
        if ((hourStemGod == TenGod.PIAN_YIN || hourStemGod == TenGod.SHANG_GUAN) &&
            hourStemGod.category() in d.unf && hourStemGod.category() != starCat
        ) {
            unfav += LiuQinItem("时坐枭伤忌", "时坐枭神/伤官为忌——子女性不良。")
        }
        if (hourStemGod.category() == GodCategory.BI && GodCategory.BI in d.unf) {
            unfav += LiuQinItem("时坐比劫忌", "时坐比劫为忌——子女耗财败家。")
        }
        // 日时支冲——在外奔走
        if (hourChong) {
            unfav += LiuQinItem("日时支冲", "日时支相冲——子女在外奔走，聚少离多，有刑克之象。")
        }
        // 宫临驿马逢冲刑——远离父母
        if (maAtHour && (hourChong || hourXing)) {
            unfav += LiuQinItem("宫临驿马逢冲刑", "子女宫临驿马又逢冲刑——冲者走也，子当远离父母，多出外发展。")
        }

        val summary = "子星${sonGod.chinese}、女星${dauGod.chinese}（${starEl.chinese}行，力${starForce}），" +
            "子女宫=时柱（${bazi.branches[3].chinese}）。${palaceNote}。喜忌口径：${d.basis}。" +
            "青中年子女宫逢冲之年有堕胎流产信息（岁运层面）。"
        return ZiNvResult(
            headFirst = headFirst,
            count = "${n}个左右",
            countBasis = countBasis.toString(),
            palaceNote = palaceNote,
            favorable = fav,
            unfavorable = unfav,
            summary = summary,
        )
    }
}

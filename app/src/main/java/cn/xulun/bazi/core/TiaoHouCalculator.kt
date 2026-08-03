package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.Climate
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.TiaoHouFormula
import cn.xulun.bazi.model.TiaoHouResult

/**
 * 调候层（《子平真诠》第十四章 论用神配气候得失）。逐字移植自 xuanzong `utils/bazi/tiaohou.js`。
 *
 * 月令→寒暖燥湿→调候用神(寒火/燥水)→调候口诀(金水伤官喜见官 等)→调候为急(override 常规得失)。
 *
 * 纪律：寒/燥不对称——urgent 只取「无火/无水」之极态，「有而弱」不入 urgent、仅作一般气候注。
 */
object TiaoHouCalculator {

    private val WINTER = setOf(EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU)   // 亥子丑——寒
    private val SUMMER = setOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI)     // 巳午未——燥
    private val SPRING = setOf(EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN)  // 寅卯辰
    private val AUTUMN = setOf(EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU)   // 申酉戌

    private val FIRE_STEMS = setOf(HeavenlyStem.BING, HeavenlyStem.DING)                  // 丙丁
    private val FIRE_BRANCHES = setOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI) // 巳午未
    private val WATER_STEMS = setOf(HeavenlyStem.REN, HeavenlyStem.GUI)                   // 壬癸
    private val WATER_BRANCHES = setOf(EarthlyBranch.ZI, EarthlyBranch.HAI)               // 子亥

    /** 金水伤官喜见官——金日冬月（金寒水冷）。供用神成败「伤官见官」之 override（单一真源）。 */
    fun isJinShuiShangGuan(dayMaster: HeavenlyStem, monthBranch: EarthlyBranch): Boolean =
        dayMaster.element == Element.METAL && monthBranch in WINTER

    /** 调候用神(火/水)相对日主的十神角色——供「调候为急」喜神标注。 */
    fun tiaoHouXiRole(dayMaster: HeavenlyStem, tiaoHouEl: Element): String {
        val dayEl = dayMaster.element
        return when {
            tiaoHouEl == dayEl -> "比劫"
            dayEl.generates == tiaoHouEl -> "食伤" // 我生
            dayEl.controls == tiaoHouEl -> "财"     // 我克
            tiaoHouEl.controls == dayEl -> "官杀"   // 克我
            else -> "印" // 生我
        }
    }

    fun analyze(bazi: BaziResult): TiaoHouResult {
        val dayMaster = bazi.dayPillar.stem
        val monthBranch = bazi.monthPillar.branch
        val dayEl = dayMaster.element

        // —— 气候判定（月令为主）——
        val climate: Climate
        val climateBasis: String
        when {
            monthBranch in WINTER -> {
                climate = Climate.HAN
                climateBasis = "生于冬月（${monthBranch.chinese}）水当令、金寒水冷"
            }
            monthBranch in SUMMER -> {
                climate = Climate.ZAO
                climateBasis = "生于夏月（${monthBranch.chinese}）火当令、火炎土燥"
            }
            else -> {
                climate = Climate.WEN_HE
                climateBasis = "生于${if (monthBranch in SPRING) "春" else "秋"}月（${monthBranch.chinese}），气候温和"
            }
        }

        // 柱中水火（参）
        val hasFireStem = bazi.stems.any { it in FIRE_STEMS }
        val hasFireBranch = bazi.branches.any { it in FIRE_BRANCHES }
        val hasFire = hasFireStem || hasFireBranch
        val hasWaterStem = bazi.stems.any { it in WATER_STEMS }
        val hasWaterBranch = bazi.branches.any { it in WATER_BRANCHES }
        val hasWater = hasWaterStem || hasWaterBranch

        // —— 调候用神（寒→火暖、燥→水润）——
        val tiaoHouEl: Element? = when (climate) {
            Climate.HAN -> Element.FIRE
            Climate.ZAO -> Element.WATER
            Climate.WEN_HE -> null
        }
        val tiaoHouPresent = when (tiaoHouEl) {
            Element.FIRE -> hasFire
            Element.WATER -> hasWater
            else -> false
        }
        val tiaoHouNote: String = when {
            tiaoHouEl == null -> "气候温和，无迫切调候之需"
            tiaoHouPresent -> {
                val where = mutableListOf<String>()
                if (tiaoHouEl == Element.FIRE) {
                    if (hasFireStem) where += "丙丁透干"
                    if (hasFireBranch) where += "巳午未支"
                } else {
                    if (hasWaterStem) where += "壬癸透干"
                    if (hasWaterBranch) where += "亥子支"
                }
                "${climate.chinese}需${tiaoHouEl.chinese}调候，柱中${where.joinToString("、")}得调候之神，气候得宜"
            }
            else -> "${climate.chinese}需${tiaoHouEl.chinese}调候，然柱中无${tiaoHouEl.chinese}，调候用神不现"
        }

        // —— 调候为急：寒极无火 / 燥极无水（非从格；从格顺势不调候）——
        val congGe = GeJuCalculator.detectCongGe(bazi)
        var urgent = false
        var urgentDesc = ""
        if (tiaoHouEl != null && !tiaoHouPresent && congGe == null) {
            urgent = true
            urgentDesc = if (climate == Climate.HAN) {
                "生于冬月而四柱全无火（金寒水冷土冻、万物不发），急需火(丙丁巳午)暖局调候——调候为急，常规得失皆当让位"
            } else {
                "生于夏月而四柱全无水（火炎土燥、万物枯槁），急需水(壬癸亥子)润局调候——调候为急，常规得失皆当让位"
            }
        }

        // —— 调候口诀（季节×日主限定，见官之吉凶随气候而殊）——
        val formulas = mutableListOf<TiaoHouFormula>()
        if (isJinShuiShangGuan(dayMaster, monthBranch)) {
            formulas.add(TiaoHouFormula("JIN_SHUI_SHANG_GUAN", "金水伤官喜见官",
                "金寒水冷，急需火官暖局；伤官见官本为祸，金水反喜见官（调候为急）", "喜"))
        }
        if (dayEl == Element.WOOD && monthBranch in SUMMER) {
            formulas.add(TiaoHouFormula("MU_HUO_SHANG_GUAN", "木火伤官喜见水",
                "夏木火炎，需水印润局；伤官佩印、夏木更秀（调候为急）", "—"))
        }
        if (dayEl == Element.WOOD && monthBranch in SPRING && hasFire) {
            formulas.add(TiaoHouFormula("MU_HUO_TONG_MING", "木火通明",
                "春木逢火泄秀为美，木火通明；不利见官（官金克木、损通明之秀）", "忌"))
        }
        if (dayEl == Element.METAL && monthBranch in AUTUMN && hasWater) {
            formulas.add(TiaoHouFormula("JIN_SHUI_XIANG_HAN", "金水相涵",
                "秋金本坚，遇水泄秀为美，金水相涵；见官无碍（与冬金金寒之金水伤官季节有别）", "无碍"))
        }

        // —— 调候得失注（季节限定得失：常规格局之美受气候所限）——
        val deShi = mutableListOf<String>()
        if (dayEl == Element.WOOD && monthBranch in WINTER) {
            deShi.add("冬木透官难贵——冬水冻而不生木，官印双全之美受气候所限")
        }
        if (dayEl == Element.METAL && monthBranch in WINTER) {
            deShi.add("冬金伤官用财小富不贵——冻水不生木财，财源受气候所限")
        }
        if (dayEl == Element.WOOD && monthBranch in SUMMER) {
            deShi.add("夏木食逢印反秀——食本畏枭印夺，然夏木火盛急需水调候，印反为泄秀调候之美")
        }

        return TiaoHouResult(
            climate = climate,
            climateBasis = climateBasis,
            tiaoHouElement = tiaoHouEl,
            tiaoHouPresent = tiaoHouPresent,
            tiaoHouNote = tiaoHouNote,
            urgent = urgent,
            urgentDesc = urgentDesc,
            congGe = congGe,
            formulas = formulas,
            deShi = deShi,
        )
    }
}

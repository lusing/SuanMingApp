package cn.xulun.bazi.core

import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EightDoor
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.PalaceAnalysis
import cn.xulun.bazi.model.PalaceAnalysisItem
import cn.xulun.bazi.model.PalaceCell
import cn.xulun.bazi.model.PalaceTone
import cn.xulun.bazi.model.QiGan
import cn.xulun.bazi.model.QiMenPlate
import cn.xulun.bazi.model.QiPalace
import cn.xulun.bazi.model.QiPattern
import java.lang.Math.floorMod

/**
 * 奇门格局识别（吉/凶约 18 种）+ 基础判读。
 *
 * 每条格局按通行定义匹配盘面元素（天盘干+地盘干、门+宫、星+神、全盘），
 * 命中即输出 名称/吉凶/依据。个别格局（如击刑、入墓）在不同门派定义略有出入，
 * 此处取最通行者并在依据中写明命中元素，便于核对。
 */
object QimenPatternCalculator {

    /** 旬空地支（时柱旬首 → 两个空亡地支） */
    private val xunKong: Map<Int, List<EarthlyBranch>> = mapOf(
        0 to listOf(EarthlyBranch.XU, EarthlyBranch.HAI),   // 甲子旬
        10 to listOf(EarthlyBranch.SHEN, EarthlyBranch.YOU), // 甲戌旬
        20 to listOf(EarthlyBranch.WU, EarthlyBranch.WEI),   // 甲申旬
        30 to listOf(EarthlyBranch.CHEN, EarthlyBranch.SI),  // 甲午旬
        40 to listOf(EarthlyBranch.YIN, EarthlyBranch.MAO),  // 甲辰旬
        50 to listOf(EarthlyBranch.ZI, EarthlyBranch.CHOU),  // 甲寅旬
    )

    /** 八门五行 */
    private fun doorElement(door: EightDoor): Element = when (door) {
        EightDoor.XIU -> Element.WATER
        EightDoor.SHENG, EightDoor.SI -> Element.EARTH
        EightDoor.SHANG, EightDoor.DU -> Element.WOOD
        EightDoor.JING -> Element.FIRE
        EightDoor.KAI, EightDoor.JING2 -> Element.METAL
    }

    /** 时支方位宫号（与排盘一致） */
    private fun palaceOf(branch: EarthlyBranch): Int = when (branch) {
        EarthlyBranch.ZI -> 1
        EarthlyBranch.CHOU, EarthlyBranch.YIN -> 8
        EarthlyBranch.MAO -> 3
        EarthlyBranch.CHEN, EarthlyBranch.SI -> 4
        EarthlyBranch.WU -> 9
        EarthlyBranch.WEI, EarthlyBranch.SHEN -> 2
        EarthlyBranch.YOU -> 7
        EarthlyBranch.XU, EarthlyBranch.HAI -> 6
    }

    private fun opposite(no: Int): Int = when (no) {
        1 -> 9; 9 -> 1; 2 -> 8; 8 -> 2; 3 -> 7; 7 -> 3; 4 -> 6; 6 -> 4
        else -> 5
    }

    /** 天盘干 + 地盘干 组合格局定义（analyze 与 analyzePalace 共用） */
    private data class ComboDef(val heaven: HeavenlyStem, val earth: HeavenlyStem, val pattern: String, val auspicious: Boolean)

    private val comboTable: List<ComboDef> = listOf(
        ComboDef(HeavenlyStem.WU, HeavenlyStem.BING, "青龙返首", true),
        ComboDef(HeavenlyStem.YI, HeavenlyStem.XIN, "青龙逃走", false),
        ComboDef(HeavenlyStem.XIN, HeavenlyStem.YI, "白虎猖狂", false),
        ComboDef(HeavenlyStem.GUI, HeavenlyStem.DING, "腾蛇妖娇", false),
        ComboDef(HeavenlyStem.DING, HeavenlyStem.GUI, "朱雀投江", false),
        ComboDef(HeavenlyStem.GENG, HeavenlyStem.BING, "太白入荧", false),
        ComboDef(HeavenlyStem.BING, HeavenlyStem.GENG, "荧入太白", false),
        ComboDef(HeavenlyStem.GENG, HeavenlyStem.GUI, "大格（刑格）", false),
    )

    /** 我(me)对他(other)的五行关系 */
    private enum class FiveRel { SAME, GENERATES, GENERATED, CONTROLS, CONTROLLED }
    private fun relation(me: Element, other: Element): FiveRel = when {
        me == other -> FiveRel.SAME
        me.generates == other -> FiveRel.GENERATES
        other.generates == me -> FiveRel.GENERATED
        me.controls == other -> FiveRel.CONTROLS
        else -> FiveRel.CONTROLLED
    }

    /** 时旬空亡所落宫号集合 */
    private fun kongPalaceNos(plate: QiMenPlate): Set<Int> {
        val hc = plate.hourPillar.cycleIndex
        val xunShouCyc = hc - floorMod(hc, 10)
        return xunKong.getValue(xunShouCyc % 60).map { palaceOf(it) }.toSet()
    }

    /** 命中本宫的组合格局（天盘干+地盘干） */
    private fun comboFor(cell: PalaceCell): QiPattern? =
        comboTable.firstOrNull { it.heaven == cell.heavenGan.stem && it.earth == cell.earthGan.stem }
            ?.let { QiPattern(it.pattern, it.auspicious, "天${QiGan.of(it.heaven)!!.chinese}加地${QiGan.of(it.earth)!!.chinese}") }

    /** 六甲击刑：天盘仪 → 其刑宫号 */
    private val xingPals: Map<QiGan, Int> = mapOf(
        QiGan.WU to 3,    // 甲子戊加震(卯) 子卯刑
        QiGan.GENG to 8,  // 甲申庚加艮(寅) 寅巳申刑
        QiGan.XIN to 9,   // 甲午辛加离(午) 午自刑
        QiGan.REN to 4,   // 甲辰壬加巽(辰) 辰自刑
        QiGan.GUI to 4,   // 甲寅癸加巽(寅) 寅巳申刑
    )

    private fun QiMenPlate.cellAt(no: Int): PalaceCell? = cells[no]

    private fun QiMenPlate.findCombo(heaven: HeavenlyStem, earth: HeavenlyStem): PalaceCell? =
        cells.values.firstOrNull { it.heavenGan.stem == heaven && it.earthGan.stem == earth }

    fun analyze(plate: QiMenPlate): List<QiPattern> {
        val out = mutableListOf<QiPattern>()
        val dun = plate.ju.dun

        // ── 天盘干+地盘干 组合格局 ──
        comboTable.forEach { def ->
            plate.findCombo(def.heaven, def.earth)?.let {
                out += QiPattern(
                    def.pattern,
                    def.auspicious,
                    "${it.palace.trigram}宫 天${QiGan.of(def.heaven)!!.chinese}加地${QiGan.of(def.earth)!!.chinese}",
                )
            }
        }

        // ── 三奇升殿（天盘奇临本位宫）──
        val sd = plate.cellAt(3)?.takeIf { it.heavenGan == QiGan.YI }
        val sp = plate.cellAt(9)?.takeIf { it.heavenGan == QiGan.BING }
        val sd2 = plate.cellAt(7)?.takeIf { it.heavenGan == QiGan.DING }
        listOfNotNull(sd, sp, sd2).takeIf { it.isNotEmpty() }?.let {
            out += QiPattern("三奇升殿", true, it.joinToString("、") { c -> "${c.heavenGan.chinese}奇临${c.palace.trigram}宫" })
        }

        // ── 三奇得使（三奇临开/休/生 三吉门）──
        val luckyDoors = setOf(EightDoor.KAI, EightDoor.XIU, EightDoor.SHENG)
        plate.cells.values.filter {
            it.heavenGan.isQi && it.door in luckyDoors
        }.takeIf { it.isNotEmpty() }?.let {
            out += QiPattern("三奇得使", true, it.joinToString("、") { c -> "${c.heavenGan.chinese}奇临${c.door!!.chinese}门" })
        }

        // ── 三奇入墓（乙临坤2、丙丁临乾6）──
        val mu = buildList {
            plate.cellAt(2)?.takeIf { it.heavenGan == QiGan.YI }?.let { add("${QiGan.YI.chinese}奇临坤宫入墓") }
            plate.cellAt(6)?.takeIf { it.heavenGan == QiGan.BING || it.heavenGan == QiGan.DING }?.let {
                add("${it.heavenGan.chinese}奇临乾宫入墓")
            }
        }
        if (mu.isNotEmpty()) out += QiPattern("三奇入墓", false, mu.joinToString("；"))

        // ── 六甲击刑（天盘仪临其刑宫）──
        xingPals.forEach { (gan, no) ->
            plate.cellAt(no)?.takeIf { it.heavenGan == gan }?.let {
                out += QiPattern("击刑（${gan.chinese}）", false, "${it.palace.trigram}宫 天${gan.chinese}临${it.palace.trigram}，干支相刑")
            }
        }

        // ── 伏吟 / 反吟 ──
        val outerNos = QiPalace.ring.map { it.no }
        val isFuYin = outerNos.all { plate.cellAt(it)!!.heavenGan == plate.cellAt(it)!!.earthGan }
        if (isFuYin) out += QiPattern("伏吟", false, "天盘与地盘重合，直符星落本宫，主停滞呻吟")
        val isFanYin = outerNos.all { plate.cellAt(it)!!.heavenGan == plate.cellAt(opposite(it))!!.earthGan }
        if (isFanYin) out += QiPattern("反吟", false, "天盘与地盘对冲，主反复动荡")

        // ── 门迫 / 宫制 ──
        plate.cells.values.forEach { c ->
            val door = c.door ?: return@forEach
            val doorEl = doorElement(door)
            val palEl = c.palace.element
            if (doorEl.controls == palEl) {
                out += QiPattern("门迫（${door.chinese}门）", false, "${c.palace.trigram}宫 ${door.chinese}门(${doorEl.chinese})克宫(${palEl.chinese})")
            } else if (palEl.controls == doorEl) {
                out += QiPattern("宫制（${door.chinese}门）", false, "${c.palace.trigram}宫 宫(${palEl.chinese})克${door.chinese}门(${doorEl.chinese})")
            }
        }

        // ── 空亡 ──
        val kongCells = kongPalaceNos(plate).mapNotNull { plate.cellAt(it) }.filter { it.palace.no != 5 }
        if (kongCells.isNotEmpty()) {
            out += QiPattern("空亡", false, "时旬空在${kongCells.joinToString("、") { it.palace.trigram + "宫" }}，主虚浮不成")
        }

        // ── 三诈（三奇 + 三吉门 + 太阴/六合/九地/九天 吉神同宫）──
        val luckySpirits = setOf(
            cn.xulun.bazi.model.EightSpirit.TAI_YIN,
            cn.xulun.bazi.model.EightSpirit.LIU_HE,
            cn.xulun.bazi.model.EightSpirit.JIU_DI,
            cn.xulun.bazi.model.EightSpirit.JIU_TIAN,
        )
        plate.cells.values.firstOrNull {
            it.heavenGan.isQi && it.door in luckyDoors && it.spirit in luckySpirits
        }?.let {
            out += QiPattern("三诈（${it.spirit!!.chinese}）", true, "${it.palace.trigram}宫 ${it.heavenGan.chinese}奇+${it.door!!.chinese}门+${it.spirit!!.chinese}，奇门吉神俱全")
        }

        return out.sortedByDescending { it.auspicious }
    }

    /** 基础判读文案（直符/直使落宫、用神旺衰） */
    fun basics(plate: QiMenPlate): List<String> {
        val zhiFuPalace = plate.cells.values.firstOrNull { it.isZhiFuPalace }
        val zhiShiPalace = plate.cells.values.firstOrNull { it.isZhiShiPalace }
        val lines = mutableListOf<String>()
        zhiFuPalace?.let {
            lines += "直符（${plate.zhiFuStar.chinese}）落${it.palace.trigram}宫，临${it.door?.chinese ?: "—"}门、${it.spirit?.chinese ?: "—"}神"
        }
        zhiShiPalace?.let {
            lines += "直使（${plate.zhiShiDoor.chinese}门）落${it.palace.trigram}宫，天${it.heavenGan.chinese}加地${it.earthGan.chinese}"
        }
        // 用神：日干为求测人、时干为所测之事，看落宫
        val dayGan = plate.dayPillar.stem
        val hourGan = plate.hourPillar.stem
        val personCell = plate.cells.values.firstOrNull { it.earthGan.stem == dayGan || it.heavenGan.stem == dayGan }
        val matterCell = plate.cells.values.firstOrNull { it.earthGan.stem == hourGan || it.heavenGan.stem == hourGan }
        personCell?.let {
            lines += "日干${dayGan.chinese}（求测人）落${it.palace.trigram}宫"
        }
        matterCell?.let {
            lines += "时干${hourGan.chinese}（所测之事）落${it.palace.trigram}宫"
        }
        return lines
    }

    // ───────────── 单宫分析 ─────────────

    /** 九星落宫：星五行对宫五行的旺相休囚 */
    private fun starAssessment(starEl: Element, palEl: Element): Pair<String, PalaceTone> = when (relation(starEl, palEl)) {
        FiveRel.SAME -> "星宫比和（${starEl.chinese}），星旺相、得地有力" to PalaceTone.GOOD
        FiveRel.GENERATED -> "宫生星（${palEl.chinese}生${starEl.chinese}），星受生、最为得地" to PalaceTone.GOOD
        FiveRel.GENERATES -> "星生宫（${starEl.chinese}生${palEl.chinese}），星泄气减力" to PalaceTone.NEUTRAL
        FiveRel.CONTROLS -> "星克宫（${starEl.chinese}克${palEl.chinese}），星不受制、力尚可" to PalaceTone.NEUTRAL
        FiveRel.CONTROLLED -> "宫克星（${palEl.chinese}克${starEl.chinese}），星受制减力" to PalaceTone.BAD
    }

    /** 八门落宫：门五行对宫五行的生克（门迫/宫制） */
    private fun doorAssessment(doorEl: Element, palEl: Element): Pair<String, PalaceTone> = when (relation(doorEl, palEl)) {
        FiveRel.SAME -> "门宫比和（${doorEl.chinese}），相辅" to PalaceTone.GOOD
        FiveRel.GENERATED -> "宫生门（${palEl.chinese}生${doorEl.chinese}），门受生、吉" to PalaceTone.GOOD
        FiveRel.GENERATES -> "门生宫（${doorEl.chinese}生${palEl.chinese}），相生" to PalaceTone.NEUTRAL
        FiveRel.CONTROLS -> "门迫：门(${doorEl.chinese})克宫(${palEl.chinese})，吉门不吉、凶门更凶" to PalaceTone.BAD
        FiveRel.CONTROLLED -> "宫制：宫(${palEl.chinese})克门(${doorEl.chinese})，门受制" to PalaceTone.BAD
    }

    /** 天盘干对地盘干的五行关系 */
    private fun ganRelation(hvEl: Element, eaEl: Element): String = when (relation(hvEl, eaEl)) {
        FiveRel.SAME -> "天地比和（${hvEl.chinese}），盘气伏吟、主停滞"
        FiveRel.GENERATED -> "地生天（${eaEl.chinese}生${hvEl.chinese}），天盘受生而旺"
        FiveRel.GENERATES -> "天生地（${hvEl.chinese}生${eaEl.chinese}），天盘泄气"
        FiveRel.CONTROLS -> "天克地（${hvEl.chinese}克${eaEl.chinese}），主动、有伤"
        FiveRel.CONTROLLED -> "地克天（${eaEl.chinese}克${hvEl.chinese}），天盘受制"
    }

    /** 单宫分析：宫位信息 + 星/门/神/天地盘干各层生克 + 本宫格局 + 用神落宫 + 综合断语 */
    fun analyzePalace(plate: QiMenPlate, palaceNo: Int): PalaceAnalysis {
        val cell = plate.cells.getValue(palaceNo)
        val pal = cell.palace
        val isCenter = palaceNo == 5
        val items = mutableListOf<PalaceAnalysisItem>()
        val patterns = mutableListOf<QiPattern>()
        val tags = mutableListOf<String>()

        if (cell.isZhiFuPalace) tags += "直符宫"
        if (cell.isZhiShiPalace) tags += "直使宫"
        val isKong = palaceNo in kongPalaceNos(plate) && !isCenter
        if (isKong) {
            tags += "空亡"
            patterns += QiPattern("空亡", false, "时旬空在此宫，主虚浮不成、事多落空")
        }

        // ── 九星 ──
        val (starText, starTone) = starAssessment(cell.star.element, pal.element)
        items += PalaceAnalysisItem(
            label = "九星",
            text = "${cell.star.chinese}（${if (cell.star.auspicious) "吉" else "凶"}星 · ${cell.star.element.chinese}）$starText",
            tone = starTone,
        )

        // ── 八门（中宫无门）──
        cell.door?.let { door ->
            val (doorText, doorTone) = doorAssessment(doorElement(door), pal.element)
            items += PalaceAnalysisItem(
                label = "八门",
                text = "${door.chinese}门（${if (door.auspicious) "吉" else "凶"}门 · ${doorElement(door).chinese}）$doorText",
                tone = doorTone,
            )
        }

        // ── 八神（中宫无神）──
        cell.spirit?.let { spirit ->
            items += PalaceAnalysisItem(
                label = "八神",
                text = "${spirit.chinese}（${if (spirit.auspicious) "吉" else "凶"}神）",
                tone = if (spirit.auspicious) PalaceTone.GOOD else PalaceTone.BAD,
            )
        }

        // ── 天地盘干 ──
        val hvEl = cell.heavenGan.stem.element
        val eaEl = cell.earthGan.stem.element
        items += PalaceAnalysisItem(
            label = "天地盘干",
            text = "天${cell.heavenGan.chinese}(${hvEl.chinese}) 加 地${cell.earthGan.chinese}(${eaEl.chinese})，${ganRelation(hvEl, eaEl)}",
        )
        comboFor(cell)?.let { patterns += it }

        // ── 本宫其它格局 ──
        // 击刑
        if (cell.heavenGan in xingPals && xingPals.getValue(cell.heavenGan) == palaceNo) {
            patterns += QiPattern("击刑（${cell.heavenGan.chinese}）", false, "天${cell.heavenGan.chinese}临${pal.trigram}宫，干支相刑")
        }
        // 三奇入墓
        if (palaceNo == 2 && cell.heavenGan == QiGan.YI) {
            patterns += QiPattern("三奇入墓", false, "乙奇临坤宫入墓")
        } else if (palaceNo == 6 && (cell.heavenGan == QiGan.BING || cell.heavenGan == QiGan.DING)) {
            patterns += QiPattern("三奇入墓", false, "${cell.heavenGan.chinese}奇临乾宫入墓")
        }
        // 三奇升殿（天奇临本位宫）
        if (cell.heavenGan.isQi && ((palaceNo == 3 && cell.heavenGan == QiGan.YI) ||
                (palaceNo == 9 && cell.heavenGan == QiGan.BING) || (palaceNo == 7 && cell.heavenGan == QiGan.DING))) {
            patterns += QiPattern("三奇升殿", true, "${cell.heavenGan.chinese}奇临${pal.trigram}宫本位")
        }
        // 三奇得使（奇临开/休/生三吉门）
        val luckyDoors = setOf(EightDoor.KAI, EightDoor.XIU, EightDoor.SHENG)
        if (cell.heavenGan.isQi && cell.door in luckyDoors) {
            patterns += QiPattern("三奇得使", true, "${cell.heavenGan.chinese}奇临${cell.door!!.chinese}门")
        }

        // ── 用神落宫（日干为求测人、时干为所测之事）──
        val dayGan = plate.dayPillar.stem
        val hourGan = plate.hourPillar.stem
        if (cell.heavenGan.stem == dayGan || cell.earthGan.stem == dayGan) {
            items += PalaceAnalysisItem("用神", "日干${dayGan.chinese}（求测人）落此宫", PalaceTone.NEUTRAL)
        }
        if (cell.heavenGan.stem == hourGan || cell.earthGan.stem == hourGan) {
            items += PalaceAnalysisItem("用神", "时干${hourGan.chinese}（所测之事）落此宫", PalaceTone.NEUTRAL)
        }
        if (isCenter) items += PalaceAnalysisItem("寄宫", "中宫寄坤二宫，天禽星随寄，无门无神", PalaceTone.NEUTRAL)

        // ── 综合断语 ──
        val summary = buildString {
            append("${pal.trigram}${pal.no}宫，${cell.star.chinese}星")
            cell.door?.let { append("临${it.chinese}门") }
            append("，天${cell.heavenGan.chinese}加地${cell.earthGan.chinese}")
            if (isKong) append("；逢空亡")
            when {
                patterns.any { !it.auspicious } -> append("；本宫有凶象，宜审慎")
                patterns.any { it.auspicious } -> append("；本宫逢吉，可用")
                else -> append("；无明显特殊格局")
            }
        }

        return PalaceAnalysis(
            palaceNo = palaceNo,
            title = "${pal.trigram}${pal.no}宫",
            subtitle = "${pal.direction}方 · ${pal.element.chinese}地",
            tags = tags,
            items = items,
            patterns = patterns,
            summary = summary,
        )
    }
}

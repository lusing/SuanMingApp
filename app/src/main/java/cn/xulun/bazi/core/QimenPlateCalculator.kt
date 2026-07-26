package cn.xulun.bazi.core

import cn.xulun.bazi.model.Dun
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.EightDoor
import cn.xulun.bazi.model.EightSpirit
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.JuResult
import cn.xulun.bazi.model.NineStar
import cn.xulun.bazi.model.PalaceCell
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.QiGan
import cn.xulun.bazi.model.QiMenPlate
import cn.xulun.bazi.model.QiPalace
import cn.xulun.bazi.model.xunShouDunYi
import java.lang.Math.floorMod
import java.time.LocalDateTime

/**
 * 转盘时家奇门排盘：地盘六仪三奇 → 直符/直使 → 天盘、九星、八神、八门三环旋转。
 *
 * 旋转模型：外八宫按后天八卦成环（坎→艮→震→巽→离→坤→兑→乾），阳遁顺转、阴遁逆转。
 *  - 直符（仪/星/神/天盘干同转）：从直符宫 Pf 旋到「时干仪」所在宫 Pshi（直符随时干）。
 *  - 直使（八门环）：从直符宫 Pf 旋到「时支方位宫」Tp（直使随时支）。
 * 中宫不入环，寄坤二宫（天禽随天芮）。
 */
object QimenPlateCalculator {

    /** 中宫寄宫：坤二宫 */
    private const val JI_GONG = 2

    fun buildPlate(dt: LocalDateTime, ju: JuResult): QiMenPlate {
        val bazi = BaziCalculator.calculate(dt)
        val dayPillar = bazi.dayPillar
        val hourPillar = bazi.hourPillar
        val dun = ju.dun

        // ── 地盘六仪三奇 ──
        val earthGan: Map<Int, QiGan> = (1..9).associateWith { q -> earthGanAt(q, ju.ju, dun) }

        // ── 直符仪 / 直符宫 / 直符星 / 直使门 ──
        val hourCyc = hourPillar.cycleIndex
        val xunShouCyc = hourCyc - floorMod(hourCyc, 10)
        val xunShouBranch = Pillar.fromCycleIndex(xunShouCyc).branch
        val zhiFuGan = xunShouDunYi.getValue(xunShouBranch)   // 直符所乘之仪
        val pf = palaceOf(earthGan, zhiFuGan)                  // 直符宫（地盘上直符仪所在）
        val zhiFuStar = NineStar.atPalace(pf)
        val zhiShiDoor = EightDoor.atPalace(pf)!!              // 直使门（直符宫本位门）

        // ── 时干仪宫 Pshi / 时支方位宫 Tp ──
        val shiYi = shiGanOf(hourPillar.stem, zhiFuGan)        // 时干对应之仪
        val pshi = palaceOf(earthGan, shiYi)
        val tp = palaceOfHourBranch(hourPillar.branch)

        // 中宫寄宫映射
        val pfR = jiGong(pf)
        val pshiR = jiGong(pshi)
        val tpR = jiGong(tp)
        val idxPf = QiPalace.ringIndexOf(pfR)
        val idxPshi = QiPalace.ringIndexOf(pshiR)
        val idxTp = QiPalace.ringIndexOf(tpR)
        val yang = dun == Dun.YANG

        // ── 旋转步数 ──
        val kStar = if (yang) floorMod(idxPshi - idxPf, 8) else floorMod(idxPf - idxPshi, 8)
        val kDoor = if (yang) floorMod(idxTp - idxPf, 8) else floorMod(idxPf - idxTp, 8)

        // ── 填外八宫 ──
        val cells = HashMap<Int, PalaceCell>()
        for (j in 0 until 8) {
            val no = QiPalace.ring[j].no
            val srcStar = if (yang) floorMod(j - kStar, 8) else floorMod(j + kStar, 8)
            val srcDoor = if (yang) floorMod(j - kDoor, 8) else floorMod(j + kDoor, 8)
            val star = NineStar.atPalace(QiPalace.ring[srcStar].no)
            val door = EightDoor.atPalace(QiPalace.ring[srcDoor].no)
            // 天盘干：与九星同转
            val heavenGan = earthGan.getValue(QiPalace.ring[srcStar].no)
            // 八神：直符落 Pshi，余者顺(阳)/逆(阴)环布
            val spirit = if (yang) EightSpirit.entries[floorMod(j - idxPshi, 8)]
            else EightSpirit.entries[floorMod(idxPshi - j, 8)]
            cells[no] = PalaceCell(
                palace = QiPalace.of(no),
                star = star,
                door = door,
                spirit = spirit,
                heavenGan = heavenGan,
                earthGan = earthGan.getValue(no),
                isZhiFuPalace = no == pshiR,
                isZhiShiPalace = no == tpR,
            )
        }

        // ── 中宫 ──
        val jiNote = if (pf == 5 || pshi == 5 || tp == 5) "中宫寄坤二宫" else "中宫寄坤二宫"
        cells[5] = PalaceCell(
            palace = QiPalace.of(5),
            star = NineStar.QIN,
            door = null,
            spirit = null,
            heavenGan = earthGan.getValue(5),
            earthGan = earthGan.getValue(5),
            isZhiFuPalace = false,
            isZhiShiPalace = false,
        )

        return QiMenPlate(
            cells = cells,
            zhiFuStar = zhiFuStar,
            zhiShiDoor = zhiShiDoor,
            ju = ju,
            dayPillar = dayPillar,
            hourPillar = hourPillar,
            jiGong = jiNote,
        )
    }

    /** 地盘某宫之仪：阳遁顺布、阴遁逆布 */
    private fun earthGanAt(q: Int, ju: Int, dun: Dun): QiGan {
        val idx = if (dun == Dun.YANG) floorMod(q - ju, 9) else floorMod(ju - q, 9)
        return QiGan.order[idx]
    }

    /** 时干对应之仪：甲取旬首遁仪，余即本身 */
    private fun shiGanOf(stem: HeavenlyStem, zhiFuGan: QiGan): QiGan =
        if (stem == HeavenlyStem.JIA) zhiFuGan else QiGan.of(stem)!!

    /** 地盘上某仪所在宫号 */
    private fun palaceOf(earthGan: Map<Int, QiGan>, gan: QiGan): Int =
        earthGan.entries.first { it.value == gan }.key

    /** 时支方位宫号 */
    private fun palaceOfHourBranch(branch: EarthlyBranch): Int = when (branch) {
        EarthlyBranch.ZI -> 1
        EarthlyBranch.CHOU, EarthlyBranch.YIN -> 8
        EarthlyBranch.MAO -> 3
        EarthlyBranch.CHEN, EarthlyBranch.SI -> 4
        EarthlyBranch.WU -> 9
        EarthlyBranch.WEI, EarthlyBranch.SHEN -> 2
        EarthlyBranch.YOU -> 7
        EarthlyBranch.XU, EarthlyBranch.HAI -> 6
    }

    /** 中宫(5)寄坤二宫 */
    private fun jiGong(no: Int): Int = if (no == 5) JI_GONG else no
}

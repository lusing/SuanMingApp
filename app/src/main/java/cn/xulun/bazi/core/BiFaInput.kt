package cn.xulun.bazi.core

import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.LiuRenPlate
import cn.xulun.bazi.model.TianJiang
import java.lang.Math.floorMod

/**
 * 毕法赋判定的预构上下文：由 [LiuRenPlate] 一次性算出四课上神、三传、天将临位、
 * 旬空/旬丁、禄墓长生、十神关系、神煞等，供百法 [judge] 复用。
 */
class BiFaInput(plate: LiuRenPlate) {
    val p: LiuRenPlate = plate
    val dayStem: HeavenlyStem = plate.dayStem
    val dayBranch: EarthlyBranch = plate.dayBranch
    val ganJi: EarthlyBranch = LiuRenTables.jiGong.getValue(dayStem)

    val ganShang: EarthlyBranch = plate.lesson(1).top   // 干上神(一课)
    val ganYin: EarthlyBranch = plate.lesson(2).top      // 干阴神(二课)
    val zhiShang: EarthlyBranch = plate.lesson(3).top    // 支上神(三课)
    val zhiYin: EarthlyBranch = plate.lesson(4).top      // 支阴神(四课)

    val c1: EarthlyBranch = plate.threeChuan.initial
    val c2: EarthlyBranch = plate.threeChuan.middle
    val c3: EarthlyBranch = plate.threeChuan.last

    val hourCyc: Int = plate.hourCycleIndex
    val kong: Set<EarthlyBranch> = LiuRenTables.xunKong(hourCyc)
    val ding: EarthlyBranch = LiuRenTables.xunDingBranch(hourCyc)
    val xunShou: EarthlyBranch = LiuRenTables.xunShouBranch(hourCyc)
    val xunWei: EarthlyBranch = LiuRenTables.xunWeiBranch(hourCyc)

    val lu: EarthlyBranch = LiuRenTables.lu.getValue(dayStem)
    val mu: EarthlyBranch = LiuRenTables.mu.getValue(dayStem)
    val cSheng: EarthlyBranch = LiuRenTables.changSheng.getValue(dayStem)

    val taiSui: EarthlyBranch = plate.yearBranch
    val yueZhi: EarthlyBranch = plate.monthBranch
    val yueJiang: EarthlyBranch = plate.yueJiang.branch   // 太阳
    val isDay: Boolean = plate.isDay
    val xingNian: EarthlyBranch? = plate.nianMing.xingNian
    val benMing: EarthlyBranch? = plate.nianMing.benMing
    val dayKey: String = "${dayStem.chinese}${dayBranch.chinese}"

    // 天将骑天盘支 → 该支上的天将；"将临/乘 b" 即 jiangOn(b)
    private val jiangOnHeaven: Map<EarthlyBranch, TianJiang> = plate.tianJiang.associate { it.heavenBranch to it.jiang }
    fun jiangOn(b: EarthlyBranch): TianJiang? = jiangOnHeaven[b]
    fun rides(b: EarthlyBranch, j: TianJiang): Boolean = jiangOnHeaven[b] == j
    fun branchOf(j: TianJiang): EarthlyBranch = p.tianJiang.first { it.jiang == j }.heavenBranch

    // 干支五行关系（十神五类）
    fun rel(b: EarthlyBranch): GanZhiRel = LiuRenTables.relOf(dayStem, b)
    fun isGui(b: EarthlyBranch) = rel(b) == GanZhiRel.GUI
    fun isCai(b: EarthlyBranch) = rel(b) == GanZhiRel.CAI
    fun isTuo(b: EarthlyBranch) = rel(b) == GanZhiRel.SHI_SHANG
    fun isYin(b: EarthlyBranch) = rel(b) == GanZhiRel.YIN
    fun isBi(b: EarthlyBranch) = rel(b) == GanZhiRel.BI

    fun isKong(b: EarthlyBranch) = b in kong
    fun isDing(b: EarthlyBranch) = b == ding
    fun liuHe(a: EarthlyBranch, b: EarthlyBranch) = LiuRenTables.liuHe(a, b)
    fun liuHai(a: EarthlyBranch, b: EarthlyBranch) = LiuRenTables.liuHai(a, b)
    fun chong(a: EarthlyBranch, b: EarthlyBranch) = LiuRenTables.chong(a, b)
    fun chuanAll(pred: (EarthlyBranch) -> Boolean) = pred(c1) && pred(c2) && pred(c3)

    val season: String
        get() = when (yueZhi) {
            EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN -> "春"
            EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI -> "夏"
            EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU -> "秋"
            else -> "冬"
        }

    // 神煞
    val guanShen: EarthlyBranch
        get() = when (season) {
            "春" -> EarthlyBranch.CHOU; "夏" -> EarthlyBranch.CHEN
            "秋" -> EarthlyBranch.WEI; else -> EarthlyBranch.XU
        }
    val sangMen: EarthlyBranch get() = EarthlyBranch.fromIndex(floorMod(taiSui.ordinal + 2, 12))
    val diaoKe: EarthlyBranch get() = EarthlyBranch.fromIndex(floorMod(taiSui.ordinal - 2, 12))
    val bingFu: EarthlyBranch get() = EarthlyBranch.fromIndex(floorMod(taiSui.ordinal - 1, 12))
    val yiMa: EarthlyBranch get() = LiuRenTables.yiMa(dayBranch)
    val tianMa: EarthlyBranch get() = LiuRenTables.tianMa(yueZhi)
    val huaGai: EarthlyBranch get() = LiuRenTables.huaGai(dayBranch)
    val taoHua: EarthlyBranch get() = LiuRenTables.taoHua(dayBranch)

    /** 生气：正月(寅)起子顺行；死气为生气对冲 */
    val shengQi: EarthlyBranch
        get() = EarthlyBranch.fromIndex(floorMod(yueZhi.ordinal - 2, 12))
    val siQi: EarthlyBranch get() = EarthlyBranch.fromIndex(floorMod(shengQi.ordinal + 6, 12))

    /** 胎神(粗)：以日干长生之支为胎 */
    val taiShen: EarthlyBranch get() = cSheng

    /** 行年/本命之上神 */
    fun xingNianShang(): EarthlyBranch? = xingNian?.let { p.heavenAt(it) }
    fun benMingShang(): EarthlyBranch? = benMing?.let { p.heavenAt(it) }

    /** 旬遁干：地支在本旬所遁之天干（旬空支无遁干，返回 null） */
    fun dunGan(b: EarthlyBranch): HeavenlyStem? {
        if (isKong(b)) return null
        val pos = floorMod(b.ordinal - xunShou.ordinal, 12)
        if (pos >= 10) return null
        return HeavenlyStem.fromIndex(pos)
    }

    /** 天盘支 b 所临的地盘支（即其"坐下"） */
    fun earthOf(b: EarthlyBranch): EarthlyBranch = EarthlyBranch.fromIndex(floorMod(b.ordinal - p.tianPanOffset, 12))
}

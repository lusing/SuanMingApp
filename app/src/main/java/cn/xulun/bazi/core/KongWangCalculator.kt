package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.KongWangMark
import cn.xulun.bazi.model.KongWangResult
import java.lang.Math.floorMod

/**
 * 旬空/空亡计算器。逐字移植自 xuanzong `utils/bazi/kongwang.js`。
 *
 * 以**日柱**六十甲子序号定旬：旬首 = cycleIndex 向下取整到 10 的倍数；
 * 旬首地支 = headCycle mod 12；空亡两支 = 旬首前两位（mod 12）。
 * 四柱中地支落此两支者即空亡。
 */
object KongWangCalculator {

    /** 由日柱六十甲子序号求旬空两支。 */
    fun xunKongBranches(dayCycleIndex: Int): Pair<EarthlyBranch, EarthlyBranch> {
        val headCycle = dayCycleIndex - floorMod(dayCycleIndex, 10) // 旬首序号
        val headBranch = floorMod(headCycle, 12)                    // 旬首地支
        val b1 = EarthlyBranch.fromIndex(floorMod(headBranch - 2, 12))
        val b2 = EarthlyBranch.fromIndex(floorMod(headBranch - 1, 12))
        return b1 to b2
    }

    /** 标记原局四柱中落空亡的柱（以日柱旬为基准）。 */
    fun analyze(bazi: BaziResult): KongWangResult {
        val (b1, b2) = xunKongBranches(bazi.dayPillar.cycleIndex)
        val marks = bazi.pillars.mapIndexedNotNull { i, p ->
            if (p.branch == b1 || p.branch == b2) KongWangMark(i, p.branch) else null
        }
        return KongWangResult(marks)
    }
}

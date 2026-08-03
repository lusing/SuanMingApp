package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.FlowItem
import cn.xulun.bazi.model.FlowType
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.WarFlags
import cn.xulun.bazi.model.WuXingFlowResult

/**
 * 五行流通 / 和局 / 战局检测试验。逐字移植自 xuanzong `utils/bazi/studyFlow.js`。
 *
 * 检测：相生链（环绕）、木火通明/金水相涵/火土成势、五行流通、金木·水火交战、化战为和。
 * 用 [GeJuCalculator.strengthScore]（分数版，与 xuanzong assessStrength 同源）判身强。
 */
object WuXingFlowCalculator {

    private val EL_NAME = arrayOf("木", "火", "土", "金", "水") // 按 Element.ordinal
    private val SEQ = listOf(0, 1, 2, 3, 4, 0, 1, 2, 3, 4)     // 相生环绕序

    /** 干支五行计数（每柱干+支各计 1），按 [cn.xulun.bazi.model.Element.ordinal] 索引。 */
    fun elementCount(pillars: List<Pillar>): IntArray {
        val c = IntArray(5)
        for (p in pillars) {
            c[p.stem.element.ordinal]++
            c[p.branch.element.ordinal]++
        }
        return c
    }

    /** 最长相生链长度：强五行(count≥min)按 木→火→土→金→水→木 序，含环绕。 */
    fun genChain(elems: IntArray, min: Int = 2): Int {
        fun has(i: Int) = elems[i] >= min
        var max = 0
        var run = 0
        for (i in SEQ.indices) {
            if (has(SEQ[i])) {
                run++
                if (run > max) max = run
                if (max >= 5) break
            } else run = 0
        }
        return minOf(max, 5)
    }

    /** 最长相生链的中文序列（如 "水木火土"）。 */
    fun chainText(elems: IntArray, min: Int = 2): String {
        fun has(i: Int) = elems[i] >= min
        var max = 0
        var run = 0
        var maxStart = 0
        var curStart = 0
        for (i in SEQ.indices) {
            if (has(SEQ[i])) {
                if (run == 0) curStart = i
                run++
                if (run > max) { max = run; maxStart = curStart }
                if (max >= 5) break
            } else run = 0
        }
        val sb = StringBuilder()
        for (i in 0 until max) sb.append(EL_NAME[SEQ[maxStart + i]])
        return sb.toString()
    }

    /** 原局交战：六冲中的金木对（寅申/卯酉）、水火对（子午/巳亥）。 */
    fun detectWar(bazi: BaziResult): WarFlags {
        val rel = DiZhiRelationCalculator.analyze(bazi)
        var jinMu = false
        var shuiHuo = false
        for (c in rel.chongList) {
            val pair = listOf(c.branch1.element.ordinal, c.branch2.element.ordinal).sorted().joinToString(",")
            if (pair == "0,3") jinMu = true   // 木+金
            if (pair == "1,4") shuiHuo = true // 火+水
        }
        return WarFlags(jinMu, shuiHuo)
    }

    /** 五行流通/战局断语。[extraPillars] 为大运/流年柱（叠加计数）。 */
    fun analyze(bazi: BaziResult, extraPillars: List<Pillar> = emptyList()): WuXingFlowResult {
        val dmEl = bazi.dayPillar.stem.element.ordinal
        val sealEl = (dmEl + 4) % 5 // 生我者（印）五行
        val elems = elementCount(bazi.pillars + extraPillars)
        val chain = genChain(elems)
        val war = detectWar(bazi)
        val strong = GeJuCalculator.strengthScore(bazi) >= 2
        val favorable = strong || elems[sealEl] >= 2
        val out = mutableListOf<FlowItem>()

        // 木火通明（日干木 + 火旺）
        if (dmEl == 0 && elems[1] >= 3) {
            out.add(FlowItem(FlowType.FAVOR, "mu_huo_tong", "木火通明日主强，胸中珠玑万斛状元才。"))
        }
        // 金水相涵有火暖（日干金 + 水旺 + 火暖）
        if (dmEl == 3 && elems[4] >= 3 && elems[1] >= 1) {
            out.add(FlowItem(FlowType.FAVOR, "jin_shui_han", "金水相涵有火暖，聪明机智，文章振发。"))
        }
        // 火土成势（火土两行成势，他行弱）
        if (elems[1] >= 2 && elems[2] >= 2 && (elems[1] + elems[2]) >= 5 &&
            maxOf(elems[0], elems[3], elems[4]) <= 1
        ) {
            out.add(FlowItem(FlowType.FAVOR, "huo_tu_cheng", "火土成势，两行清纯，利文名。"))
        }
        // 五行流通和局（相生链≥3 且顺势生扶日主）
        if (chain >= 3 && favorable) {
            val text = if (chain >= 4) "五行流通相生" else "${chainText(elems)}顺势相生"
            out.add(FlowItem(FlowType.FAVOR, "wu_xing_liu", "$text，和局清纯，利文途考学。"))
        }

        // 战局
        if (war.jinMu) out.add(FlowItem(FlowType.UNFAVOR, "jin_mu_zhan", "金木交战（卯酉/寅申冲），恐伤文途，宜通关化解。"))
        if (war.shuiHuo) out.add(FlowItem(FlowType.UNFAVOR, "shui_huo_zhan", "水火交战（子午/巳亥冲），恐伤文途，宜通关化解。"))
        // 化战为和：战局 + 流通顺势
        if ((war.jinMu || war.shuiHuo) && chain >= 3 && favorable) {
            out.add(FlowItem(FlowType.FAVOR, "hua_zhan_he", "交战得流通化解，化战为和，转危为安，反主成名。"))
        }

        return WuXingFlowResult(out, chain, elems, war, favorable)
    }
}

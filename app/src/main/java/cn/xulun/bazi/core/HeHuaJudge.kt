package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem

/**
 * 天干合化成败判定（第五六〇节四·正化条件）。总准则**从强而化**（化神行相对整个命局）。
 *
 * 判定序：
 * 1. **正化**（甲己土/乙庚金/丙辛水/丁壬木/戊癸火）四关：
 *    力≥2.5（真化神门槛）；有根基——月令同行且未变节，或支成该行之局（三合三会/中神半合≥2支），
 *    或透干≥2，或透1+支≥2（甲卯结党式）；不被克破——克行之力<化神行力，克行干居两合干之间则阻化
 *    （丙隔乙庚），有通关行（透+力≥2+力≤化神）则克行贪生忘克（木生火火生土流通式）；
 *    耗行之力<化神行力（水多土流式）；两干非力均互绊（弱干≤0.8×强干或化神≥强干行）。
 * 2. **反化**：强干行 ≥2×弱干行且有根基且≠正化行（甲极强征服己化木式）——
 *    唯印重埋没（生正化之行 ≥2.5×正化行且正化行<2.5——土重埋金/晦火无光）连反化不成。
 * 3. 其余**合而不化**（假化——互相牵绊如武士落网）。
 */
object HeHuaJudge {

    fun zhengHua(a: HeavenlyStem, b: HeavenlyStem): Element? {
        if ((a.ordinal - b.ordinal + 10) % 5 != 0 || a.ordinal == b.ordinal) return null
        return when (minOf(a.ordinal, b.ordinal)) {
            0 -> Element.EARTH; 1 -> Element.METAL; 2 -> Element.WATER; 3 -> Element.WOOD; else -> Element.FIRE
        }
    }

    data class HeHuaVerdict(
        val pair: Pair<HeavenlyStem, HeavenlyStem>,
        val huaShen: Element,
        val cheng: Boolean,
        val basis: String,
    )

    fun judge(bazi: BaziResult): HeHuaVerdict? {
        val stems = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.dayPillar.stem, bazi.hourPillar.stem)
        val pairs = mutableListOf<Pair<Int, Int>>()
        for (i in 0..3) for (j in i + 1..3) if (zhengHua(stems[i], stems[j]) != null) pairs += i to j
        if (pairs.isEmpty()) return null
        val day = bazi.dayPillar.stem
        val (pi, pj) = pairs.firstOrNull { stems[it.first] == day || stems[it.second] == day } ?: pairs.first()
        val a = stems[pi]; val b = stems[pj]
        val z = zhengHua(a, b)!!

        // 朴素力图 + 合局集合转移（三合三会全局+中神半合；拱局不动）
        val f = Element.entries.associateWith { el ->
            (bazi.stems.count { it.element == el } + bazi.branches.count { it.element == el }).toDouble() +
                (if (bazi.monthPillar.branch.element == el) 2.0 else 0.0)
        }.toMutableMap()
        val heJu = mutableListOf<Pair<Set<EarthlyBranch>, Element>>()
        DiZhiRelationCalculator.analyze(bazi).sanHeList.forEach { heJu += it.branches.toSet() to it.resultElement }
        val hh = HeHuaCalculator.analyze(bazi)
        hh.branchSanHuiList.forEach { heJu += it.branches.toSet() to it.resultElement }
        hh.branchBanHeList.forEach { heJu += setOf(it.branch1, it.branch2) to it.resultElement }
        val heJuD = heJu.distinctBy { it.first to it.second } // 两巳对一酉会重复报巳酉半合——去重防双转移
        val zhongOf = mapOf(Element.WOOD to EarthlyBranch.MAO, Element.FIRE to EarthlyBranch.WU,
            Element.METAL to EarthlyBranch.YOU, Element.WATER to EarthlyBranch.ZI)
        for ((bs, el) in heJuD) {
            val zb = zhongOf[el]
            if (el == Element.EARTH || (zb != null && zb !in bazi.branches)) continue
            for (br in bs) if (br.element != el && br in bazi.branches) {
                f[br.element] = (f[br.element]!! - 1).coerceAtLeast(0.0); f[el] = f[el]!! + 1
            }
        }
        val yueZhi = bazi.monthPillar.branch
        val yueXing = yueZhi.element

        fun genJi(x: Element): Boolean =
            (yueXing == x && heJuD.none { (bs, el) -> yueZhi in bs && el != x }) ||
                heJuD.any { (bs, el) -> el == x && bs.count { it in bazi.branches } >= 2 } ||
                bazi.stems.count { it.element == x } >= 2 ||
                (bazi.stems.count { it.element == x } >= 1 && bazi.branches.count { it.element == x } >= 2)

        val lineA = a.element; val lineB = b.element
        val weakLine = if (f[lineA]!! <= f[lineB]!!) lineA else lineB
        val strongLine = if (weakLine == lineA) lineB else lineA

        // 克行干居两合干之间 → 阻化
        fun keGanJuZhong(k: Element): Boolean {
            val lo = minOf(pi, pj); val hi = maxOf(pi, pj)
            return (lo + 1 until hi).any { stems[it].element == k }
        }

        // 正化四关
        val ke = Element.entries.first { it.controls == z }
        val tong = Element.entries.firstOrNull { it.generates == z && ke.generates == it }
        val tongOk = tong != null && f[tong]!! >= 2 && bazi.stems.any { it.element == tong } && f[tong]!! <= f[z]!!
        val q3 = !keGanJuZhong(ke) && (f[ke]!! < f[z]!! || tongOk)
        val hao = z.controls
        val q1 = f[z]!! >= 2.5
        val q2 = genJi(z)
        // 耗行以原始力计（不计月令+2与合局转移——丁巳己酉癸巳戊午酉含月令成4而火海仍化）
        val fHao = (bazi.stems.count { it.element == hao } + bazi.branches.count { it.element == hao }).toDouble()
        val q3b = fHao <= f[z]!!
        val q4 = f[weakLine]!! <= 0.8 * f[strongLine]!! || f[z]!! >= f[strongLine]!!
        if (q1 && q2 && q3 && q3b && q4) {
            return HeHuaVerdict(a to b, z, true,
                "${a.chinese}${b.chinese}合化${z.chinese}——真化：力${f[z]}有根" +
                    (if (tongOk) "（通关${tong!!.chinese}）" else "") + "（${z.chinese}行从强）")
        }
        // 印重埋没（生正化之行≥2.5×正化行且正化行<2.5——土重埋金/木多晦火）→ 连反化不成
        val yin = Element.entries.first { it.generates == z }
        // 埋没/晦光：印（生Z）或泄（Z生）之行自身有力（≥2.5）且≥2.5×Z且Z<2.5——
        // 土重埋金（印）、土方局晦火无光（泄——乙丑癸未戊辰壬戌泄土7晦火0）；只拦克型
        val mai = (f[yin]!! >= 2.5 || f[z.generates]!! >= 2.5) &&
            (f[yin]!! >= 2.5 * f[z]!! || f[z.generates]!! >= 2.5 * f[z]!!) && f[z]!! < 2.5
        // 反化双模式（强制作用——以力服人）：
        // 克型：强干行克弱干行+≥2倍+有根基（甲征服己化木）；
        // 势型：弱干无根（≤1.5）+强干≥2倍+**化神得令**（大势所趋——戊申辛酉丙申戊戌丙从辛化金，
        // 丙如火中残烛连暖金之力亦不足；从财格之理）
        if (strongLine != z && f[strongLine]!! >= 2 * f[weakLine]!!) {
            val ke2 = Element.entries.first { it.controls == strongLine }
            // 印重埋没只拦克型（生正化行≥2.5且≥2.5×正化行——土重埋金）；势服式得令大势不受埋
            val modeKe = !mai && strongLine.controls == weakLine && genJi(strongLine)
            val modeShi = f[weakLine]!! <= 1.5 && yueXing == strongLine
            if ((modeKe || modeShi) && f[ke2]!! < f[strongLine]!!) {
                return HeHuaVerdict(a to b, strongLine, true,
                    "${a.chinese}${b.chinese}合——正化${z.chinese}不成而**反化**${strongLine.chinese}：强干${strongLine.chinese}${f[strongLine]}≥2×弱干${weakLine.chinese}${f[weakLine]}" +
                    (if (modeKe) "（征服式）" else "（势服式——弱干无根化神得令，从格之理）"))
            }
        }
        val why = listOfNotNull(
            if (!q1) "力${f[z]}<2.5" else null,
            if (!q2) "无根基" else null,
            if (!q3) "被${ke.chinese}克${f[ke]}" else null,
            if (!q3b) "被${hao.chinese}耗${fHao}" else null,
            if (!q4) "两干力均互绊${f[lineA]}:${f[lineB]}" else null,
            if (mai) "印${yin.chinese}${f[yin]}埋没" else null,
        ).joinToString("，")
        return HeHuaVerdict(a to b, z, false, "${a.chinese}${b.chinese}合——合而不化（假化）：$why")
    }
}

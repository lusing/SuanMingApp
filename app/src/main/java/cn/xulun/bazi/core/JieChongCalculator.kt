package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.ChongJie
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.JieChongResult
import cn.xulun.bazi.model.XingJie

/**
 * 会合解冲刑（《子平真诠》第七章）。逐字移植自 xuanzong `utils/bazi/xingChongJieFa.js`。
 *
 * 一个冲/刑支若与另一支成六合或三合(会)，则该冲/刑被解去（不冲/不刑）。
 * 复用 [DiZhiRelationCalculator]（三合/六冲/相刑）与 [HeHuaCalculator]（六合）。
 *
 * 局限（后置）：合神自身被三合抽去→冲复返（ch7「会合不能解」），本版未建模，
 * 仅判「冲支是否被完整六合/三合所绊」。
 */
object JieChongCalculator {

    fun analyze(bazi: BaziResult): JieChongResult {
        val drel = DiZhiRelationCalculator.analyze(bazi)
        val hrel = HeHuaCalculator.analyze(bazi)

        // 合住支：参与完整六合对 或 完整三合局 的支
        val liuHeOf = HashMap<EarthlyBranch, EarthlyBranch>() // 支 → 六合 partner
        for (lh in hrel.branchLiuHeList) {
            liuHeOf[lh.branch1] = lh.branch2
            liuHeOf[lh.branch2] = lh.branch1
        }
        val sanHeSet = HashSet<EarthlyBranch>()
        for (sh in drel.sanHeList) sanHeSet.addAll(sh.branches)

        fun boundBy(b: EarthlyBranch): String? {
            if (b in sanHeSet) return "三合"
            liuHeOf[b]?.let { return "六合（${it.chinese}）" }
            return null
        }

        // 冲解：任一冲支被合住→冲解
        val chong = drel.chongList.map { c ->
            val b1by = boundBy(c.branch1)
            val b2by = boundBy(c.branch2)
            val by = b1by ?: b2by // 解神；null 表示无解
            val resolved = by != null
            val who = if (b1by != null) c.branch1.chinese else c.branch2.chinese
            ChongJie(
                branch1 = c.branch1, branch2 = c.branch2, description = c.description,
                resolved = resolved,
                resolveBy = if (resolved) "$who${by!!}" else null,
                note = if (resolved) "${c.description}：${who}为${by!!}所绊，冲不成（会合解冲）"
                else "${c.description}：无会合相解，冲成立",
            )
        }

        // 刑解：任一刑支被合住→刑解
        val xing = drel.xingList.map { x ->
            val resolver = x.branches.map { it to boundBy(it) }.firstOrNull { it.second != null }
            val resolved = resolver != null
            val involvedStr = x.branches.joinToString("") { it.chinese }
            XingJie(
                description = x.description, kind = x.kind, involved = involvedStr,
                resolved = resolved,
                resolveBy = if (resolved) "${resolver!!.first.chinese}${resolver.second}" else null,
                note = if (resolved) "${x.description}：${resolver.first.chinese}为${resolver.second}所绊，刑不成（会合解刑）"
                else "${x.description}：无会合相解，刑成立",
            )
        }

        return JieChongResult(
            chong = chong,
            xing = xing,
            hasChong = chong.isNotEmpty(),
            hasUnresolvedChong = chong.any { !it.resolved },
            hasUnresolvedXing = xing.any { !it.resolved },
            liuHeCount = hrel.branchLiuHeList.size,
            sanHeCount = drel.sanHeList.size,
        )
    }

    /** 月令是否被「未解之冲」所犯（供成败判定 月令被冲→败）。 */
    fun monthBranchUnresolvedChong(bazi: BaziResult): Boolean {
        val monthBranch = bazi.monthPillar.branch
        val r = analyze(bazi)
        return r.chong.any { !it.resolved && (it.branch1 == monthBranch || it.branch2 == monthBranch) }
    }
}

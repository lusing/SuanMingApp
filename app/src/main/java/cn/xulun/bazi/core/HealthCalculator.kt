package cn.xulun.bazi.core

import cn.xulun.bazi.core.LiuQinXiJi.categoryOf
import cn.xulun.bazi.core.LiuQinXiJi.elementOf
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HealthResult
import cn.xulun.bazi.model.LiuQinItem

/**
 * 健康引擎（.health-reference.md）。原局基调（健康线）+ 疾病倾向（受损五行→脏腑）。
 *
 * 健康三线：①身中和或格成从象五行循环相生；②身旺有克有泄而不亢旺；③天乙/天德/月德贵人照命。
 * 疾病倾向：某行被克泄耗之行合力压过自身（弱而不从）→按五行脏腑对应列易患之疾。
 * 疾病应期（岁运引发）见后续疾病分类引擎。
 */
object HealthCalculator {

    /** 五行→脏腑疾病对应（阳干腑/阴干脏合并陈述）。 */
    private val elementDisease = mapOf(
        Element.WOOD to "肝胆之疾（甲胆乙肝）、头疾肩周炎",
        Element.FIRE to "心脏小肠之疾（丁心丙小肠）、舌疮额痛眼疾",
        Element.EARTH to "脾胃之疾（戊胃己脾）、鼻面之患",
        Element.METAL to "肺与大肠之疾（辛肺庚大肠）、胸患筋骨疼痛",
        Element.WATER to "肾膀胱之疾（壬膀胱癸肾）、胫痛足疾生殖泌尿之患",
    )
    private val elementZang = mapOf(
        Element.WOOD to "肝胆", Element.FIRE to "心小肠", Element.EARTH to "脾胃",
        Element.METAL to "肺大肠", Element.WATER to "肾膀胱",
    )

    fun analyze(bazi: BaziResult): HealthResult {
        val dayEl = bazi.dayPillar.stem.element
        val d = LiuQinXiJi.derive(bazi)
        val forces = BingYaoCalculator.forces(bazi)
        val bing = BingYaoCalculator.analyze(bazi)
        val engVerdict = StrengthEngine.analyze(bazi).verdict

        val items = mutableListOf<LiuQinItem>()

        // ① 身中和或格成从象——五行循环相生生化有情
        if (bing.congGe != null) {
            items += LiuQinItem(
                "从象·生化有情",
                "格成从象（${bing.congGe!!.basis.take(30)}），五行顺势循环相生，逢一般岁运均生化有情——一生无大灾大难，身体健康（例一式）。",
            )
        } else if (engVerdict == DayStrength.BALANCED) {
            items += LiuQinItem(
                "身中和",
                "日干中和，五行生化有情——身体康健之基，纵病亦轻（例一式）。",
            )
        }

        // ② 身旺有克有泄而不亢旺
        val guanEl = elementOf(GodCategory.GUAN, dayEl)
        val shiEl = elementOf(GodCategory.SHI, dayEl)
        val caiEl = elementOf(GodCategory.CAI, dayEl)
        val hasGuan = forces[guanEl]!! >= 0.5 || bazi.stems.any { it.element == guanEl } ||
            bazi.branches.any { it.element == guanEl }
        val hasXieHao = forces[shiEl]!! >= 0.5 || forces[caiEl]!! >= 1.0 ||
            bazi.branches.any { it.element == shiEl || it.element == caiEl }
        if (engVerdict == DayStrength.STRONG && hasGuan && hasXieHao && forces[dayEl]!! <= 5.5) { // 制泄有力则不为亢(例二月令加成后5.0仍健康)
            items += LiuQinItem(
                "身旺有制泄",
                "身旺而有官杀制、食伤财星泄耗，不成亢旺——气血流通，一生健康，连感冒都少（例二式）。",
            )
        }

        // ③ 天乙/天德/月德贵人照命
        val ss = ShenShaCalculator.analyze(bazi).items
        val guiRen = ss.filter { it.name in setOf("天乙贵人", "天德贵人", "月德贵人") }
        if (guiRen.isNotEmpty()) {
            val guiFav = guiRen.any { g ->
                g.branch?.let { br -> categoryOf(br.element, dayEl) in d.fav } == true
            } || guiRen.any { it.pillarLabel.startsWith("年") || it.pillarLabel.startsWith("月") }
            if (guiFav) {
                items += LiuQinItem(
                    "贵人照命",
                    "${guiRen.distinctBy { it.name }.joinToString("、") { it.name }}照命为喜——一生安康，逢灾有解（例三式）。",
                )
            }
        }

        // ── 疾病倾向:各行受损度 ──
        val risks = mutableListOf<LiuQinItem>()
        val cong = bing.congGe != null
        Element.entries.forEach { el ->
            val own = forces[el]!!
            if (own <= 0.5 && !cong) return@forEach
            if (cong && own <= 1.0) return@forEach // 从格之行及微行不以损论
            val keEl = Element.entries.first { it.controls == el }
            val xieEl = el.generates
            val haoEl = el.controls
            val crowd = forces[keEl]!! + forces[xieEl]!! + forces[haoEl]!!
            // 水多木漂：生我之行失衡（力≥2.5倍且自身弱）
            val shengEl = Element.entries.first { it.generates == el }
            val piao = forces[shengEl]!! >= 2.5 * own && own in 0.5..2.0
            val kangWang = own >= 4.5 && forces[keEl]!! <= 1.0 // 亢旺无制
            when {
                crowd >= 2.0 * own && own < 3.0 -> risks += LiuQinItem(
                    "${el.chinese}行受损·${elementZang[el]}",
                    "${el.chinese}行力仅$own，被克泄耗之行合力$crowd（${keEl.chinese}克${xieEl.chinese}泄${haoEl.chinese}耗）——${elementDisease[el]}；" +
                        "逢克泄耗${el.chinese}之行的大运流年易发病，宜提前调养（通关之行到位则缓解）。",
                )
                piao -> risks += LiuQinItem(
                    "${shengEl.chinese}多${el.chinese}${if (el == Element.WOOD) "漂" else "失衡"}·${elementZang[el]}",
                    "生${el.chinese}之${shengEl.chinese}（力${forces[shengEl]}）远超${el.chinese}自身（$own）——${if (el == Element.WOOD) "水多木漂" else "生扶失衡"}，${elementDisease[el]}。",
                )
                kangWang -> risks += LiuQinItem(
                    "${el.chinese}行亢旺·${elementZang[el]}",
                    "${el.chinese}行力${own}亢旺而克星${keEl.chinese}力仅${forces[keEl]}——太过亦病（亢则为害），${elementDisease[el]}。",
                )
            }
        }

        val summary = "五行对应：木肝胆/火心小肠/土脾胃/金肺大肠/水肾膀胱（阳干腑阴干脏）。" +
            "原局储藏一生疾病信息，发病须待岁运引发——逢受损五行之克泄耗冲刑的岁运宜防，通关之行到位则缓解。"
        return HealthResult(items = items, risks = risks, summary = summary)
    }
}

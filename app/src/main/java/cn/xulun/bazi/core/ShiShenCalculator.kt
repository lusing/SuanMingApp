package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.ShiShenResult
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.TenGod.ZHENG_YIN
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 十神关系（柱位寓意）计算器。理论见 `.shengke-reference.md` 第六三节起。
 *
 * 两条检测线：
 * - **透干十神**（年/月/时干对日主）×柱位 → 寓意表；
 * - **坐支组合**（透干十神, 支藏本气十神）→ 坐支寓意表（如正印坐正财）。
 * 喜忌注记由 BingYao 用神行/忌神行推断。
 */
object ShiShenCalculator {

    private val pillarStemLabels = listOf("年干", "月干", "日干", "时干")
    private val pillarBranchLabels = listOf("年支", "月支", "日支", "时支")

    // ── 正印透干寓意（第六三节一；2=日支本气档并入坐支线）──
    private val zhengYinTou: Map<Int, String> = mapOf(
        0 to "旺相生富贵书香之家；月透正官祖上父辈掌权；喜用利文途；临长生母聪慧贤良体健寿长；" +
            "身旺印旺无制孤寒刑克少子息；他干又透印少时缺乳或饮他人乳（印多克食）或由他人抚养；" +
            "衰弱遭克祖上贫寒幼时家贫；日主当令三透印财不现与妻缘薄婚姻不顺（特殊格局除外）；" +
            "印衰月透劫财祖产多被手足继承",
        1 to "喜用不遭财克学业有成文才出众闻名四方；喜用官星生印身官印均衡功名显达名扬天下；" +
            "年透正官出身显贵；失令遭财克出身贫寒；日干太强支印重为忌月为兄弟宫手足缘薄或兄妹少",
        3 to "喜用子嗣贤孝多生贵子享子女福；晚运佳大器晚成（月正官岁运逢印必发达）；忌神难享子女福；" +
            "喜用财得令官衰不见不通财印之关一生事业难成贫苦命",
    )

    // ── 正印坐支寓意（第六三节二：坐支本气十神 → 文案）──
    private val zhengYinZuo: Map<TenGod, String> = mapOf(
        TenGod.ZHENG_YIN to "印太过过于自尊欲望太高反遭失败；忌时辛苦奔波克子星子女缘薄（女命尤甚）",
        TenGod.PIAN_YIN to "多愁善感子女缘薄多败少成（女命尤甚）",
        TenGod.BI_JIAN to "乐于帮助兄弟朋友；喜用事业顺利发达",
        TenGod.JIE_CAI to "为帮兄弟朋友受小损但对财运无大妨碍",
        TenGod.SHANG_GUAN to "组合不好伤官为忌多遇挫折功败垂成；伤官有制另论",
        TenGod.SHI_SHEN to "印为尊敬信任食为财源——名利俱佳吉祥如意",
        TenGod.ZHENG_CAI to "婆媳不睦忧虑多病；财旺为忌难遇良机贫病愁苦",
        TenGod.PIAN_CAI to "财利快意家庭和睦",
        TenGod.ZHENG_GUAN to "正直诚信慈善有德受人尊敬胸怀宽广忍辱负重可成大器；女命贤内助",
        TenGod.QI_SHA to "杀印相生喜用——勤奋上进恩威并具功成名就",
    )

    fun analyze(bazi: BaziResult): ShiShenResult {
        val dayMaster = bazi.dayPillar.stem
        val items = mutableListOf<ShiShenResult.ShiShenItem>()
        val xiJiMap = buildXiJi(bazi)

        fun lineOf(stem: HeavenlyStem) = stem.element
        fun add(label: String, god: TenGod, el: Element, text: String?) {
            if (text != null) items += ShiShenResult.ShiShenItem(label, god, text, xiJiMap[el] ?: "中性")
        }

        // ① 透干十神 ×柱位（日干自身跳过）
        for (i in listOf(0, 1, 3)) {
            val st = bazi.pillars[i].stem
            val g = tenGodOfStem(dayMaster, st)
            add(pillarStemLabels[i] + g.chinese, g, lineOf(st), touText(g, i))
        }
        // ② 坐支组合：透干十神(日支则支藏本气) × 支藏本气十神
        for (i in 0..3) {
            val hiddenMain = branchHiddenStems.getValue(bazi.pillars[i].branch).first()
            val zuo = tenGodOfStem(dayMaster, hiddenMain)
            val tou: TenGod = if (i == 2) zuo else tenGodOfStem(dayMaster, bazi.pillars[i].stem)
            add(pillarBranchLabels[i] + "坐" + zuo.chinese + "（" + tou.chinese + "坐" + zuo.chinese + "）", tou, lineOf(hiddenMain), zuoText(tou, zuo, i))
        }
        return ShiShenResult(items)
    }

    /** 透干寓意查表（正印成表；其余十神待教材续节扩展）。 */
    private fun touText(g: TenGod, pillar: Int): String? = when (g) {
        ZHENG_YIN -> zhengYinTou[pillar]
        else -> null
    }

    /** 坐支寓意查表 (透干十神, 坐支本气十神)；日支（2）出正印本气档。 */
    private fun zuoText(tou: TenGod, zuo: TenGod, pillar: Int): String? = when (tou) {
        ZHENG_YIN -> zhengYinZuo[zuo]
        else -> if (pillar == 2 && zuo == ZHENG_YIN)
            "日支本气为印——喜用得妻妾之助有贵人相帮；忌神夫妻不睦或不利子息" else null
    }

    /** 喜忌注记：BingYao 用神行/忌神行。 */
    private fun buildXiJi(bazi: BaziResult): Map<Element, String> {
        val r = BingYaoCalculator.analyze(bazi)
        val map = mutableMapOf<Element, String>()
        map[r.yongShenElement] = "喜用"
        val dayEl = bazi.dayPillar.stem.element
        r.jiShen.forEach { j ->
            val el = when (j) {
                GodCategory.BI -> dayEl
                GodCategory.YIN -> Element.entries.first { it.generates == dayEl }
                GodCategory.SHI -> dayEl.generates
                GodCategory.CAI -> dayEl.controls
                GodCategory.GUAN -> Element.entries.first { it.controls == dayEl }
            }
            if (el !in map) map[el] = "忌神"
        }
        return map
    }
}

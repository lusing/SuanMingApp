package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.XingGeResult

/**
 * 性格分析计算器（五行气势+十神心性+神煞性情多线）。理论见 `.shengke-reference.md` 第六五节。
 *
 * 五行线：主导五行（力≥2.5 且份额最大且不过太旺＜0.6）→通用心性；
 * 参照日干阴阳取阳干/阴干细注。交战局（前二力差≤1.5 且相克）加杀伐注记。
 */
object XingGeCalculator {

    private val common = mapOf(
        Element.WOOD to "性情随和感情丰富心地善良慈悲为怀富人情味",
        Element.FIRE to "举止文明彬彬有礼性格刚烈快人快语容易冲动",
        Element.EARTH to "性情敦厚感情纯朴做事稳重踏实讲信用性格倔强有强烈的追求精神",
        Element.METAL to "为人讲义气办事果断性格直爽敢作敢为有时争强好胜坚持己见",
        Element.WATER to "聪明好学足智多谋接受能力强一生多走动奔波",
    )
    private val yang = mapOf(
        Element.WOOD to "甲木：刚直豪迈有开拓精神勤勉随和富人情味；有时固执独断好自我显示虚伪多欺",
        Element.FIRE to "丙火：热情豪爽自信好胜心强善言健谈；性情激动虚荣急躁办事疏漏",
        Element.EARTH to "戊土：胸怀广阔乐于助人",
        Element.METAL to "庚金：勇敢好进取义理分明自尊心强；刚愎自用爱弄权术",
        Element.WATER to "壬水：心胸宽广机智灵敏积极进取；事必反复冲动易怒",
    )
    private val yin = mapOf(
        Element.WOOD to "乙木：感情细腻理智平和不愿与人争执；优柔寡断依赖性强",
        Element.FIRE to "丁火：憨厚质朴重信守义稳重有礼修身养性；缺乏耐力做事易半途而废易急躁",
        Element.EARTH to "己土：反应迟钝不知变通",
        Element.METAL to "辛金：个性坦直无私脚踏实地办事稳重；自信刻薄",
        Element.WATER to "癸水：聪明机灵斯文内象遇事冷静；心胸狭窄工于心计",
    )

    fun analyze(bazi: BaziResult): XingGeResult {
        val items = mutableListOf<XingGeResult.XingGeItem>()
        val dayEl = bazi.dayPillar.stem.element
        val f = BingYaoCalculator.forces(bazi)
        val total = Element.entries.sumOf { f[it]!! }.coerceAtLeast(0.001)
        val yangDay = bazi.dayPillar.stem.ordinal % 2 == 0

        // 五行线：主导而行（≥2.5 份额最大且非过旺——"不至于太过"）
        val dom = Element.entries.maxBy { f[it]!! }
        if (f[dom]!! >= 2.5 && f[dom]!! / total < 0.6) {
            val stemNote = if (yangDay) yang[dom]!! else yin[dom]!!
            items += XingGeResult.XingGeItem(
                dim = "气势在${dom.chinese}",
                text = common[dom]!! + "。" + stemNote + "。"
            )
        }
        // 日干本气线（日主自坐心性）
        items += XingGeResult.XingGeItem(
            dim = "日主${bazi.dayPillar.stem.chinese}${dayEl.chinese}",
            text = (if (yangDay) yang[dayEl]!! else yin[dayEl]!!) + "。"
        )
        // 交战杀伐注记：前二行相克力差≤1.5
        val sorted = Element.entries.sortedByDescending { f[it]!! }
        val a = sorted[0]; val b2 = sorted[1]
        if (f[a]!! >= 2.5 && kotlin.math.abs(f[a]!! - f[b2]!!) <= 1.5 &&
            (a.controls == b2 || b2.controls == a)) {
            items += XingGeResult.XingGeItem(
                dim = "${a.chinese}${b2.chinese}交战",
                text = "${a.chinese}${b2.chinese}两强交战——四柱具有杀伐之象（${a.chinese}众欺${b2.chinese}），讲义气好财喜为财争斗，防因财或女人之事而犯罪（重庆王先生辛酉辛卯庚子甲申式——子水无势金多水浊难解冲战之关）"
            )
        }
        // 土水混杂（双子冲荡湿土——狡猾善变侥幸犯大错）
        if (f[Element.WATER]!! >= 3.0 && f[Element.EARTH]!! >= 2.0 && dayEl == Element.WATER) {
            items += XingGeResult.XingGeItem(
                dim = "土水混杂",
                text = "水众冲荡湿土土水混杂——狡猾善变冲动易怒，会因一时心存侥幸而犯下大错（壬子癸丑壬子戊申式）"
            )
        }
        // 两戌冲辰起土旺（邓女士式倔强急躁）
        val bs = bazi.branches
        if (bs.count { it == cn.xulun.bazi.model.EarthlyBranch.XU } >= 2 && cn.xulun.bazi.model.EarthlyBranch.CHEN in bs) {
            items += XingGeResult.XingGeItem(
                dim = "两戌冲辰",
                text = "两戌冲辰冲起土旺——倔强事业心强有强烈追求精神，重面子讲信用做事急躁缺乏耐心（鄂州邓女士庚戌丙戌戊辰戊午式）"
            )
        }
        // ── 十神线（第六五节二：力量适中显正面/太过不及显负面；强旺者显之）──
        run {
            val dayMaster = bazi.dayPillar.stem
            val total2 = Element.entries.sumOf { f[it]!! }.coerceAtLeast(0.001)
            val tou = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
            val by = BingYaoCalculator.analyze(bazi)
            val jiLines = mutableSetOf<Element>()
            var yongLine: Element? = null
            val dayEl2 = dayEl
            fun lineOf(cat: cn.xulun.bazi.model.GodCategory?): Element? = when (cat) {
                cn.xulun.bazi.model.GodCategory.BI -> dayEl2
                cn.xulun.bazi.model.GodCategory.YIN -> Element.entries.first { it.generates == dayEl2 }
                cn.xulun.bazi.model.GodCategory.SHI -> dayEl2.generates
                cn.xulun.bazi.model.GodCategory.CAI -> dayEl2.controls
                cn.xulun.bazi.model.GodCategory.GUAN -> Element.entries.first { it.controls == dayEl2 }
                else -> null
            }
            yongLine = lineOf(by.yongShen)
            by.jiShen.forEach { j -> lineOf(j)?.let { jiLines += it } }
            for (st in tou) {
                val g = cn.xulun.bazi.model.tenGodOfStem(dayMaster, st)
                val el = st.element
                if (f[el]!! < 2.0) continue  // 衰弱者藏之
                val isYong = el == yongLine
                val isJi = el in jiLines
                val strong = f[el]!! >= 0.35 * total2
                val tooMuch = f[el]!! >= 0.5 * total2
                val zhengMian = isYong || (!isJi && strong && !tooMuch)
                val text = when (g) {
                    cn.xulun.bazi.model.TenGod.ZHENG_GUAN -> if (zhengMian) "品性端正光明磊落，做事稳重踏实讲信用办事认真重视名誉，富创业精神和责任感" else "固执刻板胆小怕事，维持现状明哲保身不思进取"
                    cn.xulun.bazi.model.TenGod.QI_SHA -> if (zhengMian) "积极进取豪迈直爽，正直侠义随机应变" else "叛逆偏激霸道，狡猾猜忌无视礼法——有犯罪信息（湖南赵经理式）"
                    cn.xulun.bazi.model.TenGod.ZHENG_YIN -> if (zhengMian) "平生少病心地善良慈悲为怀乐于助人，不拘文武皆掌印权" else "缺乏进取人云亦云爱耍聪明，庸碌少成"
                    cn.xulun.bazi.model.TenGod.PIAN_YIN -> if (zhengMian) "精明能干兴趣广泛聪明善变攻击性强，随机应变奇思妙想" else "尖酸刻薄自我主观个性怪异性格内向多学少成"
                    cn.xulun.bazi.model.TenGod.ZHENG_CAI -> if (zhengMian) "经济头脑勤劳节俭为人现实忠于家庭责任感强" else "好逸恶劳占有欲强能力欠佳得过且过"
                    cn.xulun.bazi.model.TenGod.PIAN_CAI -> if (zhengMian) "慷慨大方人缘颇佳豪爽风流能赚会花厌恶小气" else "贪图虚荣浪费虚掷，太过则到处留情易为情所累、钱财易得易去（四川彭先生式）"
                    cn.xulun.bazi.model.TenGod.BI_JIAN -> if (zhengMian) "性稳刚毅豪迈义气积极上进性喜自由" else "倔强固执争财夺利不聚财产，打抱不平说直话易得罪人（河北王女士式）"
                    cn.xulun.bazi.model.TenGod.JIE_CAI -> if (zhengMian) "热诚坦直喜交朋友性格乐观勇往直前" else "盲目冲动蛮横暴躁独断专行缺乏理智"
                    cn.xulun.bazi.model.TenGod.SHI_SHEN -> if (zhengMian) "斯文随和待人宽厚善良体贴乐天知命艺术感强，好歌舞鹤立鸡群" else "老实懦弱贪图享受假道斯文纵于声色"
                    cn.xulun.bazi.model.TenGod.SHANG_GUAN -> if (zhengMian) "性喜自由悟性特高才华横溢随机应变，善抓机会表现自己擅长演讲口若悬河" else "放荡不羁自命不凡招人嫉妒是非不断，与官不合官司连连（重庆张先生式——伤官制官喜与官斗）"
                }
                items += XingGeResult.XingGeItem(
                    dim = "十神" + g.chinese + "（力${f[el]}${if (zhengMian) "·正" else "·负"}）",
                    text = text + "。"
                )
            }
        }
        // ── 神煞线（第六五节三：力量适中正/太过不及负）──
        run {
            val ss = ShenShaCalculator.analyze(bazi).items
            val names = ss.map { it.name }.toSet()
            val cnt = ss.groupingBy { it.name }.eachCount()
            data class Ss(val n: String, val pos: String?, val neg: String?)
            val table = listOf(
                Ss("天德贵人", "心地善良聪明智慧做事公道，一生少病官刑不犯——遇难呈祥", null),
                Ss("月德贵人", "心地善良聪明智慧，遇难呈祥逢凶化吉", null),
                Ss("天乙贵人", "气宇轩昂聪明智慧，官禄易得出入遇贵", null),
                Ss("华盖", "性格孤僻喜静多思，谋略深远才华横溢具艺术天赋，喜神秘事物悟性高", null),
                Ss("太极贵人", "喜五术玄学，有天然预知能力见解超群，内心孤独多愁善感", null),
                Ss("驿马", "喜动一生漂泊动中求财奔波忙碌，适合新闻外交营销", null),
                Ss("文昌", "智慧超群气质雅秀，好学新知上进心强", null),
                Ss("魁罡", "聪明伶俐文章振发临事果断，绝伦显贵", "贫寒彻骨婚姻不顺易犯牢狱伤灾"),
                Ss("国印贵人", "诚实可靠严守清规办事公道，气质轩昂有掌印潜能", null),
                Ss("学堂", "聪明机巧秀气生发学业有成", null),
                Ss("将星", "军界出身有组织领导才能，办事有威信受人拥戴", null),
                Ss("金神", "聪明好学有才干性急刚烈，用神得位不富则贵", "一生多学少成六亲无义骨肉分离易受金器所伤"),
                Ss("羊刃", "刻苦勤勉积极向上努力进取", "心高气傲暴躁凶狠多伤残之灾或遭官非（身旺逢刃灾祸连连）"),
                Ss("劫煞", "聪慧伶敏才智超群好学上进", "昏庸愚蠢奸猾狡诈执拗凶狠"),
                Ss("亡神", "峻厉有威料事如神具城府善谋略", "奸诈狂妄心胸狭窄轻浮浪荡酒色风流"),
                Ss("桃花", "聪明智慧漂亮潇洒慷慨大方具艺术天赋", "贪恋酒色落魄不检疏财好欢破散家业"),
                Ss("阴阳差错", null, "做事缺乏持久婚姻不顺"),
                Ss("空亡", "空亡叠叠聪明伶俐思维敏捷，为人大气不斤斤计较——空则能容空则灵秀", null),
            )
            for (e in table) {
                if (e.n !in names) continue
                val c = cnt[e.n] ?: 1
                val tooMuch = e.n == "空亡" && c >= 2
                val text = if (tooMuch && e.neg != null) e.neg!! else (e.pos ?: e.neg ?: continue)
                items += XingGeResult.XingGeItem(
                    dim = "神煞" + e.n + (if (c >= 2) "×$c" else ""),
                    text = text + (if (e.neg != null && !tooMuch) "（力量适中·正面；太过则${e.neg}）" else "。")
                )
            }
        }
        return XingGeResult(items)
    }
}

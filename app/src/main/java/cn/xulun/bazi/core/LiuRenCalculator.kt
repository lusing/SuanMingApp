package cn.xulun.bazi.core

import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.LiuRenPlate
import cn.xulun.bazi.model.Lesson
import cn.xulun.bazi.model.NianMing
import cn.xulun.bazi.model.ThreeChuan
import cn.xulun.bazi.model.TianJiang
import cn.xulun.bazi.model.TianJiangOnPlate
import cn.xulun.bazi.model.YueJiang
import cn.xulun.bazi.model.ZongMen
import java.lang.Math.floorMod
import java.time.LocalDateTime

/**
 * 大六壬起课：月将 → 天盘 → 四课 → 三传(九宗门) → 天将 → 年命。
 * 复用 [BaziCalculator]（日干支/时支）与 [SolarTerm]（中气→月将）。规则见 .liuren-reference.md。
 */
object LiuRenCalculator {

    /** 贵人顺布的地盘支集（亥子丑寅卯辰）；其外（巳午未申酉戌）逆布。 */
    private val shunSet: Set<EarthlyBranch> = setOf(
        EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU, EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN,
    )

    /** 昼时支 */
    private val dayHours: Set<EarthlyBranch> = setOf(
        EarthlyBranch.MAO, EarthlyBranch.CHEN, EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI, EarthlyBranch.SHEN,
    )

    /** 返吟无克（井栏射/无亲）6 日 */
    private val jingLanShe: Set<String> = setOf("丁丑", "己丑", "辛丑", "丁未", "己未", "辛未")

    // 天干五合
    private val ganHe: Map<HeavenlyStem, HeavenlyStem> = mapOf(
        HeavenlyStem.JIA to HeavenlyStem.JI, HeavenlyStem.JI to HeavenlyStem.JIA,
        HeavenlyStem.YI to HeavenlyStem.GENG, HeavenlyStem.GENG to HeavenlyStem.YI,
        HeavenlyStem.BING to HeavenlyStem.XIN, HeavenlyStem.XIN to HeavenlyStem.BING,
        HeavenlyStem.DING to HeavenlyStem.REN, HeavenlyStem.REN to HeavenlyStem.DING,
        HeavenlyStem.WU to HeavenlyStem.GUI, HeavenlyStem.GUI to HeavenlyStem.WU,
    )

    fun buildPlate(
        dt: LocalDateTime,
        gender: Gender? = null,
        age: Int? = null,
        benMing: EarthlyBranch? = null,
    ): LiuRenPlate {
        val bazi = BaziCalculator.calculate(dt)
        val nianMing = computeNianMing(gender, age, benMing, bazi.yearPillar.branch)
        return compute(
            bazi.dayPillar.stem, bazi.dayPillar.branch, bazi.hourPillar.branch,
            determineYueJiang(dt).branch, bazi.hourPillar.cycleIndex,
            bazi.yearPillar.branch, bazi.monthPillar.branch, nianMing,
        )
    }

    /** 供测试/直接调用：由日干支 + 月将 + 占时起课（跳过日期→干支与月将换算） */
    internal fun compute(
        dayStem: HeavenlyStem,
        dayBranch: EarthlyBranch,
        hourBranch: EarthlyBranch,
        yueJiang: EarthlyBranch,
        hourCycleIndex: Int = 0,
        yearBranch: EarthlyBranch = EarthlyBranch.ZI,
        monthBranch: EarthlyBranch = EarthlyBranch.YIN,
        nianMing: NianMing = NianMing(null, null),
    ): LiuRenPlate {
        val offset = floorMod(yueJiang.ordinal - hourBranch.ordinal, 12)
        val isDay = hourBranch in dayHours
        val lessons = fourLessons(dayStem, dayBranch, offset)
        val threeChuan = threeChuan(dayStem, dayBranch, hourBranch, yueJiang, offset, lessons)
        val tianJiang = placeTianJiang(dayStem, offset, isDay)
        return LiuRenPlate(
            yueJiang = YueJiang.of(yueJiang),
            hourBranch = hourBranch,
            dayStem = dayStem,
            dayBranch = dayBranch,
            hourCycleIndex = hourCycleIndex,
            yearBranch = yearBranch,
            monthBranch = monthBranch,
            isDay = isDay,
            lessons = lessons,
            threeChuan = threeChuan,
            tianJiang = tianJiang,
            nianMing = nianMing,
            tianPanOffset = offset,
        )
    }

    /** 月将：取占时前最近中气（termNames odd 索引 1,3,…,23），月将支序 = floorMod(11-(k-1)/2,12) */
    fun determineYueJiang(dt: LocalDateTime): YueJiang {
        val timeline = ((dt.year - 1)..(dt.year + 1)).flatMap { y ->
            val terms = SolarTerm.getYearSolarTerms(y)
            (1..23 step 2).map { idx -> idx to terms[idx] }
        }.sortedBy { it.second }
        var curIdx = timeline.first().first
        for ((idx, time) in timeline) {
            if (!time.isAfter(dt)) curIdx = idx else break
        }
        val ord = floorMod(11 - (curIdx - 1) / 2, 12)
        return YueJiang.of(EarthlyBranch.fromIndex(ord))
    }

    /** 天盘某地盘位之上神 */
    private fun heavenAt(earthBranch: EarthlyBranch, offset: Int): EarthlyBranch =
        EarthlyBranch.fromIndex(floorMod(earthBranch.ordinal + offset, 12))

    /** 四课 */
    private fun fourLessons(dayStem: HeavenlyStem, dayBranch: EarthlyBranch, offset: Int): List<Lesson> {
        val ganJi = LiuRenTables.jiGong.getValue(dayStem)
        val top1 = heavenAt(ganJi, offset)
        val top2 = heavenAt(top1, offset)
        val top3 = heavenAt(dayBranch, offset)
        val top4 = heavenAt(top3, offset)
        return listOf(
            Lesson(1, top1, ganJi, dayStem),
            Lesson(2, top2, top1),
            Lesson(3, top3, dayBranch),
            Lesson(4, top4, top3),
        )
    }

    /** 三传（九宗门决策树） */
    private fun threeChuan(
        dayStem: HeavenlyStem,
        dayBranch: EarthlyBranch,
        hourBranch: EarthlyBranch,
        yueJiang: EarthlyBranch,
        offset: Int,
        lessons: List<Lesson>,
    ): ThreeChuan {
        val dayKey = "${dayStem.chinese}${dayBranch.chinese}"

        // 伏吟：月将 == 占时
        if (yueJiang == hourBranch) {
            val c = LiuRenTables.parseChuan(LiuRenTables.fuYin.getValue(dayKey))
            return ThreeChuan(c[0], c[1], c[2], ZongMen.FU_YIN, "伏吟·${fuYinSub(dayStem)}")
        }
        // 返吟：(月将 - 占时) mod 12 == 6
        if (floorMod(yueJiang.ordinal - hourBranch.ordinal, 12) == 6) {
            val c = LiuRenTables.parseChuan(LiuRenTables.fanYin.getValue(dayKey))
            val sub = if (dayKey in jingLanShe) "井栏射(无亲)" else "无依"
            return ThreeChuan(c[0], c[1], c[2], ZongMen.FAN_YIN, "返吟·$sub")
        }

        // 有克贼 → 重审/元首/比用/涉害
        val keZei = lessons.filter { it.hasKeZei }
        if (keZei.isNotEmpty()) {
            val (c1, zongMen, sub) = chuanFromKeZei(keZei, dayStem, lessons)
            val c2 = heavenAt(c1, offset)
            val c3 = heavenAt(c2, offset)
            return ThreeChuan(c1, c2, c3, zongMen, sub)
        }

        // 无克有遥 → 遥克
        val yao = yaoKe(lessons, dayStem)
        if (yao != null) {
            val (c1, sub) = yao
            val c2 = heavenAt(c1, offset)
            val c3 = heavenAt(c2, offset)
            return ThreeChuan(c1, c2, c3, ZongMen.YAO_KE, "遥克·$sub")
        }

        // 无克无遥：八专(干支同宫) / 昴星(四课全备) / 别责(三备)
        val jiGong = LiuRenTables.jiGong.getValue(dayStem)
        val distinctTops = lessons.map { it.top }.distinct().size
        val l1 = lessons[0]
        val l3 = lessons[2]
        return when {
            jiGong == dayBranch -> { // 八专
                val gs = l1.top
                val c1 = EarthlyBranch.fromIndex(floorMod(gs.ordinal + if (dayStem.yang) 3 else -3, 12))
                ThreeChuan(c1, gs, gs, ZongMen.BA_ZHUAN, "八专")
            }
            distinctTops == 4 -> { // 昴星
                if (dayStem.yang) {
                    val c1 = heavenAt(EarthlyBranch.YOU, offset) // 刚日仰视
                    ThreeChuan(c1, l3.top, l1.top, ZongMen.MAO_XING, "昴星·虎视转蓬")
                } else {
                    val c1 = EarthlyBranch.fromIndex(floorMod(EarthlyBranch.YOU.ordinal - offset, 12)) // 柔日俯视
                    ThreeChuan(c1, l1.top, l3.top, ZongMen.MAO_XING, "昴星·冬蛇掩目")
                }
            }
            else -> { // 别责
                val c1 = if (dayStem.yang) {
                    val heGan = ganHe.getValue(dayStem)
                    heavenAt(LiuRenTables.jiGong.getValue(heGan), offset)
                } else {
                    val sanHe = LiuRenTables.sanHeGroupOf(dayBranch)
                    sanHe[(sanHe.indexOf(dayBranch) + 1) % 3]
                }
                ThreeChuan(c1, l1.top, l1.top, ZongMen.BIE_ZE, "别责")
            }
        }
    }

    /** 重审/元首/比用/涉害 取初传，返回 (初传, 宗门, 课名) */
    private fun chuanFromKeZei(
        keZei: List<Lesson>,
        dayStem: HeavenlyStem,
        allLessons: List<Lesson>,
    ): Triple<EarthlyBranch, ZongMen, String> {
        if (keZei.size == 1) {
            val z = if (keZei[0].isShangKeXia) ZongMen.YUAN_SHOU else ZongMen.ZHONG_SHEN
            return Triple(keZei[0].top, z, z.chinese)
        }
        // 比用：与日干阴阳相比者
        val dayYang = dayStem.yang
        val bi = keZei.filter { (it.top.ordinal % 2 == 0) == dayYang }
        val (cands, viaBi) = if (bi.isNotEmpty() && bi.size < keZei.size) bi to true else keZei to false
        if (cands.size == 1) {
            val z = ZongMen.BI_YONG
            return Triple(cands[0].top, z, "比用")
        }
        // 涉害
        val (c1, sub) = sheHai(cands, dayStem, allLessons)
        return Triple(c1, ZongMen.SHE_HAI, "涉害·$sub")
    }

    /** 涉害深浅 + 孟仲季 决断，返回 (初传, 子格名) */
    private fun sheHai(cands: List<Lesson>, dayStem: HeavenlyStem, allLessons: List<Lesson>): Pair<EarthlyBranch, String> {
        val scored = cands.map { it to sheHaiDepth(it.top, it.bottomBranch) }
        val maxDepth = scored.maxOf { it.second }
        val deepest = scored.filter { it.second == maxDepth }
        if (deepest.size == 1) return deepest[0].first.top to "见机"
        // 孟仲季
        val meng = deepest.filter { it.first.top in LiuRenTables.siMeng }
        if (meng.size == 1) return meng[0].first.top to "见机"
        if (meng.size > 1) { // 都在四孟 → 缀瑕，取干上神
            return allLessons[0].top to "缀瑕"
        }
        val zhong = deepest.filter { it.first.top in LiuRenTables.siZhong }
        if (zhong.size == 1) return zhong[0].first.top to "察微"
        val ji = deepest.filter { it.first.top in LiuRenTables.siJi }
        if (ji.size == 1) return ji[0].first.top to "察微"
        return deepest[0].first.top to "见机"
    }

    /** 涉害深度：从下神位(含)顺时针到上神本家(不含)，累计"克上神"次数（地支五行+寄干） */
    private fun sheHaiDepth(shangShen: EarthlyBranch, xiaShen: EarthlyBranch): Int {
        var depth = 0
        var cur = xiaShen.ordinal
        val target = shangShen.ordinal
        while (cur != target) {
            val b = EarthlyBranch.fromIndex(cur)
            if (b.element.controls == shangShen.element) depth++
            LiuRenTables.ganOfBranch[b]?.forEach { g ->
                if (g.element.controls == shangShen.element) depth++
            }
            cur = floorMod(cur + 1, 12)
        }
        return depth
    }

    /** 遥克：先上神克日干(蒿矢)，无则日干克上神(弹射)。返回 (初传, 子格) 或 null */
    private fun yaoKe(lessons: List<Lesson>, dayStem: HeavenlyStem): Pair<EarthlyBranch, String>? {
        val tops = lessons.map { it.top }
        val keGan = tops.filter { it.element.controls == dayStem.element }
        if (keGan.isNotEmpty()) return pickByBi(keGan, dayStem) to "蒿矢"
        val ganKe = tops.filter { dayStem.element.controls == it.element }
        if (ganKe.isNotEmpty()) return pickByBi(ganKe, dayStem) to "弹射"
        return null
    }

    /** 按与日干阴阳相比筛选，仍多取首 */
    private fun pickByBi(branches: List<EarthlyBranch>, dayStem: HeavenlyStem): EarthlyBranch {
        val dayYang = dayStem.yang
        val bi = branches.filter { (it.ordinal % 2 == 0) == dayYang }
        return (bi.ifEmpty { branches }).first()
    }

    /** 十二天将落宫 */
    private fun placeTianJiang(dayStem: HeavenlyStem, offset: Int, isDay: Boolean): List<TianJiangOnPlate> {
        val (dayNoble, nightNoble) = LiuRenTables.guiRen.getValue(dayStem)
        val nobleTianpan = if (isDay) dayNoble else nightNoble
        val nobleDizhi = EarthlyBranch.fromIndex(floorMod(nobleTianpan.ordinal - offset, 12))
        val shun = nobleDizhi in shunSet
        val step = if (shun) 1 else -1
        val result = mutableListOf<TianJiangOnPlate>()
        var cur = nobleDizhi.ordinal
        TianJiang.entries.forEach { jiang ->
            val tianpan = EarthlyBranch.fromIndex(floorMod(cur + offset, 12))
            result.add(TianJiangOnPlate(jiang, tianpan, EarthlyBranch.fromIndex(cur)))
            cur = floorMod(cur + step, 12)
        }
        return result
    }

    private fun computeNianMing(gender: Gender?, age: Int?, benMing: EarthlyBranch?, yearBranch: EarthlyBranch): NianMing {
        val xingNian = if (gender != null && age != null && age > 0) {
            val base = if (gender == Gender.MALE) EarthlyBranch.YIN.ordinal else EarthlyBranch.SHEN.ordinal
            val ord = if (gender == Gender.MALE) floorMod(base + (age - 1), 12) else floorMod(base - (age - 1), 12)
            EarthlyBranch.fromIndex(ord)
        } else null
        // 本命支：用户显式给出为准；否则由虚岁粗推出生年支（yearBranch 仅供 API 对齐，此处不自动猜，
        // 避免虚岁/立春边界歧义——由 UI 让用户直接选本命支）
        @Suppress("UNUSED_PARAMETER")
        val bm = benMing
        return NianMing(xingNian, bm)
    }

    private fun fuYinSub(dayStem: HeavenlyStem): String = when (dayStem) {
        HeavenlyStem.JIA, HeavenlyStem.BING, HeavenlyStem.WU, HeavenlyStem.GENG -> "自任"
        HeavenlyStem.YI, HeavenlyStem.REN -> "杜传"
        HeavenlyStem.GUI -> "有克"
        else -> "自信"
    }

    /** 取某天将所临之天盘支（供 BiFa 用） */
    fun jiangAt(plate: LiuRenPlate, jiang: TianJiang): EarthlyBranch =
        plate.tianJiang.first { it.jiang == jiang }.heavenBranch
}

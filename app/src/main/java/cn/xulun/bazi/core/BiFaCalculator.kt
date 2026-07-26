package cn.xulun.bazi.core

import cn.xulun.bazi.model.BiFaHit
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.LiuRenPlate
import cn.xulun.bazi.model.TianJiang
import java.lang.Math.floorMod

/**
 * 毕法赋百法判定（《六壬大全》卷十一/十二，凌福之原本）。
 * 数据驱动：100 条 [Fa]，每条 [judge] 返回命中依据串或 null。
 * 高频法判定精确；冷门/盘例专指法取通行版，basis 注「约/通行版」。
 */
object BiFaCalculator {

    private data class Fa(
        val no: Int,
        val name: String,
        val auspicious: Boolean,
        val duanYu: String,
        val note: Boolean = false,
        val judge: (BiFaInput) -> String?, // 命中依据 or null
    )

    fun analyze(plate: LiuRenPlate): List<BiFaHit> {
        val input = BiFaInput(plate)
        return rules.mapNotNull { r ->
            r.judge(input)?.let { BiFaHit(r.no, r.name, r.auspicious, it, r.duanYu, r.note) }
        }
    }

    // ── 工具 ──
    private fun prev(b: EarthlyBranch) = EarthlyBranch.fromIndex(floorMod(b.ordinal - 1, 12))
    private fun next(b: EarthlyBranch) = EarthlyBranch.fromIndex(floorMod(b.ordinal + 1, 12))
    private fun neighbors(c: EarthlyBranch, a: EarthlyBranch, b: EarthlyBranch) = setOf(a, b) == setOf(prev(c), next(c))
    private fun even(b: EarthlyBranch) = b.ordinal % 2 == 0
    private val yangGan = setOf(HeavenlyStem.JIA, HeavenlyStem.BING, HeavenlyStem.WU, HeavenlyStem.GENG, HeavenlyStem.REN)

    private fun seasonEl(i: BiFaInput): Element = when (i.season) {
        "春" -> Element.WOOD; "夏" -> Element.FIRE; "秋" -> Element.METAL; else -> Element.WATER
    }

    /** 天将五行（贵人/勾陈/天空/太常寄土，余按本气） */
    private fun jiangEl(j: TianJiang): Element = when (j) {
        TianJiang.TENG_SHE, TianJiang.ZHU_QUE -> Element.FIRE
        TianJiang.LIU_HE, TianJiang.QING_LONG -> Element.WOOD
        TianJiang.BAI_HU, TianJiang.TAI_YIN -> Element.METAL
        TianJiang.XUAN_WU, TianJiang.TIAN_HOU -> Element.WATER
        else -> Element.EARTH
    }

    private val rules: List<Fa> = listOf(
        // 1 前后引从
        Fa(1, "前后引从升迁吉", true, "进职迁宅，得人提携；动则有庆") { i ->
            when {
                neighbors(i.ganJi, i.c1, i.c3) -> "初末传${i.c1.chinese}${i.c3.chinese}引从日干寄宫${i.ganJi.chinese}"
                neighbors(i.dayBranch, i.c1, i.c3) -> "初末传引从日支${i.dayBranch.chinese}"
                else -> null
            }
        },
        // 2 首尾相见
        Fa(2, "首尾相见始终宜", true, "所谋皆成，行人复来；不宜占释散") { i ->
            val a = i.ganShang == i.xunWei && i.zhiShang == i.xunShou
            val b = i.ganShang == i.xunShou && i.zhiShang == i.xunWei
            if (a || b) "干上${i.ganShang.chinese}(旬${if (a) "尾" else "首"})、支上${i.zhiShang.chinese}(旬${if (a) "首" else "尾"})" else null
        },
        // 3 帘幕贵人
        Fa(3, "帘幕贵人高甲第", true, "科场高中；有官人占反主休官") { i ->
            val (dayN, nightN) = LiuRenTables.guiRen.getValue(i.dayStem)
            val linMu = if (i.isDay) nightN else dayN
            val onYear = i.xingNianShang()?.let { i.rides(it, TianJiang.GUI_REN) } == true
            if ((i.ganShang == linMu && i.rides(i.ganShang, TianJiang.GUI_REN)) || onYear)
                "${if (i.isDay) "夜" else "昼"}贵(帘幕)临${if (onYear) "年命" else "干上"}" else null
        },
        // 4 催官使者
        Fa(4, "催官使者赴官期", true, "赴任极速；空亡则虚信") { i ->
            if (i.isGui(i.ganShang) && i.rides(i.ganShang, TianJiang.BAI_HU))
                "日鬼${i.ganShang.chinese}乘白虎临干" else null
        },
        // 5 六阳
        Fa(5, "六阳数足须公用", true, "利公不利私，明白事理") { i ->
            val ok = i.dayStem in yangGan && even(i.dayBranch) && even(i.ganShang) && even(i.zhiShang) &&
                i.chuanAll(::even)
            if (ok) "干支干上支上初中末俱居阳位" else null
        },
        // 6 六阴
        Fa(6, "六阴相继尽昏迷", false, "利阴私，尽昏迷，占病死") { i ->
            val ok = i.dayStem !in yangGan && !even(i.dayBranch) && !even(i.ganShang) && !even(i.zhiShang) &&
                i.chuanAll { !even(it) }
            if (ok) "干支干上支上初中末俱居阴位" else null
        },
        // 7 旺禄临身
        Fa(7, "旺禄临身徒妄作", true, "宜守不宜动，舍此反空") { i ->
            if (i.ganShang == i.lu) "干上神${i.ganShang.chinese}为日禄" else null
        },
        // 8 权摄不正
        Fa(8, "权摄不正禄临支", false, "不自尊大，权摄不正") { i ->
            if (i.zhiShang == i.lu) "日禄${i.lu.chinese}加临支上" else null
        },
        // 9 避难逃生
        Fa(9, "避难逃生须弃旧", true, "弃旧就新，避祸就福") { i ->
            val bad = (i.chuanAll { i.isCai(it) } && i.isKong(i.c1)) || i.isGui(i.c2) || i.isTuo(i.c3)
            val good = i.isYin(i.ganShang) || i.isYin(i.zhiShang)
            if (bad && good) "三传无益(财空/中鬼/末脱)而干上支上有生日干之神" else null
        },
        // 10 朽木难雕
        Fa(10, "朽木难雕别作为", false, "改科易业，谋事不遂") { i ->
            if (i.c1 == EarthlyBranch.MAO && i.isKong(EarthlyBranch.MAO)) "初传卯且落旬空" else null
        },
        // 11 众鬼虽彰
        Fa(11, "众鬼虽彰全不畏", true, "先惊后无畏，谋害终不成祸") { i ->
            if (i.chuanAll { i.isGui(it) } && (i.isYin(i.ganShang) || i.ganShang.element.controls == i.c1.element))
                "三传皆日鬼而干上有制鬼之神" else null
        },
        // 12 狐假虎威
        Fa(12, "虽忧狐假虎威仪", false, "切不可动谋，离依靠即受祸") { i ->
            if (i.isGui(i.ganShang) && i.zhiShang.element.controls == i.ganShang.element)
                "干上神克干，支上神${i.zhiShang.chinese}制干上神" else null
        },
        // 13 鬼贼当时
        Fa(13, "鬼贼当时无畏忌", false, "暂不畏，过令祸发") { i ->
            if (i.chuanAll { i.isGui(it) } && i.c1.element == seasonEl(i)) "三传皆鬼且鬼当令(${i.season})" else null
        },
        // 14 传财太旺
        Fa(14, "传财太旺反财亏", false, "求财反费，待身旺月方取") { i ->
            if (i.chuanAll { i.isCai(it) } && i.c1.element == seasonEl(i)) "三传皆财且财当令(${i.season})" else null
        },
        // 15 脱上逢脱
        Fa(15, "脱上逢脱防虚诈", false, "尽脱耗，多虚诈") { i ->
            val j = i.jiangOn(i.ganShang)
            if (i.isTuo(i.ganShang) && j != null && i.ganShang.element.generates == jiangEl(j))
                "干生干上${i.ganShang.chinese}，干上又生所乘${j.chinese}将" else null
        },
        // 16 空上乘空
        Fa(16, "空上乘空事莫追", false, "指空话空，全无实象") { i ->
            if (i.isKong(i.ganShang) && i.rides(i.ganShang, TianJiang.TIAN_KONG)) "干上神旬空且乘天空" else null
        },
        // 17 进茹空亡
        Fa(17, "进茹空亡宜退步", false, "宜退不宜进") { i ->
            val forward = next(i.c1) == i.c2 && next(i.c2) == i.c3
            if (forward && i.chuanAll { i.isKong(it) }) "三传前进连茹且全空" else null
        },
        // 18 踏脚空亡
        Fa(18, "踏脚空亡进用宜", true, "宜进不宜退") { i ->
            val back = prev(i.c1) == i.c2 && prev(i.c2) == i.c3
            if (back && i.chuanAll { i.isKong(it) }) "三传退步连茹且全空" else null
        },
        // 19 胎财生气
        Fa(19, "胎财生气妻怀孕", true, "占妻必孕") { i ->
            if (i.ganShang == i.taiShen && i.isCai(i.ganShang) && i.ganShang == i.shengQi)
                "干上${i.ganShang.chinese}为胎财又作月生气" else null
        },
        // 20 胎财死气
        Fa(20, "胎财死气损胎推", false, "占孕必损") { i ->
            if (i.ganShang == i.taiShen && i.isCai(i.ganShang) && i.ganShang == i.siQi)
                "干上${i.ganShang.chinese}为胎财又作月死气" else null
        },
        // 21 交车相合
        Fa(21, "交车相合交关利", true, "交关交易皆成") { i ->
            if (i.liuHe(i.ganShang, i.dayBranch) && i.liuHe(i.zhiShang, i.ganJi))
                "干上${i.ganShang.chinese}合支、支上${i.zhiShang.chinese}合干" else null
        },
        // 22 上下皆合
        Fa(22, "上下皆合两心齐", true, "主客相顺，神合道合") { i ->
            if (i.liuHe(i.ganShang, i.zhiShang) && i.liuHe(i.dayBranch, i.ganJi))
                "干支上神六合、地盘干支六合" else null
        },
        // 23 支传干
        Fa(23, "彼求我事支传干", true, "他人托我，行人至求取得") { i ->
            if (i.c3 == i.ganShang && i.c1 != i.ganShang) "末传${i.c3.chinese}归干上" else null
        },
        // 24 干传支
        Fa(24, "我求彼事干传支", false, "勉强俯求，百事不举") { i ->
            if (i.c3 == i.zhiShang && i.c1 != i.zhiShang) "末传${i.c3.chinese}归支上" else null
        },
        // 25 金日逢丁
        Fa(25, "金日逢丁凶祸动", false, "必主凶动，有官人赴任速") { i ->
            val dingIn = listOf(i.c1, i.c2, i.c3, i.ganShang, i.zhiShang).any { it == i.ding }
            if ((i.dayStem == HeavenlyStem.GENG || i.dayStem == HeavenlyStem.XIN) && dingIn)
                "${i.dayStem.chinese}日六处逢旬丁${i.ding.chinese}" else null
        },
        // 26 水日逢丁
        Fa(26, "水日逢丁财动之", true, "远方财物至") { i ->
            val dingIn = listOf(i.c1, i.c2, i.c3, i.ganShang, i.zhiShang).any { it == i.ding }
            if ((i.dayStem == HeavenlyStem.REN || i.dayStem == HeavenlyStem.GUI) && dingIn)
                "${i.dayStem.chinese}日六处逢旬丁${i.ding.chinese}" else null
        },
        // 27 传财化鬼
        Fa(27, "传财化鬼财休觅", false, "取财致祸，妻与鬼交") { i ->
            if (i.chuanAll { i.isCai(it) } && i.isGui(i.ganShang)) "三传皆财而生干上日鬼${i.ganShang.chinese}" else null
        },
        // 28 传鬼化财
        Fa(28, "传鬼化财钱险危", true, "危险中得财，不安稳") { i ->
            val kongN = listOf(i.c1, i.c2, i.c3).count { i.isKong(it) }
            if (i.chuanAll { i.isGui(it) } && kongN >= 2 && i.chuanAll { !i.isKong(it) || i.isCai(it) } && listOf(i.c1, i.c2, i.c3).any { !i.isKong(it) && i.isCai(it) })
                "三传皆鬼而两空独存一财" else null
        },
        // 29 眷属丰盈
        Fa(29, "眷属丰盈居狭宅", true, "人口旺宅窄，不可迁宽屋") { i ->
            if (i.chuanAll { i.isYin(it) }) "三传${i.c1.chinese}${i.c2.chinese}${i.c3.chinese}生干" else null
        },
        // 30 屋宅宽广
        Fa(30, "屋宅宽广致人衰", false, "宅广人衰，宜迁居") { i ->
            if (i.chuanAll { i.isTuo(it) }) "三传${i.c1.chinese}${i.c2.chinese}${i.c3.chinese}脱干" else null
        },
        // 31 三传递生
        Fa(31, "三传递生人举荐", true, "隔三隔四有人举荐") { i ->
            val fwd = i.c1.element.generates == i.c2.element && i.c2.element.generates == i.c3.element && i.c3.element.generates == i.dayStem.element
            val bwd = i.c3.element.generates == i.c2.element && i.c2.element.generates == i.c1.element && i.c1.element.generates == i.dayStem.element
            if (fwd || bwd) "初中末递相生日干" else null
        },
        // 32 三传互克
        Fa(32, "三传互克众人欺", false, "递互克害，众口欺凌") { i ->
            val fwd = i.c1.element.controls == i.c2.element && i.c2.element.controls == i.c3.element && i.c3.element.controls == i.dayStem.element
            val bwd = i.c3.element.controls == i.c2.element && i.c2.element.controls == i.c1.element && i.c1.element.controls == i.dayStem.element
            if (fwd || bwd) "初中末递相克日干" else null
        },
        // 33 有始无终 / 难变易
        Fa(33, "有始无终难变易", true, "先吉后凶 / 先难后易") { i ->
            if (i.c1 == i.cSheng && i.c3 == i.mu) "初传长生${i.c1.chinese}、末传墓${i.c3.chinese}"
            else if (i.c1 == i.mu && i.c3 == i.cSheng) "初传墓、末传长生(难变易)" else null
        },
        // 34 苦去甘来
        Fa(34, "苦去甘来乐里悲", true, "先磨折后安逸") { i ->
            if (i.isGui(i.c1) && i.c3.element.controls == i.c1.element) "末传${i.c3.chinese}制初传之鬼" else null
        },
        // 35 人宅受脱
        Fa(35, "人宅受脱俱招盗", false, "人被脱赚，家宅被盗") { i ->
            if (i.isTuo(i.ganShang) && i.isTuo(i.zhiShang)) "干上${i.ganShang.chinese}脱干、支上${i.zhiShang.chinese}脱(约)" else null
        },
        // 36 干支皆败
        Fa(36, "干支皆败事倾颓", false, "身宅皆衰败") { i ->
            if (i.ganShang == next(i.cSheng)) "干上神为干沐浴败气(约)" else null
        },
        // 37 末助初
        Fa(37, "末助初兮三等论", true, "傍人相助，或教唆暗助") { i ->
            if (i.c3.element.generates == i.c1.element) "末传${i.c3.chinese}生助初传${i.c1.chinese}" else null
        },
        // 38 闭口
        Fa(38, "闭口卦体两般推", false, "捕盗追逃；占病哑、事闭口") { i ->
            when {
                i.lu == i.xunWei -> "日禄${i.lu.chinese}作旬尾(禄作闭口)"
                i.c1 == i.xunWei -> "初传为旬尾${i.xunWei.chinese}"
                i.rides(i.xunShou.let { i.p.heavenAt(it) }, TianJiang.XUAN_WU) -> "旬首乘玄武"
                else -> null
            }
        },
        // 39 太阳照武
        Fa(39, "太阳照武宜擒贼", true, "贼必败露") { i ->
            if (i.rides(i.yueJiang, TianJiang.XUAN_WU)) "玄武乘月将(太阳)${i.yueJiang.chinese}" else null
        },
        // 40 后合占婚
        Fa(40, "后合占婚岂用媒", false, "先奸后娶，私情交通") { i ->
            val g = i.rides(i.ganShang, TianJiang.TIAN_HOU) || i.rides(i.ganShang, TianJiang.LIU_HE)
            val z = i.rides(i.zhiShang, TianJiang.LIU_HE) || i.rides(i.zhiShang, TianJiang.TIAN_HOU)
            if (g && z) "干上乘天后/六合、支上乘六合/天后" else null
        },
        // 41 富贵禄马
        Fa(41, "富贵干支逢禄马", true, "君子加官添俸；常人病讼凶") { i ->
            if (i.ganShang == i.yiMa && i.zhiShang == i.lu)
                "干上${i.ganShang.chinese}为支驿马、支上${i.zhiShang.chinese}为干禄" else null
        },
        // 42 传内三奇
        Fa(42, "尊崇传内遇三奇", true, "官居一品，常人消灾") { i ->
            val dg = listOf(i.dunGan(i.c1), i.dunGan(i.c2), i.dunGan(i.c3)).filterNotNull().toSet()
            when {
                dg == setOf(HeavenlyStem.YI, HeavenlyStem.BING, HeavenlyStem.DING) -> "三传遁干为乙丙丁(三奇)"
                dg == setOf(HeavenlyStem.JIA, HeavenlyStem.GENG, HeavenlyStem.WU) -> "三传遁干为甲戊庚"
                else -> null
            }
        },
        // 43 害贵讼直
        Fa(43, "害贵讼直作屈断", false, "讼理直而曲断，弄巧成拙") { i ->
            if (i.rides(i.ganShang, TianJiang.GUI_REN) && i.liuHai(i.ganShang, i.ganJi))
                "贵人临干而干上与干寄宫相害(约)" else null
        },
        // 44 课传俱贵
        Fa(44, "课传俱贵转无依", false, "贵多不贵，无依倚") { i ->
            val guiCount = listOf(i.ganShang, i.zhiShang, i.c1, i.c2, i.c3).count { i.rides(it, TianJiang.GUI_REN) }
            if (guiCount >= 3) "六处贵人齐聚${guiCount}处" else null
        },
        // 45 昼夜贵加
        Fa(45, "昼夜贵加求两贵", true, "涉两贵成事；占谒贵反不见") { i ->
            val (dayN, nightN) = LiuRenTables.guiRen.getValue(i.dayStem)
            if (i.ganShang == dayN && i.zhiShang == nightN || i.ganShang == nightN && i.zhiShang == dayN)
                "昼夜贵人相加于干支上" else null
        },
        // 46 贵人差迭(蹉跎)
        Fa(46, "贵人差迭事参差", false, "告贵不归一，尖担两头脱") { i ->
            val (dayN, nightN) = LiuRenTables.guiRen.getValue(i.dayStem)
            if (i.ganShang == nightN && i.zhiShang == dayN)
                "昼贵临夜方、夜贵临昼方(蹉跎)" else null
        },
        // 47 贵虽在狱
        Fa(47, "贵虽在狱宜临干", true, "乙辛反宜告贵；余干干贵怒") { i ->
            val onChenXu = i.ganShang == EarthlyBranch.CHEN || i.ganShang == EarthlyBranch.XU
            if (i.rides(i.ganShang, TianJiang.GUI_REN) && onChenXu) "贵人加辰戌(入狱)临干" else null
        },
        // 48 鬼乘天乙
        Fa(48, "鬼乘天乙乃神祗", false, "占病神祗为害，宜修设功德") { i ->
            if (i.isGui(i.ganShang) && i.rides(i.ganShang, TianJiang.GUI_REN)) "日鬼${i.ganShang.chinese}乘贵人临干" else null
        },
        // 49 两贵受克
        Fa(49, "两贵受克难干贵", false, "不可告贵，二贵自顾不暇") { i ->
            val (dayN, nightN) = LiuRenTables.guiRen.getValue(i.dayStem)
            if (i.dayStem.element.controls == dayN.element && i.dayStem.element.controls == nightN.element)
                "日干克昼夜贵${dayN.chinese}${nightN.chinese}(两贵受克,约)" else null
        },
        // 50 二贵皆空
        Fa(50, "二贵皆空虚喜期", false, "虚喜，事允被人搀越") { i ->
            val (dayN, nightN) = LiuRenTables.guiRen.getValue(i.dayStem)
            if (i.isKong(dayN) && i.isKong(nightN)) "昼夜贵${dayN.chinese}${nightN.chinese}皆落空亡" else null
        },
        // 51 魁度天门
        Fa(51, "魁度天门关隔定", false, "谋用皆阻隔，访人不见") { i ->
            if (i.c1 == EarthlyBranch.XU && i.p.heavenAt(EarthlyBranch.XU) == EarthlyBranch.HAI)
                "初传戌加亥(魁度天门)" else null
        },
        // 52 罡塞鬼户
        Fa(52, "罡塞鬼户任谋为", true, "万鬼潜形，谋为亨利") { i ->
            if (i.p.heavenAt(EarthlyBranch.YIN) == EarthlyBranch.CHEN)
                "辰(天罡)加寅(鬼户)" else null
        },
        // 53 两蛇夹墓
        Fa(53, "两蛇夹墓凶难免", false, "占病腹中积块不救") { i ->
            if (i.ganShang == i.mu && i.rides(i.ganShang, TianJiang.TENG_SHE) && i.rides(i.zhiShang, TianJiang.TENG_SHE))
                "干墓${i.mu.chinese}临干，昼夜乘蛇(约两蛇夹墓)" else null
        },
        // 54 虎视逢虎
        Fa(54, "虎视逢虎力难施", false, "惊天动地，凶祸难免") { i ->
            if (i.p.threeChuan.zongMen == cn.xulun.bazi.model.ZongMen.MAO_XING && i.rides(i.c1, TianJiang.BAI_HU))
                "昴星(虎视)课初传乘白虎" else null
        },
        // 55 所谋逢罗网
        Fa(55, "所谋多拙逢罗网", false, "动则网罗缠身") { i ->
            if (i.ganShang == next(i.ganJi) && i.zhiShang == next(i.dayBranch))
                "干上为干前一辰、支上为支前一辰(天罗地网)" else null
        },
        // 56 天网自裹
        Fa(56, "天网自裹己招非", false, "自招其祸，昏晦如雾") { i ->
            if (i.ganShang == i.mu && i.benMing == i.mu) "干墓${i.mu.chinese}覆日且为本命" else null
        },
        // 57 费余得不足
        Fa(57, "费有余而得不足", false, "所得不偿所费") { i ->
            if (i.isKong(i.ganShang) && i.isKong(i.zhiShang) && i.isGui(i.ganYin))
                "干支相生皆空而二四课反见鬼贼(约)" else null
        },
        // 58 用破身心
        Fa(58, "用破身心无所归", false, "亦无欢笑亦无愁") { i ->
            if (i.isCai(i.c1) && i.c1.element.controls == i.p.heavenAt(i.c1).element && listOf(i.c2, i.c3).any { i.isGui(it) })
                "初传财坐克方乘凶将、中末入鬼乡(约)" else null
        },
        // 59 华盖覆日
        Fa(59, "华盖覆日人昏晦", false, "多昏多晦，行人不归") { i ->
            if (i.c1 == i.huaGai && i.c1 == i.mu) "初传华盖${i.huaGai.chinese}作日墓临干发用" else null
        },
        // 60 太阳射宅
        Fa(60, "太阳射宅屋光辉", true, "屋向阳，上人光饰") { i ->
            if (i.zhiShang == i.yueJiang) "支上神为月将(太阳)" else null
        },
        // 61 干乘墓虎
        Fa(61, "干乘墓虎无占病", false, "占病必死，诸事昏迷") { i ->
            if (i.ganShang == i.mu && i.rides(i.ganShang, TianJiang.BAI_HU)) "日墓${i.mu.chinese}乘白虎临干" else null
        },
        // 62 支乘墓虎
        Fa(62, "支乘墓虎有伏尸", false, "宅有伏尸，孝服动") { i ->
            if (i.zhiShang == i.mu && i.rides(i.zhiShang, TianJiang.BAI_HU)) "墓乘白虎临支(约)" else null
        },
        // 63 彼此全伤
        Fa(63, "彼此全伤防两损", false, "讼两家皆扑责") { i ->
            if (i.isGui(i.ganShang) && i.zhiShang.element.controls == i.dayBranch.element)
                "干上克干、支上克支(约)" else null
        },
        // 64 夫妇芜淫
        Fa(64, "夫妇芜淫各有私", false, "夫妇各有私情，奸私不协") { i ->
            val wu = i.isGui(i.ganShang) && i.zhiShang.element.controls == i.ganJi.element // 约
            if (wu) "干被支上克、支被干上克(芜淫/解离约)" else null
        },
        // 65 干墓并关
        Fa(65, "干墓并关人宅废", false, "人口灾衰 / 宅颓废") { i ->
            if (i.c1 == i.mu && i.c1 == i.guanShen) "日墓${i.mu.chinese}作${i.season}季关神发用" else null
        },
        // 66 支坟财并旅
        Fa(66, "支坟财并旅程稽", false, "商贩折本，在路阻程") { i ->
            val zhiMu = LiuRenTables.mu[i.dayStem] // 约用日墓
            if (i.c1 == zhiMu && i.isCai(i.c1)) "墓作日财发用(约)" else null
        },
        // 67 受虎克神(病症)
        Fa(67, "受虎克神为病症", false, "占病定位极验") { i ->
            val t = i.branchOf(TianJiang.BAI_HU)
            if (t in listOf(i.ganShang, i.zhiShang, i.c1, i.c2, i.c3)) {
                val organ = when (t.element) {
                    Element.METAL -> "肝"; Element.WOOD -> "脾"; Element.WATER -> "心"; Element.FIRE -> "肺"; Element.EARTH -> "肾"
                }
                "白虎乘${t.chinese}(${t.element.chinese})临干支传，主${organ}疾"
            } else null
        },
        // 68 制鬼良医
        Fa(68, "制鬼之位乃良医", true, "制鬼神临年命宜雪理") { i ->
            val zhiGui = listOf(i.c1, i.c2, i.c3).firstOrNull { i.isGui(it) }
            if (zhiGui != null) {
                val zhi = EarthlyBranch.entries.firstOrNull {
                    it.element.controls == zhiGui.element &&
                        (it == i.ganShang || it == i.zhiShang || it == i.xingNianShang() || it == i.benMingShang())
                }
                if (zhi != null) "制鬼之神${zhi.chinese}临干支年命(良医)" else null
            } else null
        },
        // 69 虎乘遁鬼
        Fa(69, "虎乘遁鬼殃非浅", false, "至凶至危，纵空不能救") { i ->
            val t = i.branchOf(TianJiang.BAI_HU)
            val dg = i.dunGan(t)
            if (dg != null && dg.element.controls == i.dayStem.element) "白虎所乘${t.chinese}遁${dg.chinese}克日干" else null
        },
        // 70 鬼临三四
        Fa(70, "鬼临三四讼灾随", false, "官词病患继踵而至") { i ->
            if (i.isGui(i.zhiShang) && i.isGui(i.zhiYin)) "日鬼临第三课${i.zhiShang.chinese}与第四课${i.zhiYin.chinese}" else null
        },
        // 71 病符克宅
        Fa(71, "病符克宅全家患", false, "阖家病患，遭时疫") { i ->
            if (i.zhiShang == i.bingFu && i.zhiShang.element.controls == i.dayBranch.element)
                "病符(旧太岁)${i.bingFu.chinese}临支克支" else null
        },
        // 72 丧吊全逢
        Fa(72, "丧吊全逢挂缟衣", false, "必哭送亲姻，身披孝服") { i ->
            val onPlate = listOf(i.ganShang, i.zhiShang, i.c1, i.c2, i.c3)
            if (i.sangMen in onPlate && i.diaoKe in onPlate) "丧门${i.sangMen.chinese}、吊客${i.diaoKe.chinese}全逢干支" else null
        },
        // 73 前后逼迫
        Fa(73, "前后逼迫难进退", false, "惟宜守干上旺，不可妄动") { i ->
            val fwdKong = i.isKong(i.c2) || i.isKong(i.c3)
            val backGui = i.isGui(prev(i.c1)) || i.isTuo(prev(i.c1))
            if (fwdKong && backGui) "前进值空、后退值鬼脱(约)" else null
        },
        // 74 空空如也
        Fa(74, "空空如也事休追", false, "指空话空，惟宜解散忧疑") { i ->
            if (i.chuanAll { i.isKong(it) }) "三传皆空亡" else null
        },
        // 75 宾主不投刑在上
        Fa(75, "宾主不投刑在上", false, "交涉各有异心，占讼被刑责") { i ->
            val onTop = listOf(i.ganShang, i.zhiShang)
            val yiZi = onTop.all { it in LiuRenTables.ziXing } // 一字刑：干支上全自刑
            val ziMao = onTop.contains(EarthlyBranch.ZI) && onTop.contains(EarthlyBranch.MAO) // 二字刑
            val sanXing = (setOf(i.c1, i.c2, i.c3) == setOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN)) ||
                (setOf(i.c1, i.c2, i.c3) == setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI)) // 三字刑
            if (yiZi || ziMao || sanXing) "干支上乘刑(一字/二字/三字刑)" else null
        },
        // 76 彼此猜忌害相随
        Fa(76, "彼此猜忌害相随", false, "主客不相顾接，各相谋害") { i ->
            if (i.liuHai(i.ganShang, i.zhiShang)) "干支上神${i.ganShang.chinese}${i.zhiShang.chinese}相害" else null
        },
        // 77 互生俱生
        Fa(77, "互生俱生凡事益", true, "彼此有益，合本营生") { i ->
            val hu = i.zhiShang.element.generates == i.dayStem.element && i.ganShang.element.generates == i.dayBranch.element
            val ju = i.isYin(i.ganShang) && i.zhiShang.element.generates == i.dayBranch.element
            if (hu || ju) "干支互生或俱生" else null
        },
        // 78 互旺皆旺
        Fa(78, "互旺皆旺坐谋宜", true, "惟宜坐待，不利谋动") { i ->
            if (i.ganShang.element == i.dayBranch.element && i.zhiShang.element == i.dayStem.element)
                "干上为支旺神、支上为干旺神(互旺)" else null
        },
        // 79 干支值绝
        Fa(79, "干支值绝凡谋决", true, "惟宜结绝凶事，释解官讼") { i ->
            if (i.ganShang.element.controls == i.dayBranch.element && i.zhiShang.element.controls == i.dayStem.element)
                "干支上皆乘绝神(约)" else null
        },
        // 80 人宅皆死
        Fa(80, "人宅皆死各衰羸", false, "惟宜休息，占病必死") { i ->
            if (i.ganShang == i.mu && i.zhiShang == i.mu) "干上支上皆为墓(死气约)" else null
        },
        // 81 传墓入墓
        Fa(81, "传墓入墓分憎爱", false, "吉神入墓凶，凶神入墓喜") { i ->
            val chuzhongMu = (i.c2 == i.mu || i.c3 == i.mu)
            if (chuzhongMu && !i.isGui(i.c1)) "初传${i.c1.chinese}、中末传日墓${i.mu.chinese}" else null
        },
        // 82 不行传者考初时
        Fa(82, "不行传者考初时", false, "中末空则只看初传") { i ->
            if (i.isKong(i.c2) && i.isKong(i.c3)) "中传末传皆空，只凭初传" else null
        },
        // 83 万事喜忻三六合
        Fa(83, "万事喜忻三六合", true, "三六相呼，凡谋皆遂") { i ->
            val ju = sanHeMatch(setOf(i.c1, i.c2, i.c3))
            val he = i.liuHe(i.c2, i.p.heavenAt(i.c2))
            if (ju && he) "三传合局且中传六合" else null
        },
        // 84 合中犯杀
        Fa(84, "合中犯杀蜜中砒", false, "恩中变怨，笑里刀蜜里砒") { i ->
            val ju = sanHeMatch(setOf(i.c1, i.c2, i.c3))
            val sha = listOf(i.c1, i.c2, i.c3).any { it in LiuRenTables.ziXing } ||
                listOf(i.c1, i.c2, i.c3).any { b -> listOf(i.ganShang, i.zhiShang).any { i.chong(b, it) || i.liuHai(b, it) } }
            if (ju && sha) "三传合局而干支上见煞(自刑/冲/害)" else null
        },
        // 85 初遭夹克
        Fa(85, "初遭夹克不由己", false, "身不由己；鬼受夹克反吉") { i ->
            val j = i.jiangOn(i.c1)
            if (j != null && jiangEl(j).controls == i.c1.element) "初传${i.c1.chinese}所乘${j.chinese}将克之(夹克约)" else null
        },
        // 86 将逢内战
        Fa(86, "将逢内战所谋危", false, "用事将成人搅扰") { i ->
            val zhan = i.tianJiangList().any { (p, j) -> p.element.controls == jiangEl(j) }
            if (zhan) "有天将所临地盘神克天将本身(内战)" else null
        },
        // 87 人宅坐墓
        Fa(87, "人宅坐墓甘招晦", false, "自招其祸，情情愿愿受暗昧") { i ->
            if (i.earthOf(i.ganShang) == i.mu || i.earthOf(i.zhiShang) == i.mu)
                "天盘干支坐地盘日墓上(约)" else null
        },
        // 88 干支乘墓
        Fa(88, "干支乘墓各昏迷", false, "所为皆不通") { i ->
            if (i.ganShang == i.mu && i.zhiShang == i.mu) "干上支上皆为日墓" else null
        },
        // 89 任信丁马
        Fa(89, "任信丁马须言动", true, "静中求动，访人必出干事") { i ->
            val fuYin = i.p.threeChuan.zongMen == cn.xulun.bazi.model.ZongMen.FU_YIN
            val dingMa = listOf(i.c1, i.c2, i.c3, i.ganShang, i.zhiShang).any { it == i.ding || it == i.tianMa || it == i.yiMa }
            if (dingMa) "六处见旬丁/天马/驿马${if (fuYin) "(伏吟任信)" else ""}" else null
        },
        // 90 来去俱空
        Fa(90, "来去俱空岂动宜", false, "虽有动意实不动") { i ->
            val fan = i.p.threeChuan.zongMen == cn.xulun.bazi.model.ZongMen.FAN_YIN
            if (fan && i.chuanAll { i.isKong(it) }) "返吟(来去)而三传皆空" else null
        },
        // 91 虎临干鬼
        Fa(91, "虎临干鬼凶速速", false, "凶祸速中又速；占病必死") { i ->
            if (i.isGui(i.ganShang) && i.rides(i.ganShang, TianJiang.BAI_HU)) "日鬼${i.ganShang.chinese}乘白虎临干" else null
        },
        // 92 龙加生气
        Fa(92, "龙加生气吉迟迟", true, "徐徐发福，缓而作吉") { i ->
            val j = i.jiangOn(i.ganShang)
            if (j == TianJiang.QING_LONG && i.ganShang.element.generates == i.dayStem.element && i.ganShang == i.shengQi)
                "青龙乘生干之神${i.ganShang.chinese}作月生气" else null
        },
        // 93 妄用三传(警示)
        Fa(93, "妄用三传灾福异", false, "三传起例须正，错则灾福断错", note = true) { i ->
            "占断须以正确九宗门起三传（涉害/返吟/别责/八专等）；本课为${i.p.threeChuan.keMing}"
        },
        // 94 喜惧空亡
        Fa(94, "喜惧空亡乃妙机", false, "鬼空宜制，生空反凶") { i ->
            val guiKong = listOf(i.c1, i.c2, i.c3).any { i.isGui(it) && i.isKong(it) }
            val yinKong = listOf(i.ganShang, i.zhiShang).any { i.isYin(it) && i.isKong(it) }
            if (guiKong || yinKong) "鬼空宜制(吉)或生空(凶)，游行空=七八分力" else null
        },
        // 95 六爻现卦
        Fa(95, "六爻现卦防其克", false, "逐类现卦防该类神灾") { i ->
            when {
                i.chuanAll { i.isCai(it) } -> "财现卦，忧父母"
                i.chuanAll { i.isYin(it) } -> "父母现卦，忧子息"
                i.chuanAll { i.isTuo(it) } -> "子息现卦，忧官"
                i.chuanAll { i.isGui(it) } -> "官鬼现卦，忧己身兄弟"
                else -> null
            }
        },
        // 96 旬内空亡
        Fa(96, "旬内空亡逐类推", false, "财空乏储，鬼空敌遁，救空策拙") { i ->
            val k = i.kong
            if (k.isNotEmpty()) "本旬空${k.joinToString("") { it.chinese }}，逐类推之" else null
        },
        // 97 所筮不入凭类(警示)
        Fa(97, "所筮不入仍凭类", false, "类神不在六处，仍以类神推方所色目", note = true) { i ->
            "所占类神(玄武/财/官等)若不入课传，仍凭类神推之"
        },
        // 98 非占现类勿言(警示)
        Fa(98, "非占现类勿言之", false, "占什么说什么，勿言非所占之类", note = true) { i ->
            "占此言彼为忌，须就所占之类断之"
        },
        // 99 常问不应逢吉象
        Fa(99, "常问不应逢吉象", false, "常人恐致灾咎；占病见阴司", note = true) { i ->
            if (i.p.threeChuan.zongMen == cn.xulun.bazi.model.ZongMen.FU_YIN ||
                i.p.threeChuan.zongMen == cn.xulun.bazi.model.ZongMen.FAN_YIN)
                "逢伏吟/返吟大象，常人占反主灾咎(约)" else null
        },
        // 100 已灾凶逃返无疑
        Fa(100, "已灾凶逃返无疑", false, "已灾后再占反消；未灾前占丧祸并至", note = true) { i ->
            if (i.p.threeChuan.zongMen == cn.xulun.bazi.model.ZongMen.FAN_YIN)
                "返吟大象：已灾后占反消，未灾前占丧祸并至" else null
        },
    )

    private fun sanHeMatch(set: Set<EarthlyBranch>): Boolean = set.size == 3 && listOf(
        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN),
        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI),
        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),
        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU),
    ).any { it == set }

    // BiFaInput 天将遍历辅助（地盘支, 天将）
    private fun BiFaInput.tianJiangList(): List<Pair<EarthlyBranch, TianJiang>> =
        p.tianJiang.map { it.earthBranch to it.jiang }
}

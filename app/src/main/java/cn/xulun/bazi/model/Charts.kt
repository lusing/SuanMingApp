package cn.xulun.bazi.model

import java.time.LocalDateTime

/** 日主强弱（打分法参考值，仅作辅助） */
enum class DayStrength(val chinese: String) {
    STRONG("身强"),
    BALANCED("中和"),
    WEAK("身弱"),
}

/** 流年（某一公历年的干支，按公历年份直接取） */
data class LiuNian(val year: Int, val pillar: Pillar) {
    /** 该流年天干相对日主的十神 */
    fun tenGodOf(dayMaster: HeavenlyStem): TenGod = tenGodOfStem(dayMaster, pillar.stem)
}

/** 一步大运（十年） */
data class DaYun(
    val pillar: Pillar,
    val startAge: Int,
    val startYear: Int,
    val liuNian: List<LiuNian>,
) {
    val endAge: Int get() = startAge + 9
    val endYear: Int get() = startYear + 9
}

/** 大运排盘结果 */
data class DaYunInfo(
    /** 顺行 / 逆行 */
    val forward: Boolean,
    /** 起运说明，如「出生后 8 年 4 个月 24 天起运」 */
    val qiYunText: String,
    /** 起运日期（交脱大运的公历日期，北京时间墙钟） */
    val startDate: LocalDateTime,
    /** 大运列表（默认 8 步） */
    val yuns: List<DaYun>,
)

/** 格局判定结果 */
data class GeJuResult(
    /** 格局名称，如「正官格」「建禄格」 */
    val name: String,
    /** 定格十神 */
    val tenGod: TenGod,
    /** 透出天干的月令藏干；未透干或建禄/阳刃时为 null */
    val touGanStem: HeavenlyStem?,
    /** 判定依据说明 */
    val basis: String,
    /** 日主强弱（参考） */
    val strength: DayStrength,
) {
    override fun toString(): String = "$name（${strength.chinese}）"
}

/** 已保存的八字命盘 */
data class SavedChart(
    val id: String,
    val name: String,
    val year: Int,
    val month: Int,
    val day: Int,
    /** 出生小时（时辰起始小时，23=晚子时） */
    val hour: Int,
    val gender: Gender,
    /** 保存时间（毫秒时间戳） */
    val savedAt: Long,
) {
    val birthDateTime: LocalDateTime get() = LocalDateTime.of(year, month, day, hour, 0)
    val birthText: String get() = "${year}年${month}月${day}日 ${hour}时"
}

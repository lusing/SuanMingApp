package cn.xulun.bazi.ui

import cn.xulun.bazi.model.Pillar

/** 大运 / 流年选择状态（UI 层）。同一时刻至多选中一项，再点当前项即取消。 */
sealed interface YunSelection {
    /** 选中的柱（用于并入原局做关系分析） */
    val pillar: Pillar
    /** 柱位标签，会出现在关系条目的 location 中 */
    val label: String

    data class DaYun(val yun: cn.xulun.bazi.model.DaYun) : YunSelection {
        override val pillar: Pillar get() = yun.pillar
        override val label: String = "大运"
    }

    data class LiuNian(
        val yun: cn.xulun.bazi.model.DaYun,
        val liuNian: cn.xulun.bazi.model.LiuNian,
    ) : YunSelection {
        override val pillar: Pillar get() = liuNian.pillar
        override val label: String = "流年"
    }
}

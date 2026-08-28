package cn.xulun.bazi.model

/** 交际能力推断结果（交际=语言/眼神/肢体表达——食伤为代表；水主智）。 */
data class JiaoJiResult(
    /** 交际天赋结论（好/一般/差——食伤强弱+是否被制住合住+水智） */
    val talent: String,
    /** 推断依据链 */
    val basis: String,
    /** 交际风格（伤官强——直接爽快 / 食神强——含蓄委婉） */
    val style: String,
    /** 食伤与水生克（水生食伤加强 / 相克打折） */
    val waterNote: String,
    /** 食伤大运转折（原局食伤弱而行食伤运——一改木讷封闭变得善交际） */
    val yunTurning: String,
    val summary: String,
)

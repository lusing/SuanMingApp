package cn.xulun.bazi.ui.theme

import androidx.compose.ui.graphics.Color
import cn.xulun.bazi.model.Element

/** 深红（朱砂）主色与米色底色 */
val SeedRed = Color(0xFF8B0000)
val CreamBg = Color(0xFFFDF6EC)
val CreamSurface = Color(0xFFFFFBF3)
val PillarBorder = Color(0xFFE0D5C1)

/** 五行配色：木绿 / 火红 / 土棕 / 金黄 / 水蓝 */
val ElementColor: Map<Element, Color> = mapOf(
    Element.WOOD to Color(0xFF4CAF50),
    Element.FIRE to Color(0xFFF44336),
    Element.EARTH to Color(0xFF795548),
    Element.METAL to Color(0xFFFFC107),
    Element.WATER to Color(0xFF2196F3),
)

/** 取五行对应颜色 */
fun elementColor(element: Element): Color = ElementColor.getValue(element)

/** 地支六冲配色（朱红，表冲突） */
val ChongColor = Color(0xFFD32F2F)

/** 地支相刑配色（暗紫，表刑伤） */
val XingColor = Color(0xFF8E44AD)

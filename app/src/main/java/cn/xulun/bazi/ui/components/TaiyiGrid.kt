package cn.xulun.bazi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.SixteenGod
import cn.xulun.bazi.model.TaiyiPlate
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 太乙临宫描边色 */
private val TaiyiBorder = Color(0xFF6A1B9A)
/** 卷四星神盘上着色：吉绿凶红（与列表一致） */
private val StarGreen = Color(0xFF2E7D32)
private val StarRed = Color(0xFFC62828)
/** 三基走十二支（按支落间辰），余星神按宫号落正神格/中宫。 */
private val SAN_JI = setOf("君基", "臣基", "民基")
private fun starShort(name: String): String = name   // 完整星名入盘（君基/臣基/民基不缩写，避免丢字）

/**
 * 太乙地盘 5×5：外环 16 间辰（每宫拆两间辰）+ 中宫。上南下北。
 * 每个间辰格显示「位+宫数 / 十六神名 / 所临将神」；中宫显示落中五的将（杜塞）。
 * -1 = 空白内格，-2 = 中宫(5)。
 */
private val GRID16: List<List<Int>> = listOf(
    listOf(6, 7, 8, 9, 10),     // 巽 巳 午 未 坤
    listOf(5, -1, -1, -1, 11),  // 辰 _ _ _ 申
    listOf(4, -1, -2, -1, 12),  // 卯 _ 中 _ 酉
    listOf(3, -1, -1, -1, 13),  // 寅 _ _ _ 戌
    listOf(2, 1, 0, 15, 14),    // 艮 丑 子 亥 乾
)

/** 太乙宫号 → 正神间辰 idx（将按宫号落正神间辰；宫5→中宫）。 */
private fun palaceToZhengIdx(no: Int): Int? =
    if (no == 5) null else SixteenGod.entries.first { it.isZheng && it.palaceNo == no }.idx

@Composable
fun TaiyiGrid(
    plate: TaiyiPlate,
    modifier: Modifier = Modifier,
) {
    val taiyiIdx = palaceToZhengIdx(plate.ju.taiyiPalaceNo)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GRID16.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { code ->
                    when {
                        code == -2 -> CenterCell(plate, Modifier.weight(1f))
                        code == -1 -> Box(Modifier.weight(1f))
                        else -> JianChenCell(
                            god = SixteenGod.at(code),
                            occupants = occupantsAtIdx(plate, code),
                            jiXing = jiXingAtIdx(plate, code),
                            isTaiyi = taiyiIdx == code,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** 某间辰(idx)所临的将/神。 */
private fun occupantsAtIdx(plate: TaiyiPlate, idx: Int): List<String> {
    val list = mutableListOf<String>()
    if (palaceToZhengIdx(plate.ju.taiyiPalaceNo) == idx) list += "太乙"
    if (plate.wenChang.idx == idx) list += "文昌"
    if (plate.shiJi.idx == idx) list += "始击"
    if (plate.dingMu.idx == idx) list += "定目"
    if (SixteenGod.ofBranch(plate.taiSui).idx == idx) list += "太岁"
    if (SixteenGod.ofBranch(plate.heShen).idx == idx) list += "合神"
    if (SixteenGod.ofBranch(plate.jiShen).idx == idx) list += "计神"
    listOf(
        "主大" to plate.zhuDaJiang, "主参" to plate.zhuCanJiang,
        "客大" to plate.keDaJiang, "客参" to plate.keCanJiang,
        "定大" to plate.dingDaJiang, "定参" to plate.dingCanJiang,
    ).forEach { (n, g) -> if (g != 5 && palaceToZhengIdx(g) == idx) list += n }
    return list
}

/** 某间辰(idx)所临的卷四星神（简称,吉凶）：三基走支→按支十六神idx；福/游/符神→按宫号正神idx。落中5者另算。 */
private fun jiXingAtIdx(plate: TaiyiPlate, idx: Int): List<Pair<String, Boolean>> {
    val out = mutableListOf<Pair<String, Boolean>>()
    plate.jiXing.forEach { s ->
        val hit = if (s.name in SAN_JI) {
            // 三基走十二支：按支的十六神 idx 落间辰/正神格（同太岁/合神/计神法）
            SixteenGod.entries.firstOrNull { it.position == s.location }?.idx == idx
        } else {
            // 福星/游神/符神：按宫号落正神格（中5归中宫，不在此列）
            s.palaceNo != null && s.palaceNo != 5 && palaceToZhengIdx(s.palaceNo) == idx
        }
        if (hit) out += starShort(s.name) to s.auspicious
    }
    return out
}

/** 落中宫(5)的卷四星神。 */
private fun centerJiXing(plate: TaiyiPlate): List<Pair<String, Boolean>> =
    plate.jiXing.filter { it.palaceNo == 5 }.map { starShort(it.name) to it.auspicious }

/** 落中宫的将（杜塞）。 */
private fun centerOccupants(plate: TaiyiPlate): List<String> =
    listOf(
        "主大" to plate.zhuDaJiang, "客大" to plate.keDaJiang, "定大" to plate.dingDaJiang,
        "主参" to plate.zhuCanJiang, "客参" to plate.keCanJiang, "定参" to plate.dingCanJiang,
    ).filter { it.second == 5 }.map { it.first }

@Composable
private fun JianChenCell(
    god: SixteenGod,
    occupants: List<String>,
    jiXing: List<Pair<String, Boolean>>,
    isTaiyi: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTaiyi) SeedRed.copy(alpha = 0.10f) else CreamSurface,
        ),
        border = if (isTaiyi) BorderStroke(2.dp, TaiyiBorder) else null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${god.position}${god.palaceNo ?: ""}",
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF424242),
            )
            Text(god.title, fontSize = 9.sp, color = Color(0xFF9E9E9E))
            if (occupants.isNotEmpty()) {
                Text(
                    occupants.joinToString(" "),
                    fontSize = 9.sp,
                    fontWeight = if (isTaiyi) FontWeight.Bold else FontWeight.Medium,
                    color = if (occupants.any { it == "太乙" }) SeedRed else Color(0xFF1565C0),
                )
            }
            if (jiXing.isNotEmpty()) {
                Text(
                    buildAnnotatedString {
                        jiXing.forEachIndexed { i, p ->
                            if (i > 0) append(" ")
                            withStyle(SpanStyle(color = if (p.second) StarGreen else StarRed)) { append(p.first) }
                        }
                    },
                    fontSize = 9.sp, fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun CenterCell(plate: TaiyiPlate, modifier: Modifier = Modifier) {
    val occ = centerOccupants(plate)
    val stars = centerJiXing(plate)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("中5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9E9E9E))
            if (occ.isNotEmpty()) {
                Text(occ.joinToString(" "), fontSize = 9.sp, color = SeedRed, fontWeight = FontWeight.Bold)
            }
            if (stars.isNotEmpty()) {
                Text(
                    buildAnnotatedString {
                        stars.forEachIndexed { i, p ->
                            if (i > 0) append(" ")
                            withStyle(SpanStyle(color = if (p.second) StarGreen else StarRed)) { append(p.first) }
                        }
                    },
                    fontSize = 9.sp, fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

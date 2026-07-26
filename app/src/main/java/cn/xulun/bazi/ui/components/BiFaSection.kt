package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.BiFaHit
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

private val GoodColor = Color(0xFF2E7D32)
private val BadColor = Color(0xFFC62828)
private val NoteColor = Color(0xFF616161)

/** 毕法赋分析：吉格 / 凶格 / 提示 分区，每条法号·名 + 断语 + 依据。 */
@Composable
fun BiFaSection(hits: List<BiFaHit>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("毕法赋分析", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SeedRed)
            Text(
                "依《六壬大全》百法逐条套用四课三传天将；冷门法取通行版，依据列命中要素以便核对",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 2.dp),
            )

            val good = hits.filter { !it.note && it.auspicious }
            val bad = hits.filter { !it.note && !it.auspicious }
            val notes = hits.filter { it.note }

            if (good.isNotEmpty()) {
                SectionLabel("吉格（${good.size}）")
                good.forEach { HitRow(it, GoodColor) }
            }
            if (bad.isNotEmpty()) {
                SectionLabel("凶格（${bad.size}）")
                bad.forEach { HitRow(it, BadColor) }
            }
            if (notes.isNotEmpty()) {
                SectionLabel("提示（${notes.size}）")
                notes.forEach { HitRow(it, NoteColor) }
            }
            if (good.isEmpty() && bad.isEmpty()) {
                Text("本课无明显吉凶定格", fontSize = 13.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = SeedRed.copy(alpha = 0.8f),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun HitRow(hit: BiFaHit, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${hit.no.toString().padStart(2)} ${hit.name}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = color,
            )
        }
        Text(hit.duanYu, fontSize = 12.sp, color = Color(0xFF424242), modifier = Modifier.padding(top = 1.dp))
        Text("依据：${hit.basis}", fontSize = 11.sp, color = Color(0xFF9E9E9E))
    }
}

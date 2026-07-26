@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cn.xulun.bazi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.PalaceAnalysis
import cn.xulun.bazi.model.PalaceTone
import cn.xulun.bazi.ui.theme.SeedRed

private val ToneGood = Color(0xFF2E7D32)
private val ToneBad = Color(0xFFC62828)
private val ToneNeutral = Color(0xFF424242)

private fun PalaceTone.color(): Color = when (this) {
    PalaceTone.GOOD -> ToneGood
    PalaceTone.BAD -> ToneBad
    PalaceTone.NEUTRAL -> ToneNeutral
}

/** 单宫分析弹层：点击九宫某格后弹出，展示该宫各层生克与格局 */
@Composable
fun QimenPalaceSheet(
    analysis: PalaceAnalysis,
    onDismiss: () -> Unit,
) {
    val state = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            // 标题区
            Row(verticalAlignment = Alignment.Bottom) {
                Text(analysis.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SeedRed)
                Spacer(Modifier.width(10.dp))
                Text(analysis.subtitle, fontSize = 13.sp, color = Color(0xFF616161), modifier = Modifier.padding(bottom = 3.dp))
            }
            if (analysis.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    analysis.tags.forEach { tag -> TagChip(tag) }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 各层判读
            analysis.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        item.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeedRed,
                        modifier = Modifier.width(64.dp),
                    )
                    Text(item.text, fontSize = 13.sp, color = item.tone.color(), modifier = Modifier.fillMaxWidth())
                }
            }

            // 本宫格局
            if (analysis.patterns.isNotEmpty()) {
                SectionLabel("本宫格局")
                analysis.patterns.forEach { p ->
                    val c = if (p.auspicious) ToneGood else ToneBad
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c)
                        Text("依据：${p.basis}", fontSize = 11.sp, color = Color(0xFF757575))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 综合断语
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SeedRed.copy(alpha = 0.08f))
                    .padding(12.dp),
            ) {
                Text("综合", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SeedRed)
                Text(analysis.summary, fontSize = 14.sp, color = Color(0xFF333333), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SeedRed.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, fontSize = 11.sp, color = SeedRed, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF616161),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

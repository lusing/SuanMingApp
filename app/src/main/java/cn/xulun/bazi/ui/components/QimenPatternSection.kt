package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.QiPattern
import cn.xulun.bazi.ui.theme.CreamSurface

/** 格局分析卡：吉格（绿）+ 凶格（红）+ 基础判读 */
@Composable
fun QimenPatternSection(
    patterns: List<QiPattern>,
    basics: List<String>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "格局分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "依通行定式匹配天盘地盘、门宫、星神；个别格局门派定义有出入，以命中元素为准",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )

            val lucky = patterns.filter { it.auspicious }
            val fierce = patterns.filter { !it.auspicious }

            if (lucky.isNotEmpty()) {
                SectionLabel("吉格", Color(0xFF2E7D32))
                lucky.forEach { PatternRow(it, Color(0xFF2E7D32)) }
            }
            if (fierce.isNotEmpty()) {
                SectionLabel("凶格", Color(0xFFC62828))
                fierce.forEach { PatternRow(it, Color(0xFFC62828)) }
            }
            if (patterns.isEmpty()) {
                Text(
                    "无明显大吉大凶之格",
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (basics.isNotEmpty()) {
                SectionLabel("基础判读", Color(0xFF616161))
                basics.forEach {
                    Text(it, fontSize = 12.sp, color = Color(0xFF424242), modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun PatternRow(p: QiPattern, color: Color) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
        Text("依据：${p.basis}", fontSize = 11.sp, color = Color(0xFF757575))
    }
}

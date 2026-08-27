package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import cn.xulun.bazi.model.ShiShenResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/**
 * 十神关系（柱位寓意）展示区域。理论见 .shengke-reference.md 第六三节起。
 * 每条 = 柱位标签 + 喜忌徽标 + 寓意正文（透干×柱位 / 十神坐十神组合）。
 */
@Composable
fun ShiShenSection(result: ShiShenResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "十神关系",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "寓意以喜忌为前提——喜用显正面、忌神显负面，程度随强弱喜忌而定",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
            )

            if (result.items.isEmpty()) {
                Text(
                    text = "柱中暂无成表十神（正印寓意表已就绪，其余十神待教材续节）",
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 10.dp),
                )
                return@Column
            }

            result.items.forEach { item ->
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = item.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeedRed,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = "【${item.xiJi}】",
                        fontSize = 11.sp,
                        color = if (item.xiJi == "喜用") Color(0xFF2E7D32) else if (item.xiJi == "忌神") Color(0xFFC62828) else Color(0xFF9E9E9E),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = item.meaning,
                    fontSize = 13.sp,
                    color = Color(0xFF424242),
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

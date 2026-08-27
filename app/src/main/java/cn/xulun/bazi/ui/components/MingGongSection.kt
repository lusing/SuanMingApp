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
import cn.xulun.bazi.model.MingGongResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 命宫 / 胎元 / 小限 / 小运展示区域（附加柱）。 */
@Composable
fun MingGongSection(result: MingGongResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "命宫 · 胎元 · 小限 · 小运",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "命宫 ${result.mingGong.fullName}　${result.starName}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(text = result.starText, fontSize = 12.sp, color = Color(0xFF616161))
            Text(
                text = "胎元 ${result.taiYuan.fullName}　" +
                    "小限（${result.xiaoXianYear}年）${result.xiaoXian.fullName}　" +
                    "小运（${result.xiaoYunAge}岁·${if (result.xiaoYunForward) "顺" else "逆"}）${result.xiaoYun.fullName}",
                fontSize = 13.sp,
            )
            Text(
                text = "命宫推算：${result.mingGongBasis}；命宫可视为第五柱，其干支五行参与命局喜忌。" +
                    "小限小运随${if (result.xiaoXianYear > 0) "选定流年" else "出生年"}显示。",
                fontSize = 12.sp,
                color = if (result.xiaoYunForward) Color(0xFF757575) else SeedRed.copy(alpha = 0.7f),
                lineHeight = 18.sp,
            )
        }
    }
}

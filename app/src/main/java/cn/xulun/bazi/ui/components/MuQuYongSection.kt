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
import cn.xulun.bazi.model.MuQuYongResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 穷通取用·木（甲乙日主的逐月取用）展示区域。 */
@Composable
fun MuQuYongSection(result: MuQuYongResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "穷通取用 · 木",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "日主${result.strength.chinese}　用神：${result.yongShen.chinese}" +
                    (result.fuYong?.let { "（兼${it.chinese}）" } ?: "") +
                    "　调候：${result.tiaoHou.chinese}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (result.special != cn.xulun.bazi.model.MuSpecial.NONE) {
                Text(text = "【${result.special.chinese}】", fontSize = 13.sp, color = SeedRed)
            }
            Text(text = result.basis, fontSize = 12.sp, color = Color(0xFF757575), lineHeight = 18.sp)
        }
    }
}

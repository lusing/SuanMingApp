package cn.xulun.bazi.ui.components

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
import cn.xulun.bazi.model.JiaoJiResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 交际能力展示区域：天赋 · 风格 · 水智 · 运转折 */
@Composable
fun JiaoJiSection(result: JiaoJiResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "交际能力",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "交际以语言（口语/眼神/肢体）为载体——食伤为代表；水主智，水强者应变好",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(result.talent, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SeedRed, modifier = Modifier.padding(top = 10.dp))
            Text("依据：${result.basis}", fontSize = 11.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(top = 2.dp))
            if (result.style.isNotEmpty()) {
                Text("交际风格：${result.style}", fontSize = 12.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 8.dp))
            }
            if (result.waterNote.isNotEmpty()) {
                Text("水智：${result.waterNote}", fontSize = 12.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 4.dp))
            }
            if (result.yunTurning.isNotEmpty()) {
                Text("运转折：${result.yunTurning}", fontSize = 12.sp, color = SeedRed, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

package cn.xulun.bazi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.GeJuResult
import cn.xulun.bazi.ui.theme.SeedRed

/** 格局横幅：格局名 + 身强弱 + 判定依据 */
@Composable
fun GeJuBanner(geju: GeJuResult, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SeedRed.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .border(1.dp, SeedRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = geju.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SeedRed,
            )
            Row(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .background(SeedRed.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = geju.strength.chinese,
                    fontSize = 12.sp,
                    color = SeedRed,
                )
            }
        }
        Text(
            text = geju.basis,
            fontSize = 11.sp,
            color = Color(0xFF757575),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

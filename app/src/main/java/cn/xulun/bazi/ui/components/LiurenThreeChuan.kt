package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.core.LiuRenTables
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.LiuRenPlate
import cn.xulun.bazi.model.TianJiang
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor

/** 三传：初/中/末，每传标天将(所乘)与旬空。附宗门课名。 */
@Composable
fun LiurenThreeChuan(plate: LiuRenPlate, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val labels = listOf("初传" to plate.threeChuan.initial, "中传" to plate.threeChuan.middle, "末传" to plate.threeChuan.last)
            labels.forEach { (label, branch) ->
                ChuanColumn(label, branch, plate, modifier = Modifier.weight(1f))
            }
        }
        Text(
            "课体：${plate.threeChuan.keMing}（${plate.threeChuan.zongMen.chinese}）",
            fontSize = 13.sp,
            color = SeedRed,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun ChuanColumn(label: String, branch: EarthlyBranch, plate: LiuRenPlate, modifier: Modifier = Modifier) {
    val jiang = plate.tianJiang.firstOrNull { it.heavenBranch == branch }?.jiang
    val kong = LiuRenTables.isKong(branch, plate.hourCycleIndex)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, fontSize = 11.sp, color = SeedRed, fontWeight = FontWeight.Bold)
            Text(
                branch.chinese,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = elementColor(branch.element),
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                jiang?.chinese ?: "",
                fontSize = 12.sp,
                color = if (jiang?.auspicious == true) Color(0xFF2E7D32) else Color(0xFFC62828),
            )
            if (kong) Text("空", fontSize = 10.sp, color = Color(0xFF9E9E9E))
        }
    }
}

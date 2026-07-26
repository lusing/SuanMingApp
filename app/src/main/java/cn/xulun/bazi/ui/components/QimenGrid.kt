package cn.xulun.bazi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.PalaceCell
import cn.xulun.bazi.model.QiMenPlate
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor

/** 选中宫描边色 */
private val SelectedBorder = Color(0xFF00897B)

/** 后天八卦九宫方位（上南下北）：顶[巽4 离9 坤2] 中[震3 中5 兑7] 底[艮8 坎1 乾6] */
private val gridOrder: List<List<Int>> = listOf(
    listOf(4, 9, 2),
    listOf(3, 5, 7),
    listOf(8, 1, 6),
)

@Composable
fun QimenGrid(
    plate: QiMenPlate,
    modifier: Modifier = Modifier,
    selectedPalace: Int? = null,
    onPalaceClick: (Int) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        gridOrder.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { no ->
                    val cell = plate.cells[no]
                    if (cell != null) {
                        PalaceCellView(
                            cell = cell,
                            selected = selectedPalace == no,
                            onClick = { onPalaceClick(no) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PalaceCellView(
    cell: PalaceCell,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val border = when {
        selected -> BorderStroke(2.5.dp, SelectedBorder)
        cell.isZhiFuPalace -> BorderStroke(2.dp, SeedRed)
        cell.isZhiShiPalace -> BorderStroke(2.dp, Color(0xFF1565C0))
        else -> null
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) CreamSurface.copy(alpha = 0.6f) else CreamSurface),
        border = border,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶：九星 + 八神
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    cell.star.chinese,
                    fontSize = 10.sp,
                    color = if (cell.star.auspicious) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    cell.spirit?.chinese ?: "",
                    fontSize = 10.sp,
                    color = Color(0xFF616161),
                )
            }
            // 中：天盘干(大) / 地盘干(小)
            Text(
                cell.heavenGan.chinese,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = elementColor(cell.heavenGan.stem.element),
            )
            Text(
                cell.earthGan.chinese,
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
            )
            // 底：八门 + 宫名
            Text(
                "${cell.palace.trigram}${cell.palace.no}·${cell.door?.chinese ?: ""}门",
                fontSize = 10.sp,
                color = Color(0xFF757575),
            )
            if (cell.palace.no == 5) {
                Text("寄坤2", fontSize = 9.sp, color = SeedRed.copy(alpha = 0.7f))
            }
            if (cell.isZhiFuPalace) {
                Text("符", fontSize = 9.sp, color = SeedRed, fontWeight = FontWeight.Bold)
            } else if (cell.isZhiShiPalace) {
                Text("使", fontSize = 9.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
            }
        }
    }
}

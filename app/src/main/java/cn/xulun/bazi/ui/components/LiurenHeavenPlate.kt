package cn.xulun.bazi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.LiuRenPlate
import cn.xulun.bazi.model.TianJiang
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor

internal val LIUREN_HEAVEN_PLATE_GRID: List<List<EarthlyBranch?>> = listOf(
    listOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI, EarthlyBranch.SHEN),
    listOf(EarthlyBranch.CHEN, null, null, EarthlyBranch.YOU),
    listOf(EarthlyBranch.MAO, null, null, EarthlyBranch.XU),
    listOf(EarthlyBranch.YIN, EarthlyBranch.CHOU, EarthlyBranch.ZI, EarthlyBranch.HAI),
)

internal data class LiurenHeavenPlateCell(
    val earthBranch: EarthlyBranch,
    val heavenBranch: EarthlyBranch,
    val tianJiang: TianJiang,
)

internal fun liurenHeavenPlateCell(
    plate: LiuRenPlate,
    earthBranch: EarthlyBranch,
): LiurenHeavenPlateCell = LiurenHeavenPlateCell(
    earthBranch = earthBranch,
    heavenBranch = plate.heavenAt(earthBranch),
    tianJiang = plate.tianJiang.single { it.earthBranch == earthBranch }.jiang,
)

/** 天地盘十二宫方盘：上南下北，每宫显示地盘支、天盘支与所乘天将。 */
@Composable
fun LiurenHeavenPlate(
    plate: LiuRenPlate,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LIUREN_HEAVEN_PLATE_GRID.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.forEach { earthBranch ->
                        if (earthBranch == null) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        } else {
                            LiurenHeavenPlateCellView(
                                cell = liurenHeavenPlateCell(plate, earthBranch),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.46f)
                .aspectRatio(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = CreamSurface),
            border = BorderStroke(1.dp, SeedRed.copy(alpha = 0.25f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "${plate.yueJiang.title}将·${plate.yueJiang.branch.chinese}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                )
                Text(
                    "占时·${plate.hourBranch.chinese}",
                    fontSize = 12.sp,
                    color = Color(0xFF616161),
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "上南下北",
                    fontSize = 10.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LiurenHeavenPlateCellView(
    cell: LiurenHeavenPlateCell,
    modifier: Modifier = Modifier,
) {
    val jiangColor = if (cell.tianJiang.auspicious) {
        Color(0xFF2E7D32)
    } else {
        Color(0xFFC62828)
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                cell.earthBranch.chinese,
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
            )
            Text(
                cell.heavenBranch.chinese,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = elementColor(cell.heavenBranch.element),
            )
            Text(
                cell.tianJiang.chinese,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = jiangColor,
                maxLines = 1,
            )
        }
    }
}

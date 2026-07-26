package cn.xulun.bazi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import cn.xulun.bazi.model.XiJi
import cn.xulun.bazi.model.YongShenResult
import cn.xulun.bazi.model.YongShenStar
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor

private val GoodColor = Color(0xFF2E7D32)
private val BadColor = Color(0xFFC62828)
private val NoteColor = Color(0xFF616161)

/** 子平真诠 用神 / 相神 分析展示区。 */
@Composable
fun YongShenSection(result: YongShenResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "用神 · 相神（子平真诠）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "月令格局派：用神取自月令所藏，相神护卫成格，据成败救应断格局高低",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )

            // 用神
            SectionLabel("用神（月令所藏）", Modifier.padding(top = 14.dp))
            val ys = result.yongShen
            Row(verticalAlignment = Alignment.Bottom) {
                if (ys != null) {
                    Text(
                        text = "${ys.stem.chinese}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = elementColor(ys.stem.element),
                    )
                    Text(
                        text = "（${ys.tenGod.chinese}）",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                    )
                }
                Text(
                    text = "　·　${result.geJuName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Text(
                text = "${result.source.chinese}　${result.yongShenBasis}",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 2.dp),
            )

            // 相神
            SectionLabel("相神（辅用成格）", Modifier.padding(top = 14.dp))
            val xs = result.xiangShen
            if (xs != null) {
                StarChip(xs, subtitle = result.xiangShenRole)
                Text(result.xiangShenBasis, fontSize = 11.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(top = 2.dp))
            } else {
                HintText("原局无得力相神，格欠清纯（" + result.xiangShenBasis + "）")
            }

            // 格局成败
            SectionLabel("格局成败", Modifier.padding(top = 14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.chengBai.chinese,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = chengBaiColor(result),
                    modifier = Modifier
                        .background(chengBaiColor(result).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            Text(result.chengBaiReason, fontSize = 12.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 4.dp))

            // 喜忌
            if (result.xiJi.isNotEmpty()) {
                SectionLabel("运岁喜忌", Modifier.padding(top = 14.dp))
                val xi = result.xiJi.filter { it.kind == XiJi.XI }
                val ji = result.xiJi.filter { it.kind == XiJi.JI }
                if (xi.isNotEmpty()) {
                    Text("喜", fontSize = 12.sp, color = GoodColor)
                    ChipFlow {
                        xi.forEach { StarChipSmall("${it.tenGod.chinese}（${it.desc}）", GoodColor) }
                    }
                }
                if (ji.isNotEmpty()) {
                    Text("忌", fontSize = 12.sp, color = BadColor, modifier = Modifier.padding(top = 4.dp))
                    ChipFlow {
                        ji.forEach { StarChipSmall("${it.tenGod.chinese}（${it.desc}）", BadColor) }
                    }
                }
            }

            // 断语
            SectionLabel("断语", Modifier.padding(top = 14.dp))
            Text(result.duanYu, fontSize = 13.sp, color = SeedRed, fontWeight = FontWeight.Medium)
        }
    }
}

private fun chengBaiColor(result: YongShenResult): Color = when {
    result.chengBai.auspicious -> GoodColor
    result.chengBai == cn.xulun.bazi.model.GeJuChengBai.PIAN -> NoteColor
    else -> BadColor
}

@Composable
private fun StarChip(star: YongShenStar, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${star.stem.chinese} ${star.tenGod.chinese}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = elementColor(star.stem.element),
        )
        Text(
            text = "（${subtitle}·${star.location}${if (star.hidden) "·藏" else "·透"}）",
            fontSize = 11.sp,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

@Composable
private fun StarChipSmall(text: String, color: Color) {
    Text(text, fontSize = 12.sp, color = color, modifier = Modifier.padding(end = 6.dp, bottom = 2.dp))
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = SeedRed.copy(alpha = 0.8f),
        modifier = modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun HintText(text: String) {
    Text(text = text, fontSize = 13.sp, color = Color(0xFF9E9E9E))
}

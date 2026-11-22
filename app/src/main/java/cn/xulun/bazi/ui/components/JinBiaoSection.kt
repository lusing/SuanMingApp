package cn.xulun.bazi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.JinBiaoHealth
import cn.xulun.bazi.model.JinBiaoResult
import cn.xulun.bazi.model.JinBiaoVerdict
import cn.xulun.bazi.ui.theme.elementColor

// 金镖门模块专属配色（与老引擎 SeedRed 区分：玄墨底 + 鎏金）
private val JinBiaoInk = Color(0xFF263238)
private val JinBiaoGold = Color(0xFFC9A227)
private val GoldSoft = Color(0xFF8A6D1D)
private val GoodColor = Color(0xFF2E7D32)
private val BadColor = Color(0xFFC62828)
private val NoteColor = Color(0xFF616161)

/**
 * 盲派金镖门 · 专门分析模块（独立于老引擎的新口径）。
 * 讲义：.jinbiao-reference.md。带有事实的案例仍以老引擎（传统综合区块）对照分析。
 */
@Composable
fun JinBiaoSection(result: JinBiaoResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = JinBiaoInk),
    ) {
        // 模块鎏金页眉
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "盲派金镖门",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JinBiaoGold,
                )
                Text(
                    text = "　独立分析模块 · 新引擎",
                    fontSize = 11.sp,
                    color = JinBiaoGold.copy(alpha = 0.7f),
                )
            }
            HorizontalDivider(color = JinBiaoGold.copy(alpha = 0.35f))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(result.kouJing, fontSize = 11.sp, color = Color(0xFFB0BEC5))

            // ── 日主定位与弱极 ──
            GoldLabel("日主定位", Modifier.padding(top = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(result.strength.chinese, JinBiaoGold)
                if (result.ruoJi) {
                    Badge("弱极 · ${result.ruoJiType!!.chinese}", BadColor, Modifier.padding(start = 8.dp))
                }
                if (result.wangJi) {
                    val prefix = if (result.wangJiType == cn.xulun.bazi.model.JinBiaoWangJiType.YIN_BI_TONG_XIN) "强极" else "旺极"
                    Badge("$prefix · ${result.wangJiType!!.chinese}", BadColor, Modifier.padding(start = 8.dp))
                }
            }
            Text(
                text = result.strengthBasis,
                fontSize = 11.sp,
                color = Color(0xFF90A4AE),
                modifier = Modifier.padding(top = 4.dp),
            )
            if (result.ruoJi && result.ruoJiBasis != null) {
                Text(
                    text = "弱极判据：${result.ruoJiBasis}",
                    fontSize = 12.sp,
                    color = BadColor,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (result.wangJi && result.wangJiBasis != null) {
                val prefix = if (result.wangJiType == cn.xulun.bazi.model.JinBiaoWangJiType.YIN_BI_TONG_XIN) "强极" else "旺极"
                Text(
                    text = "$prefix 判据：${result.wangJiBasis}",
                    fontSize = 12.sp,
                    color = BadColor,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // ── 用神（模块核心） ──
            GoldLabel("用神（对日主最需要最有帮助之五行，十神代称）", Modifier.padding(top = 16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = result.yongShenElement.chinese,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = elementColor(result.yongShenElement),
                )
                Text(
                    text = "（${result.yongShenGod.chinese}）",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
                )
                Badge(result.quYongWay.chinese, JinBiaoGold, Modifier.padding(start = 10.dp).padding(bottom = 4.dp))
            }
            Text(
                text = "取用依据：${result.quYongBasis}",
                fontSize = 11.sp,
                color = Color(0xFF90A4AE),
                modifier = Modifier.padding(top = 2.dp),
            )
            if (result.yongShenStars.isNotEmpty()) {
                Text(
                    text = "命中用神之字：${result.yongShenStars.joinToString("　")}",
                    fontSize = 12.sp,
                    color = Color(0xFFCFD8DC),
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(
                    text = "用神五行不现于原局——以虚神论（应于运岁补入之时）",
                    fontSize = 12.sp,
                    color = GoldSoft,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // 十神代五行
            GoldLabel("十神代五行（十神是五行的代名词）", Modifier.padding(top = 12.dp))
            StanceFlow {
                result.daiMing.forEach { dm ->
                    Text(
                        text = "${dm.relation}→${dm.category}(${dm.element.chinese})",
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5),
                        modifier = Modifier.padding(end = 10.dp, bottom = 3.dp),
                    )
                }
            }

            // ── 用神健全度 ──
            GoldLabel("用神健全度（得力健全则八字好，无气受伤则不好）", Modifier.padding(top = 12.dp))
            val healthColor = when (result.health.level) {
                JinBiaoHealth.DE_LI -> GoodColor
                else -> BadColor
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(result.health.level.chinese, healthColor)
                Text(
                    text = " ${result.health.rootNote}",
                    fontSize = 12.sp,
                    color = Color(0xFFCFD8DC),
                )
            }
            if (result.health.supports.isNotEmpty()) {
                Text(
                    text = "得扶：" + result.health.supports.joinToString("、"),
                    fontSize = 11.sp,
                    color = GoodColor,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (result.health.damages.isNotEmpty()) {
                Text(
                    text = "受制：" + result.health.damages.joinToString("、"),
                    fontSize = 11.sp,
                    color = BadColor,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // ── 清浊（第六讲） ──
            GoldLabel("清浊（用神有力无病为清）", Modifier.padding(top = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(
                    result.qingZhuo.chinese,
                    when (result.qingZhuo.auspicious) {
                        true -> GoodColor
                        false -> BadColor
                        null -> NoteColor
                    },
                )
            }
            Text(
                text = result.qingZhuoBasis,
                fontSize = 11.sp,
                color = Color(0xFF90A4AE),
                modifier = Modifier.padding(top = 4.dp),
            )

            // ── 喜忌（帮用/损用） ──
            GoldLabel("喜忌（一律以用神为中心）", Modifier.padding(top = 12.dp))
            val daiMingOf: (Element) -> String = { el ->
                result.daiMing.firstOrNull { it.element == el }?.category ?: el.chinese
            }
            StanceFlow {
                Text("帮用则吉：", fontSize = 12.sp, color = GoodColor, modifier = Modifier.padding(bottom = 3.dp))
                result.xiElements.forEach { el ->
                    Text(
                        text = "${daiMingOf(el)}(${el.chinese})",
                        fontSize = 12.sp,
                        color = GoodColor,
                        modifier = Modifier.padding(end = 8.dp, bottom = 3.dp),
                    )
                }
            }
            StanceFlow {
                Text("耗泄克破则凶：", fontSize = 12.sp, color = BadColor, modifier = Modifier.padding(bottom = 3.dp))
                result.jiElements.forEach { el ->
                    Text(
                        text = "${daiMingOf(el)}(${el.chinese})",
                        fontSize = 12.sp,
                        color = BadColor,
                        modifier = Modifier.padding(end = 8.dp, bottom = 3.dp),
                    )
                }
            }

            // ── 运年评断 ──
            if (result.yuns.isNotEmpty()) {
                GoldLabel("大运流年（帮扶用神则吉，耗泄克破则凶）", Modifier.padding(top = 12.dp))
                result.yuns.forEach { yun ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Text(
                            text = "${yun.yunName} ${yun.yearRange}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                        Badge(
                            yun.verdict.chinese,
                            verdictColor(yun.verdict),
                            Modifier.padding(start = 8.dp),
                        )
                        Text(
                            text = "　" + yun.items.joinToString("　") { "${it.chinese}${it.action}" },
                            fontSize = 11.sp,
                            color = Color(0xFF90A4AE),
                        )
                    }
                    val keyYears = yun.years.filter { it.verdict != JinBiaoVerdict.GAN_ZHI_FEN }
                    if (keyYears.isNotEmpty()) {
                        StanceFlow {
                            keyYears.forEach { y ->
                                Text(
                                    text = "${y.year}${y.pillarName}·${y.verdict.chinese}",
                                    fontSize = 11.sp,
                                    color = verdictColor(y.verdict),
                                    modifier = Modifier.padding(end = 8.dp, bottom = 3.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ── 断语 ──
            GoldLabel("断语", Modifier.padding(top = 14.dp))
            Text(
                text = result.duanYu,
                fontSize = 13.sp,
                color = JinBiaoGold,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JinBiaoGold.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
            )

            // ── 子女判读（第十一讲：子星按原局喜忌而定） ──
            result.ziNv?.let { zn ->
                GoldLabel("子女判读", Modifier.padding(top = 14.dp))
                Text(
                    text = "${zn.qingKuang.chinese}（子星：${zn.ziXing?.chinese ?: ""}·${zn.ziEl?.chinese ?: ""}）\n${zn.basis}",
                    fontSize = 12.sp,
                    color = JinBiaoGold,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(JinBiaoInk.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                )
            }

            // ── 学历判读（案九十四·学历章：印星不坏=脑子没问题；学历看求学运，干定学历） ──
            result.xueLi?.let { xl ->
                GoldLabel("学历判读", Modifier.padding(top = 14.dp))
                Text(
                    text = "${xl.level.chinese}（印${if (xl.yinWanHao) "完好" else "被破坏"}）\n${xl.basis}",
                    fontSize = 12.sp,
                    color = JinBiaoGold,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(JinBiaoInk.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                )
            }

            // 校准注记
            Text(
                text = "注：${result.note}",
                fontSize = 10.sp,
                color = Color(0xFF78909C),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** 老引擎/新引擎分区分隔标签（双引擎对照用）。 */
@Composable
fun EngineDivider(label: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        HorizontalDivider(
            color = Color(0xFFBCAAA4),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = " $label ",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8D6E63),
        )
        HorizontalDivider(
            color = Color(0xFFBCAAA4),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StanceFlow(content: @Composable () -> Unit) {
    // FlowRow 布局（金镖门模块内部用）
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        content()
    }
}

@Composable
private fun GoldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = JinBiaoGold.copy(alpha = 0.85f),
        modifier = modifier.padding(bottom = 5.dp),
    )
}

@Composable
private fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

private fun verdictColor(v: JinBiaoVerdict): Color = when (v) {
    JinBiaoVerdict.JI -> GoodColor
    JinBiaoVerdict.PING -> GoldSoft
    JinBiaoVerdict.XIONG -> BadColor
    JinBiaoVerdict.GAN_ZHI_FEN -> NoteColor
}

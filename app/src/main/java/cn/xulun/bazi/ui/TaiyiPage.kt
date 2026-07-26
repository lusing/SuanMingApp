@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cn.xulun.bazi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.core.TaiyiCalculator
import cn.xulun.bazi.model.TaiyiCountType
import cn.xulun.bazi.model.TaiyiFenYe
import cn.xulun.bazi.model.TaiyiJiXing
import cn.xulun.bazi.model.TaiyiPalace
import cn.xulun.bazi.model.TaiyiPlate
import cn.xulun.bazi.ui.components.TaiyiGrid
import cn.xulun.bazi.ui.theme.CreamBg
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

private val MinDate: LocalDate = LocalDate.of(1900, 1, 1)
private val MaxDate: LocalDate = LocalDate.of(2100, 12, 31)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaiyiPage(onBack: () -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var hourIndex by remember { mutableIntStateOf(0) }
    var countType by remember { mutableStateOf(TaiyiCountType.HOUR) }
    var plate by remember { mutableStateOf<TaiyiPlate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun generate() {
        val hour = HourOptions[hourIndex].representativeHour
        val dt = LocalDateTime.of(date, java.time.LocalTime.of(hour, 0))
        plate = TaiyiCalculator.build(dt, countType)
    }

    androidx.compose.material3.Scaffold(
        containerColor = CreamBg,
        topBar = {
            TopAppBar(
                title = { Text("太乙神数", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SeedRed,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    TextButton(onClick = onBack) { Text("八字", color = Color.White) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InputCard("起课时间", Icons.Filled.CalendarMonth) {
                DateField(date = date) { showDatePicker = true }
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HourOptions.forEachIndexed { i, opt ->
                        FilterChip(
                            selected = i == hourIndex,
                            onClick = { hourIndex = i },
                            label = { Text(opt.branch.chinese, fontWeight = if (i == hourIndex) FontWeight.Bold else FontWeight.Normal) },
                        )
                    }
                }
                Text(
                    if (countType == TaiyiCountType.HOUR) HourOptions[hourIndex].label else "年计仅取年份，时辰忽略",
                    fontSize = 13.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 6.dp),
                )
            }

            InputCard("计法", Icons.Filled.CalendarMonth) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TaiyiCountType.HOUR, TaiyiCountType.DAY, TaiyiCountType.YEAR).forEach { t ->
                        FilterChip(
                            selected = t == countType,
                            onClick = { countType = t },
                            label = { Text(t.chinese) },
                        )
                    }
                }
                Text("时计主日用百事；日计主日内选择/出行；年计主岁中大事。月计待实现", fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 6.dp))
            }

            Button(
                onClick = { generate() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SeedRed),
            ) { Text("起课排盘", fontSize = 18.sp, fontWeight = FontWeight.Bold) }

            plate?.let { p ->
                JuHeader(p)
                TaiyiGrid(plate = p)
                SuanSection(p)
                JiXingSection(p)
                FenYeChart(p)
                PatternSection(p)
            }
        }
    }

    if (showDatePicker) {
        val init = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = init,
            initialDisplayedMonthMillis = init,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis in MinDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()..
                    MaxDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun JuHeader(p: TaiyiPlate) {
    val ju = p.ju
    val palace = cn.xulun.bazi.model.TaiyiPalace.of(ju.taiyiPalaceNo)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CreamSurface)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${ju.dun.chinese}${ju.juNo}局",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = SeedRed, letterSpacing = 2.sp,
            )
            Text(
                "${ju.countType.chinese} · 太乙${palace.trigram}${ju.taiyiPalaceNo}宫(${palace.direction}) · ${ju.liTianDiRen}",
                fontSize = 13.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 4.dp),
            )
            val zhiShiText = p.zhiShiMen?.let { "直使${it.chinese}门" } ?: "日计八门阙载（卷一未载）"
            Text(
                "太岁${p.taiSui.chinese}　合神${p.heShen.chinese}　计神${p.jiShen.chinese}　$zhiShiText",
                fontSize = 12.sp, color = SeedRed.copy(alpha = 0.85f), modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                "文昌${p.wenChang.position}${p.wenChang.title}　始击${p.shiJi.position}${p.shiJi.title}",
                fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SuanSection(p: TaiyiPlate) {
    InputCard("主客定算", Icons.Filled.CalendarMonth) {
        SuanRow("主算", p.zhuSuan, p.zhuDaJiang, p.zhuCanJiang)
        SuanRow("客算", p.keSuan, p.keDaJiang, p.keCanJiang)
        SuanRow("定算", p.dingSuan, p.dingDaJiang, p.dingCanJiang)
        if (p.suanClass.isNotEmpty()) {
            Text(p.suanClass.joinToString("　"), fontSize = 12.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun SuanRow(label: String, suan: Int, da: Int, can: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("$label $suan", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text("大将${da}宫　参将${can}宫", fontSize = 13.sp, color = if (da == 5 || can == 5) SeedRed else Color(0xFF424242))
    }
}

@Composable
private fun PatternSection(p: TaiyiPlate) {
    if (p.patterns.isEmpty()) return
    InputCard("格局", Icons.Filled.CalendarMonth) {
        p.patterns.forEach { pat ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                Text(
                    pat.name,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = if (pat.auspicious) Color(0xFF2E7D32) else Color(0xFFC62828),
                )
                Text("　${pat.basis}", fontSize = 12.sp, color = Color(0xFF757575))
            }
        }
    }
}

@Composable
private fun JiXingSection(p: TaiyiPlate) {
    if (p.jiXing.isEmpty()) return
    InputCard("卷四基星 · 卷五符神", Icons.Filled.CalendarMonth) {
        p.jiXing.forEach { star ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                Text(
                    star.name,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = if (star.auspicious) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.width(44.dp),
                )
                Text(
                    "${star.location}　第${star.inYear}年　${star.period}",
                    fontSize = 13.sp, color = Color(0xFF424242),
                )
            }
            Text(
                star.note, fontSize = 11.sp, color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(start = 44.dp, bottom = 2.dp),
            )
        }
        Text(
            "卷四基星起戌邦·五福独立积年·大游起七宫；四神/天乙/地乙/直符依卷五立成十二位环；三堂分野依卷八，见下十二分野图",
            fontSize = 11.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun InputCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CreamSurface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = SeedRed, modifier = Modifier.size(20.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FenYeChart(p: TaiyiPlate) {
    val marks = TaiyiFenYe.markers(p)
    InputCard("十二分野图", Icons.Filled.CalendarMonth) {
        TaiyiPalace.gridOrder.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { no ->
                    FenYeCell("${no}宫·${TaiyiFenYe.palaceZhou[no]}", marks[no.toString()].orEmpty(), Modifier.weight(1f))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            TaiyiFenYe.tangOrder.forEach { tang ->
                FenYeCell("$tang·${TaiyiFenYe.tangZhou[tang]}", marks[tang].orEmpty(), Modifier.weight(1f))
            }
        }
        Text(
            "卷八 九宫分野(并序)：九宫(含中五)配九州；绛宫交州·明堂益州·玉堂幽州",
            fontSize = 11.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun FenYeCell(title: String, gods: List<TaiyiJiXing>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(2.dp),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = if (gods.isEmpty()) Color.White else Color(0xFFFFF8F0),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontSize = 12.sp, color = Color(0xFF616161))
            if (gods.isEmpty()) {
                Text("　", fontSize = 13.sp)
            } else {
                gods.forEach { g ->
                    Text(
                        g.name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (g.auspicious) Color(0xFF2E7D32) else Color(0xFFC62828),
                    )
                }
            }
        }
    }
}

@Composable
private fun DateField(date: LocalDate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${date.year}年${date.monthValue}月${date.dayOfMonth}日", fontSize = 18.sp)
            Text("▾", color = Color(0xFF9E9E9E))
        }
    }
}

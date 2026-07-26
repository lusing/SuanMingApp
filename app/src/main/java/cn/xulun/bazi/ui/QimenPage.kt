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
import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.QimenJuCalculator
import cn.xulun.bazi.core.QimenPatternCalculator
import cn.xulun.bazi.core.QimenPlateCalculator
import cn.xulun.bazi.model.JuMethod
import cn.xulun.bazi.model.QiMenPlate
import cn.xulun.bazi.ui.components.QimenGrid
import cn.xulun.bazi.ui.components.QimenPalaceSheet
import cn.xulun.bazi.ui.components.QimenPatternSection
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
fun QimenPage(onBack: () -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var hourIndex by remember { mutableIntStateOf(0) }
    var method by remember { mutableStateOf(JuMethod.CHAIBU) }
    var plate by remember { mutableStateOf<QiMenPlate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedPalace by remember { mutableStateOf<Int?>(null) }

    fun generate() {
        val hour = HourOptions[hourIndex].representativeHour
        val dt = LocalDateTime.of(date, java.time.LocalTime.of(hour, 0))
        val ju = QimenJuCalculator.determineJu(dt, method)
        plate = QimenPlateCalculator.buildPlate(dt, ju)
    }

    androidx.compose.material3.Scaffold(
        containerColor = CreamBg,
        topBar = {
            TopAppBar(
                title = { Text("奇门遁甲", fontWeight = FontWeight.Bold) },
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
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InputCard("起局时间", Icons.Filled.CalendarMonth) {
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
                Text(HourOptions[hourIndex].label, fontSize = 13.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 6.dp))
            }

            InputCard("定局法", Icons.Filled.CalendarMonth) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JuMethod.entries.forEach { m ->
                        FilterChip(
                            selected = m == method,
                            onClick = { method = m },
                            label = { Text(m.chinese) },
                        )
                    }
                }
                Text(method.desc, fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 6.dp))
            }

            Button(
                onClick = { generate() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SeedRed),
            ) {
                Text("起局排盘", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            plate?.let { p ->
                JuHeader(p)
                Text(
                    "点击任一宫查看该宫星门神、天地盘干生克与格局分析",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                )
                QimenGrid(
                    plate = p,
                    selectedPalace = selectedPalace,
                    onPalaceClick = { selectedPalace = it },
                )
                QimenPatternSection(
                    patterns = QimenPatternCalculator.analyze(p),
                    basics = QimenPatternCalculator.basics(p),
                )

                selectedPalace?.let { no ->
                    QimenPalaceSheet(
                        analysis = QimenPatternCalculator.analyzePalace(p, no),
                        onDismiss = { selectedPalace = null },
                    )
                }
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
private fun JuHeader(p: QiMenPlate) {
    val ju = p.ju
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${ju.dun.chinese}${ju.ju}局",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SeedRed,
                letterSpacing = 2.sp,
            )
            Text(
                buildString {
                    append(ju.termName)
                    append(" · ")
                    append(ju.yuan.chinese)
                    append(" · ")
                    append(ju.method.chinese)
                    if (ju.isLeap) append(" · 置闰")
                },
                fontSize = 13.sp,
                color = Color(0xFF616161),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "符头：${ju.fuTouName}　时辰：${p.hourPillar.fullName}（${p.dayPillar.fullName}日）",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                "直符：${p.zhiFuStar.chinese}　直使：${p.zhiShiDoor.chinese}门",
                fontSize = 12.sp,
                color = SeedRed.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
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

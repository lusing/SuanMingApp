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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.core.BiFaCalculator
import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.LiuRenCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.LiuRenPlate
import cn.xulun.bazi.ui.components.BiFaSection
import cn.xulun.bazi.ui.components.LiurenFourLessonGrid
import cn.xulun.bazi.ui.components.LiurenThreeChuan
import cn.xulun.bazi.ui.components.LiurenHeavenPlate
import cn.xulun.bazi.ui.theme.CreamBg
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

private val MinDate: LocalDate = LocalDate.of(1900, 1, 1)
private val MaxDate: LocalDate = LocalDate.of(2100, 12, 31)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiurenPage(onBack: () -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var hourIndex by remember { mutableIntStateOf(0) }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var ageText by remember { mutableStateOf("") }
    var benMingIndex by remember { mutableIntStateOf(-1) } // -1 = 不限
    var plate by remember { mutableStateOf<LiuRenPlate?>(null) }
    var bazi by remember { mutableStateOf<BaziResult?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun generate() {
        val hour = HourOptions[hourIndex].representativeHour
        val dt = LocalDateTime.of(date, java.time.LocalTime.of(hour, 0))
        val age = ageText.trim().toIntOrNull()
        val benMing = if (benMingIndex >= 0) EarthlyBranch.entries[benMingIndex] else null
        bazi = BaziCalculator.calculate(dt)
        plate = LiuRenCalculator.buildPlate(dt, gender, age, benMing)
    }

    androidx.compose.material3.Scaffold(
        containerColor = CreamBg,
        topBar = {
            TopAppBar(
                title = { Text("大六壬", fontWeight = FontWeight.Bold) },
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
                Text(HourOptions[hourIndex].label, fontSize = 13.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 6.dp))
            }

            InputCard("求测人（行年/本命，可选）", Icons.Filled.Person) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Gender.entries.forEach { g ->
                        FilterChip(selected = g == gender, onClick = { gender = g }, label = { Text(if (g == Gender.MALE) "男" else "女") })
                    }
                }
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("虚岁（推行年）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text("本命支（可选）", fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(selected = benMingIndex == -1, onClick = { benMingIndex = -1 }, label = { Text("不限") })
                    EarthlyBranch.entries.forEachIndexed { i, b ->
                        FilterChip(
                            selected = i == benMingIndex,
                            onClick = { benMingIndex = i },
                            label = { Text(b.chinese) },
                        )
                    }
                }
            }

            Button(
                onClick = { generate() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SeedRed),
            ) {
                Text("起课排盘", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            plate?.let { p ->
                PlateHeader(p, bazi)
                Text("天地盘（上南下北）", fontSize = 13.sp, color = Color(0xFF616161))
                LiurenHeavenPlate(p)
                Text("四课（上神 / 下神）", fontSize = 13.sp, color = Color(0xFF616161))
                LiurenFourLessonGrid(p)
                Text("三传（初/中/末 · 所乘天将）", fontSize = 13.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 4.dp))
                LiurenThreeChuan(p)
                BiFaSection(hits = BiFaCalculator.analyze(p))
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
private fun PlateHeader(p: LiuRenPlate, bazi: BaziResult?) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CreamSurface)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${p.yueJiang.title}将 · ${if (p.isDay) "昼占" else "夜占"} · ${p.threeChuan.keMing}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SeedRed,
            )
            if (bazi != null) {
                Text(
                    bazi.fullBazi,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                "日干${p.dayStem.chinese}(${p.dayStem.element.chinese}) 日支${p.dayBranch.chinese}　占时${p.hourBranch.chinese}",
                fontSize = 13.sp,
                color = Color(0xFF616161),
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

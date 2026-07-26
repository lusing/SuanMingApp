@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cn.xulun.bazi.ui

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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.data.ChartStorage
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.SavedChart
import cn.xulun.bazi.ui.theme.CreamBg
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.launch

/** 十二时辰选项（地支、代表小时、标签）。代表小时与参考应用一致（子时取 0）。 */
data class HourOption(val branch: EarthlyBranch, val representativeHour: Int, val label: String)

val HourOptions: List<HourOption> = listOf(
    HourOption(EarthlyBranch.ZI, 0, "子时 (23:00-00:59)"),
    HourOption(EarthlyBranch.CHOU, 1, "丑时 (01:00-02:59)"),
    HourOption(EarthlyBranch.YIN, 3, "寅时 (03:00-04:59)"),
    HourOption(EarthlyBranch.MAO, 5, "卯时 (05:00-06:59)"),
    HourOption(EarthlyBranch.CHEN, 7, "辰时 (07:00-08:59)"),
    HourOption(EarthlyBranch.SI, 9, "巳时 (09:00-10:59)"),
    HourOption(EarthlyBranch.WU, 11, "午时 (11:00-12:59)"),
    HourOption(EarthlyBranch.WEI, 13, "未时 (13:00-14:59)"),
    HourOption(EarthlyBranch.SHEN, 15, "申时 (15:00-16:59)"),
    HourOption(EarthlyBranch.YOU, 17, "酉时 (17:00-18:59)"),
    HourOption(EarthlyBranch.XU, 19, "戌时 (19:00-20:59)"),
    HourOption(EarthlyBranch.HAI, 21, "亥时 (21:00-22:59)"),
)

private val MinDate: LocalDate = LocalDate.of(1900, 1, 1)
private val MaxDate: LocalDate = LocalDate.of(2100, 12, 31)
private val MinMillis: Long = MinDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private val MaxMillis: Long = MaxDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaziPage(
    storage: ChartStorage,
    pendingLoad: SavedChart?,
    onPendingLoadConsumed: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenQimen: () -> Unit,
    onOpenLiuren: () -> Unit,
    onOpenTaiyi: () -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var hourIndex by remember { mutableIntStateOf(0) }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var result by remember { mutableStateOf<Pair<BaziResult, DaYunInfo>?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 从已存命盘载入
    LaunchedEffect(pendingLoad) {
        val chart = pendingLoad ?: return@LaunchedEffect
        val idx = HourOptions.indexOfFirst { it.representativeHour == chart.hour }
        date = LocalDate.of(chart.year, chart.month, chart.day)
        hourIndex = if (idx >= 0) idx else 0
        gender = chart.gender
        result = compute(date, hourIndex, gender)
        onPendingLoadConsumed()
    }

    fun generate() {
        result = compute(date, hourIndex, gender)
    }

    androidx.compose.material3.Scaffold(
        containerColor = CreamBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("生辰八字", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SeedRed,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    TextButton(onClick = onOpenLiuren) { Text("六壬", color = Color.White, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = onOpenQimen) { Text("奇门", color = Color.White, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = onOpenTaiyi) { Text("太乙", color = Color.White, fontWeight = FontWeight.Bold) }
                    IconButton(onClick = onOpenSaved) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "已保存的命盘")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 出生日期
            InputCard(title = "出生日期", icon = Icons.Filled.CalendarMonth) {
                DateField(date = date) { showDatePicker = true }
            }
            // 出生时辰
            InputCard(title = "出生时辰", icon = Icons.Filled.CalendarMonth) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HourOptions.forEachIndexed { i, opt ->
                        FilterChip(
                            selected = i == hourIndex,
                            onClick = { hourIndex = i },
                            label = {
                                Text(
                                    text = "${opt.branch.chinese}",
                                    fontWeight = if (i == hourIndex) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
                Text(
                    text = HourOptions[hourIndex].label,
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            // 性别
            InputCard(title = "性别（用于排大运）", icon = Icons.Filled.CalendarMonth) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Gender.entries.forEach { g ->
                        FilterChip(
                            selected = g == gender,
                            onClick = { gender = g },
                            label = { Text(g.label) },
                        )
                    }
                }
            }
            // 生成
            Button(
                onClick = { generate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SeedRed),
            ) {
                Text("生成八字", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            // 结果
            result?.let { (bazi, dayun) ->
                OutlinedButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SeedRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SeedRed),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("保存此盘")
                }
                BaziDisplay(result = bazi, daYun = dayun, gender = gender)
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.toUtcMillis(),
            initialDisplayedMonthMillis = date.toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis in MinMillis..MaxMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = millisToLocalDate(it) }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showSaveDialog) {
        SaveChartDialog(
            defaultName = "${date.year}年${date.monthValue}月${date.dayOfMonth}日",
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                showSaveDialog = false
                if (name.isEmpty()) return@SaveChartDialog
                val birthHour = HourOptions[hourIndex].representativeHour
                val now = System.currentTimeMillis()
                storage.save(
                    SavedChart(
                        id = now.toString(),
                        name = name,
                        year = date.year,
                        month = date.monthValue,
                        day = date.dayOfMonth,
                        hour = birthHour,
                        gender = gender,
                        savedAt = now,
                    ),
                )
                scope.launch { snackbarHostState.showSnackbar("已保存「$name」") }
            },
        )
    }
}

private fun compute(
    date: LocalDate,
    hourIndex: Int,
    gender: Gender,
): Pair<BaziResult, DaYunInfo> {
    val birth = LocalDateTime.of(date, java.time.LocalTime.of(HourOptions[hourIndex].representativeHour, 0))
    val bazi = BaziCalculator.calculate(birth)
    val dayun = DaYunCalculator.compute(bazi, gender)
    return bazi to dayun
}

@Composable
private fun InputCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = SeedRed, modifier = Modifier.size(20.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DateField(date: LocalDate, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveChartDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(defaultName) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存命盘") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("备注名") },
                placeholder = { Text("如：张三、我自己") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

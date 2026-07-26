@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cn.xulun.bazi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.data.ChartStorage
import cn.xulun.bazi.model.SavedChart
import cn.xulun.bazi.ui.theme.CreamBg
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedChartsPage(
    storage: ChartStorage,
    onClose: () -> Unit,
    onPick: (SavedChart) -> Unit,
) {
    var charts by remember { mutableStateOf<List<SavedChart>?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<SavedChart?>(null) }

    LaunchedEffect(reloadKey) {
        charts = storage.loadAll()
    }

    androidx.compose.material3.Scaffold(
        containerColor = CreamBg,
        topBar = {
            TopAppBar(
                title = { Text("已保存的命盘", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SeedRed,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        val list = charts
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                list == null -> CircularProgressIndicator()
                list.isEmpty() -> Text(
                    text = "暂无保存的命盘\n排盘后点击「保存此盘」即可保存",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF757575),
                    lineHeight = 28.sp,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.id }) { chart ->
                        SavedChartItem(
                            chart = chart,
                            onClick = { onPick(chart) },
                            onDelete = { pendingDelete = chart },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { chart ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除命盘") },
            text = { Text("确定删除「${chart.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    storage.delete(chart.id)
                    pendingDelete = null
                    reloadKey++
                }) { Text("删除", color = Color(0xFFD32F2F)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SavedChartItem(chart: SavedChart, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = SeedRed,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(chart.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "${chart.birthText} · ${chart.gender.label}",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "删除", tint = Color(0xFF9E9E9E))
            }
        }
    }
}

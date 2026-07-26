package cn.xulun.bazi.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cn.xulun.bazi.data.ChartStorage
import cn.xulun.bazi.model.SavedChart

private sealed interface Screen {
    data object Input : Screen
    data object Saved : Screen
    data object Qimen : Screen
    data object Liuren : Screen
    data object Taiyi : Screen
}

/** 应用根：在「八字排盘」「已存命盘」「奇门遁甲」「大六壬」间切换（状态式导航，零额外依赖） */
@Composable
fun BaziApp() {
    val context = LocalContext.current
    val storage = remember {
        ChartStorage(context.getSharedPreferences("bazi", Context.MODE_PRIVATE))
    }
    var screen by remember { mutableStateOf<Screen>(Screen.Input) }
    var pendingLoad by remember { mutableStateOf<SavedChart?>(null) }

    when (screen) {
        Screen.Input -> BaziPage(
            storage = storage,
            pendingLoad = pendingLoad,
            onPendingLoadConsumed = { pendingLoad = null },
            onOpenSaved = { screen = Screen.Saved },
            onOpenQimen = { screen = Screen.Qimen },
            onOpenLiuren = { screen = Screen.Liuren },
            onOpenTaiyi = { screen = Screen.Taiyi },
        )

        Screen.Saved -> SavedChartsPage(
            storage = storage,
            onClose = { screen = Screen.Input },
            onPick = { chart ->
                pendingLoad = chart
                screen = Screen.Input
            },
        )

        Screen.Qimen -> QimenPage(onBack = { screen = Screen.Input })
        Screen.Liuren -> LiurenPage(onBack = { screen = Screen.Input })
        Screen.Taiyi -> TaiyiPage(onBack = { screen = Screen.Input })
    }
}

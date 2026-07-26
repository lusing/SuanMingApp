package cn.xulun.bazi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SeedRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9D9),
    onPrimaryContainer = SeedRed,
    secondary = Color(0xFF8C6D4F),
    tertiary = Color(0xFFB07A3C),
    background = CreamBg,
    onBackground = Color(0xFF3A2A1F),
    surface = CreamSurface,
    onSurface = Color(0xFF3A2A1F),
    surfaceVariant = Color(0xFFF3E9D8),
    onSurfaceVariant = Color(0xFF6B5B4A),
    outline = Color(0xFFC9B79E),
    error = Color(0xFFB00020),
)

@Composable
fun BaziTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 固定深红主色：关闭 Material You 动态取色
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // 本应用统一使用浅色米底 + 朱砂主色，不随系统深色切换
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}

# 大六壬天地盘十二宫方盘 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在大六壬起课结果中新增上南下北的天地盘十二宫方盘，并用方盘替代原有十二天将标签列表。

**Architecture:** 保持 `LiuRenCalculator` 和 `LiuRenPlate` 不变，在新的 `LiurenHeavenPlate.kt` 中定义固定方位网格、纯映射函数及 Compose 视图。视图按地盘位调用 `LiuRenPlate.heavenAt()` 取得天盘支，并按 `TianJiangOnPlate.earthBranch` 查找天将；`LiurenPage` 只负责按既定顺序挂载组件。

**Tech Stack:** Kotlin 2.x、Android Jetpack Compose、Material 3、JUnit 4、Gradle Wrapper（Android SDK 位于 `G:/android`）

## Global Constraints

- 方盘固定上南下北，外围顺序为顶行“巳午未申”、左侧“辰卯”、右侧“酉戌”、底行“寅丑子亥”。
- 每宫依次显示地盘支、天盘支和天将；天盘支沿用五行色，吉将绿色、凶将红色。
- 中央显示月将专名及地支、占时和“上南下北”。
- 不增加横向滚动；窄屏不得文字截断、宫格重叠或横向溢出。
- 删除原“十二天将所临”标题、`LiurenTianJiang` 调用和已无引用的旧组件文件。
- 不修改月将、天盘旋转、天将布法、四课、三传、毕法赋、输入或导航算法。
- 当前项目根目录不是 Git 仓库；执行时不得初始化仓库。每个任务末尾保留条件式提交命令，仅当执行环境已恢复原 Git 元数据时运行，否则记录变更文件并跳过提交。

## File Structure

- Create: `app/src/main/java/cn/xulun/bazi/ui/components/LiurenHeavenPlate.kt`
  - 唯一职责：定义十二宫固定位置、将 `LiuRenPlate` 映射为每宫展示数据、绘制方盘。
- Modify: `app/src/test/java/cn/xulun/bazi/LiuRenCalculatorTest.kt`
  - 增加方盘方位完整性、普通课、伏吟和返吟映射测试；既有算法测试不变。
- Modify: `app/src/main/java/cn/xulun/bazi/ui/LiurenPage.kt`
  - 在课盘摘要与四课之间挂载新方盘，移除旧天将列表。
- Delete: `app/src/main/java/cn/xulun/bazi/ui/components/LiurenTianJiang.kt`
  - 方盘接管十二天将展示后，该组件不再有调用方。

---

### Task 1: 建立可测试的十二宫方位与映射

**Files:**
- Create: `app/src/main/java/cn/xulun/bazi/ui/components/LiurenHeavenPlate.kt`
- Modify: `app/src/test/java/cn/xulun/bazi/LiuRenCalculatorTest.kt:1-13,40-48`

**Interfaces:**
- Consumes: `LiuRenPlate.heavenAt(earthBranch: EarthlyBranch): EarthlyBranch`、`LiuRenPlate.tianJiang: List<TianJiangOnPlate>`、`TianJiangOnPlate.earthBranch`。
- Produces: `internal val LIUREN_HEAVEN_PLATE_GRID: List<List<EarthlyBranch?>>`、`internal data class LiurenHeavenPlateCell`、`internal fun liurenHeavenPlateCell(plate: LiuRenPlate, earthBranch: EarthlyBranch): LiurenHeavenPlateCell`，供本任务测试、Task 2 的边界测试和 Task 3 的 Compose 视图使用。

- [ ] **Step 1: 在现有六壬测试中写方位和普通课映射失败测试**

在 `LiuRenCalculatorTest.kt` 增加导入：

```kotlin
import cn.xulun.bazi.ui.components.LIUREN_HEAVEN_PLATE_GRID
import cn.xulun.bazi.ui.components.liurenHeavenPlateCell
```

在现有“天盘（月将加占时）”测试之后加入：

```kotlin
    // ── 天地盘方盘映射 ──
    @Test
    fun `天地盘方位 上南下北且十二地支无缺漏`() {
        assertEquals(
            listOf(
                listOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI, EarthlyBranch.SHEN),
                listOf(EarthlyBranch.CHEN, null, null, EarthlyBranch.YOU),
                listOf(EarthlyBranch.MAO, null, null, EarthlyBranch.XU),
                listOf(EarthlyBranch.YIN, EarthlyBranch.CHOU, EarthlyBranch.ZI, EarthlyBranch.HAI),
            ),
            LIUREN_HEAVEN_PLATE_GRID,
        )
        val branches = LIUREN_HEAVEN_PLATE_GRID.flatten().filterNotNull()
        assertEquals(12, branches.size)
        assertEquals(EarthlyBranch.entries.toSet(), branches.toSet())
    }

    @Test
    fun `天地盘宫位 按地盘支映射天盘支与天将`() {
        val p = LiuRenCalculator.compute(
            HeavenlyStem.JIA,
            EarthlyBranch.ZI,
            EarthlyBranch.WU,
            EarthlyBranch.CHOU,
        )

        val noon = liurenHeavenPlateCell(p, EarthlyBranch.WU)

        assertEquals(EarthlyBranch.WU, noon.earthBranch)
        assertEquals(EarthlyBranch.CHOU, noon.heavenBranch)
        assertEquals(TianJiang.GUI_REN, noon.tianJiang)
    }
```

- [ ] **Step 2: 运行定向测试并确认因新接口不存在而失败**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests "cn.xulun.bazi.LiuRenCalculatorTest"
```

Expected: Kotlin test compilation fails，错误包含 `Unresolved reference 'LIUREN_HEAVEN_PLATE_GRID'` 或 `Unresolved reference 'liurenHeavenPlateCell'`。

- [ ] **Step 3: 创建最小纯映射实现**

创建 `LiurenHeavenPlate.kt`，先写不含 Compose UI 的以下内容：

```kotlin
package cn.xulun.bazi.ui.components

import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.LiuRenPlate
import cn.xulun.bazi.model.TianJiang

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
```

使用 `single` 而不是 `firstOrNull`：十二地盘位一宫一将是排盘不变量，缺失或重复应在开发期直接失败，不能静默画错盘。

- [ ] **Step 4: 运行定向测试并确认通过**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests "cn.xulun.bazi.LiuRenCalculatorTest"
```

Expected: `BUILD SUCCESSFUL`，新增两个测试及原有 `LiuRenCalculatorTest` 全部通过。

- [ ] **Step 5: 条件式提交 Task 1**

若 `git status` 能正常返回仓库状态，则运行：

```bash
git add app/src/main/java/cn/xulun/bazi/ui/components/LiurenHeavenPlate.kt app/src/test/java/cn/xulun/bazi/LiuRenCalculatorTest.kt
git commit -m "test: define liuren heaven plate mapping

Co-Authored-By: Claude <noreply@anthropic.com>"
```

当前非 Git 环境下跳过命令，记录 Task 1 变更的两个文件。

---

### Task 2: 覆盖伏吟与返吟方盘不变量

**Files:**
- Modify: `app/src/test/java/cn/xulun/bazi/LiuRenCalculatorTest.kt`（紧接 Task 1 新增的方盘映射测试）

**Interfaces:**
- Consumes: Task 1 的 `LIUREN_HEAVEN_PLATE_GRID` 与 `liurenHeavenPlateCell()`。
- Produces: 对伏吟天地盘重合、返吟天地盘相冲的回归保护；不产生新的生产接口。

- [ ] **Step 1: 写伏吟和返吟映射测试**

在 Task 1 的方盘测试后加入：

```kotlin
    @Test
    fun `天地盘方盘 伏吟时天地支逐宫重合`() {
        val p = LiuRenCalculator.compute(
            HeavenlyStem.JIA,
            EarthlyBranch.ZI,
            EarthlyBranch.CHOU,
            EarthlyBranch.CHOU,
        )
        val branches = LIUREN_HEAVEN_PLATE_GRID.flatten().filterNotNull()

        assertTrue(branches.all { branch ->
            liurenHeavenPlateCell(p, branch).heavenBranch == branch
        })
    }

    @Test
    fun `天地盘方盘 返吟时天地支逐宫相冲`() {
        val p = LiuRenCalculator.compute(
            HeavenlyStem.JIA,
            EarthlyBranch.ZI,
            EarthlyBranch.WEI,
            EarthlyBranch.CHOU,
        )
        val branches = LIUREN_HEAVEN_PLATE_GRID.flatten().filterNotNull()

        assertTrue(branches.all { branch ->
            val expectedOpposite = EarthlyBranch.fromIndex((branch.ordinal + 6) % 12)
            liurenHeavenPlateCell(p, branch).heavenBranch == expectedOpposite
        })
    }
```

- [ ] **Step 2: 临时制造错误以验证测试能捕获旋转回归**

在 `liurenHeavenPlateCell()` 中临时把：

```kotlin
heavenBranch = plate.heavenAt(earthBranch),
```

改为：

```kotlin
heavenBranch = earthBranch,
```

- [ ] **Step 3: 运行定向测试并确认返吟测试失败**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests "cn.xulun.bazi.LiuRenCalculatorTest.天地盘方盘 返吟时天地支逐宫相冲"
```

Expected: FAIL，断言为 false。此步骤证明测试并非只复述固定布局。

- [ ] **Step 4: 恢复正确映射**

将实现恢复为：

```kotlin
heavenBranch = plate.heavenAt(earthBranch),
```

- [ ] **Step 5: 运行全部六壬计算器测试**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests "cn.xulun.bazi.LiuRenCalculatorTest"
```

Expected: `BUILD SUCCESSFUL`，伏吟与返吟新增测试均通过。

- [ ] **Step 6: 条件式提交 Task 2**

若项目已处于 Git 仓库，运行：

```bash
git add app/src/test/java/cn/xulun/bazi/LiuRenCalculatorTest.kt
git commit -m "test: cover liuren plate fu yin and fan yin

Co-Authored-By: Claude <noreply@anthropic.com>"
```

当前非 Git 环境下跳过命令并记录测试文件变更。

---

### Task 3: 绘制天地盘十二宫 Compose 方盘

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/ui/components/LiurenHeavenPlate.kt`

**Interfaces:**
- Consumes: Task 1 的 `LIUREN_HEAVEN_PLATE_GRID`、`liurenHeavenPlateCell()`，以及现有 `elementColor(Element): Color`、`CreamSurface`、`SeedRed`。
- Produces: `@Composable fun LiurenHeavenPlate(plate: LiuRenPlate, modifier: Modifier = Modifier)`，供 Task 4 的 `LiurenPage` 调用。

- [ ] **Step 1: 在纯映射代码下方加入公开方盘组件和私有宫格组件**

在 `LiurenHeavenPlate.kt` 补齐 Compose 导入并加入以下实现：

```kotlin
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
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor
```

```kotlin
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
```

保持生产文件只有一个 `package` 声明，并将全部 `import` 放在 `package` 与 `internal val` 之间；不要把上述导入原样追加到文件中部。

- [ ] **Step 2: 编译 Debug 变体以捕获 Compose API、导入和可见性错误**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`，无 unresolved import、`weight` 作用域或 Compose 类型错误。

- [ ] **Step 3: 运行六壬映射测试确认 UI 扩展未破坏纯逻辑**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests "cn.xulun.bazi.LiuRenCalculatorTest"
```

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 4: 条件式提交 Task 3**

若项目已处于 Git 仓库，运行：

```bash
git add app/src/main/java/cn/xulun/bazi/ui/components/LiurenHeavenPlate.kt
git commit -m "feat: draw liuren heaven plate grid

Co-Authored-By: Claude <noreply@anthropic.com>"
```

当前非 Git 环境下跳过命令并记录组件文件变更。

---

### Task 4: 集成方盘并移除旧十二天将列表

**Files:**
- Modify: `app/src/main/java/cn/xulun/bazi/ui/LiurenPage.kt:60-64,177-185`
- Delete: `app/src/main/java/cn/xulun/bazi/ui/components/LiurenTianJiang.kt`

**Interfaces:**
- Consumes: Task 3 的 `@Composable fun LiurenHeavenPlate(plate: LiuRenPlate, modifier: Modifier = Modifier)`。
- Produces: 起课结果顺序“课盘摘要 → 天地盘 → 四课 → 三传 → 毕法赋”，且生产代码中无 `LiurenTianJiang` 引用。

- [ ] **Step 1: 替换页面导入**

在 `LiurenPage.kt` 将：

```kotlin
import cn.xulun.bazi.ui.components.LiurenTianJiang
```

替换为：

```kotlin
import cn.xulun.bazi.ui.components.LiurenHeavenPlate
```

- [ ] **Step 2: 在课盘摘要后插入方盘并删除旧列表调用**

将 `plate?.let { p -> ... }` 内部替换为以下顺序：

```kotlin
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
```

这一步明确删除：

```kotlin
Text("十二天将所临", ...)
LiurenTianJiang(p)
```

- [ ] **Step 3: 搜索旧组件引用，确认只剩待删除定义**

使用专用内容搜索在 `app/src` 中查找：

```text
LiurenTianJiang
```

Expected: 只命中 `LiurenTianJiang.kt` 中的函数定义；不得再命中 `LiurenPage.kt` 或其他调用方。

- [ ] **Step 4: 删除旧组件文件**

删除：

```text
app/src/main/java/cn/xulun/bazi/ui/components/LiurenTianJiang.kt
```

不得删除 `TianJiang`、`TianJiangOnPlate` 或 `LiuRenPlate.tianJiang`，这些仍由新方盘和毕法赋使用。

- [ ] **Step 5: 编译并运行全部单元测试**

Run:

```bash
./gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`；无旧组件 unresolved reference；APK 正常生成。

- [ ] **Step 6: 条件式提交 Task 4**

若项目已处于 Git 仓库，运行：

```bash
git add app/src/main/java/cn/xulun/bazi/ui/LiurenPage.kt app/src/main/java/cn/xulun/bazi/ui/components/LiurenHeavenPlate.kt app/src/main/java/cn/xulun/bazi/ui/components/LiurenTianJiang.kt
git commit -m "feat: show liuren heaven plate on results

Co-Authored-By: Claude <noreply@anthropic.com>"
```

当前非 Git 环境下跳过命令，并记录页面修改与旧文件删除。

---

### Task 5: 真机验证普通、伏吟与返吟方盘

**Files:**
- Verify only: `app/build/outputs/apk/debug/app-debug.apk`
- Verify only: `app/src/main/java/cn/xulun/bazi/ui/components/LiurenHeavenPlate.kt`
- Modify only if verification exposes a layout defect: `app/src/main/java/cn/xulun/bazi/ui/components/LiurenHeavenPlate.kt`

**Interfaces:**
- Consumes: Task 4 生成的 Debug APK，以及现有 `LiurenPage` 日期/时辰输入流程。
- Produces: 真机验收证据：方位正确、数据对应、旧列表消失、窄屏无溢出，且普通课/伏吟/返吟都可读。

- [ ] **Step 1: 确认真机在线并安装 Debug APK**

Run:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected: `adb devices` 至少列出一个状态为 `device` 的序列号；安装返回 `Success`。

- [ ] **Step 2: 启动应用并进入大六壬页面**

Run:

```bash
adb shell am force-stop cn.xulun.bazi
adb shell monkey -p cn.xulun.bazi -c android.intent.category.LAUNCHER 1
```

在真机上点击“六壬”，保持页面纵向滚动到起课区域。若常规 `adb shell input tap` 在设备上无效，按项目既有真机驱动方式使用短距离 `adb shell input swipe x y x+1 y+1 100` 模拟点击，并用 `uiautomator dump` 获取控件坐标。

Expected: 顶栏显示“大六壬”，输入卡和“起课排盘”按钮可见。

- [ ] **Step 3: 验证普通课方盘和页面顺序**

选择一个月将不等于占时且不相冲的日期/时辰后点击“起课排盘”。记录截图：

```bash
adb exec-out screencap -p > build/liuren-heaven-plate-normal.png
```

逐项确认：

- 方盘位于课盘摘要之后、四课之前；
- 顶行是“巳午未申”，底行是“寅丑子亥”；
- 每宫均有灰色地盘支、五行色大号天盘支、吉绿凶红天将；
- 中央有月将、占时和“上南下北”；
- 页面不再出现“十二天将所临”标题或独立标签列表；
- 方盘没有横向滚动、重叠或文字截断。

- [ ] **Step 4: 验证伏吟课天地盘重合**

选择占时等于当前月将支的时辰并重新起课。记录截图：

```bash
adb exec-out screencap -p > build/liuren-heaven-plate-fuyin.png
```

Expected: 十二宫内每个大号天盘支都与同宫灰色地盘支相同；课名包含“伏吟”；四课、三传和毕法赋仍在方盘下方显示。

- [ ] **Step 5: 验证返吟课天地盘相冲**

选择与当前月将支相冲的占时并重新起课。记录截图：

```bash
adb exec-out screencap -p > build/liuren-heaven-plate-fanyin.png
```

Expected: 每宫大号天盘支与灰色地盘支相冲，例如子午、丑未、寅申；课名包含“返吟”；天将仍每宫一个。

- [ ] **Step 6: 若真机出现窄屏问题，只调整表现参数并回归验证**

允许的最小调整范围仅为 `LiurenHeavenPlate.kt` 中：

```kotlin
fontSize
padding
Arrangement.spacedBy
fillMaxWidth(0.46f)
```

不得为了适配屏幕改变方位数据、`heavenAt()` 映射或天将落宫查找。调整后重新运行：

```bash
./gradlew.bat testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected: 构建和全部单元测试通过，重新检查三种盘面均无溢出。

- [ ] **Step 7: 最终验证全套测试和旧引用清理**

Run:

```bash
./gradlew.bat testDebugUnitTest assembleDebug
```

并搜索 `app/src`：

```text
LiurenTianJiang
十二天将所临
```

Expected: Gradle 返回 `BUILD SUCCESSFUL`；两个搜索词均无命中。

- [ ] **Step 8: 条件式提交真机适配调整**

仅当 Step 6 实际修改了代码且项目已处于 Git 仓库时运行：

```bash
git add app/src/main/java/cn/xulun/bazi/ui/components/LiurenHeavenPlate.kt
git commit -m "fix: fit liuren heaven plate on device

Co-Authored-By: Claude <noreply@anthropic.com>"
```

若未修改代码或当前仍非 Git 仓库，则跳过提交。

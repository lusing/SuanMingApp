# 太乙神数 · 十二分野图（P4-c）设计

> 日期：2026-07-26。范围：四神/天乙/地乙/直符落三堂（绛宫/明堂/玉堂）时缺乏空间显示的补全——按卷八「九宫分野(并序)」十二分野表绘图，常驻显示四神位置。
> 依据：Kanripo KR3g0047_008（卷八）「九宫分野(并序)：一宫冀州 二宫荆州 三宫青州 四宫徐州 五宫豫州 六宫雍州 七宫梁州 八宫兖州 九宫扬州 绛宫交州 明堂益州 玉堂幽州」。

## 1. 布局与时机（用户裁定 2026-07-26）

- **布局**：太乙九宫方阵（上南下北，`TaiyiPalace.gridOrder` = `[[9,2,7],[4,5,6],[3,8,1]]`）+ 第四行 `[绛宫·交, 明堂·益, 玉堂·幽]`。
- **时机**：常驻显示（非仅落三堂时）；四神/天乙/地乙/直符落宫时亦在对应宫格标记。
- 每格：首行标题「9宫·扬」式（宫号·州 / 堂名·州），下方竖排落格神名，吉绿凶红加粗（沿用格局区配色 `#2E7D32`/`#C62828`）。

## 2. 模型层（`model/Taiyi.kt` 新增 `TaiyiFenYe` object）

```kotlin
object TaiyiFenYe {
    val palaceZhou: Map<Int, String>      // 1冀 2荆 3青 4徐 5豫 6雍 7梁 8兖 9扬
    val tangZhou: Map<String, String>       // 绛宫→交 明堂→益 玉堂→幽
    val tangOrder: List<String>             // [绛宫, 明堂, 玉堂]（第四行）
    fun markers(plate: TaiyiPlate): Map<String, List<TaiyiJiXing>>
    // key: 宫号字符串("1".."9") 或堂名("绛宫"/"明堂"/"玉堂")；value: 落格的四神/天乙/地乙/直符
}
```

- `markers` 只收四神/天乙/地乙/直符四神（基星/五福/游神不入图）；palaceNo 非空 → 宫格，否则按 location 归三堂格。
- 多神同格：list 保序（按 jiXing 列表原序）。

## 3. UI（`ui/TaiyiPage.kt`）

- 新 `FenYeChart(p: TaiyiPlate)`：独立 `InputCard("十二分野图")`，渲染位置在 `JiXingSection(p)` 之后、`PatternSection(p)` 之前（`TaiyiPage` 的 plate 区块内）。
- 4 行 3 列：前三行 `TaiyiPalace.gridOrder`（格标题 `"${no}宫·${palaceZhou[no]}"`），第四行 `tangOrder`（格标题 `"${tang}·${tangZhou[tang]}"`）；格内神名竖排、按 `auspicious` 着色。
- `JiXingSection` 标题「卷四星宿」→「卷四基星 · 卷五符神」；底部注改为：「卷四基星起戌邦·五福独立积年·大游起七宫；四神/天乙/地乙/直符依卷五立成十二位环；三堂分野依卷八」。

## 4. 测试（`TaiyiModelTest` 增补）

- 分野映射：`palaceZhou` 1..9 全对、`tangZhou` 三堂全对、`tangOrder` 顺序。
- `markers` 归格（2024 年计盘）：四神→"6"、天乙→"明堂"、地乙→"2"、直符→"绛宫"；四神之外（君基等）不入图。
- 回归：全量测试无回归。

## 5. 文档同步

- `.taiyi-reference.md`：§1 分野行补三堂（绛交/明益/玉幽，出卷八）；§17 符神段末补一句「三堂分野坐标出卷八 九宫分野(并序)，UI 十二分野图按此绘」。
- memory `taiyi-feature`：P4-c 完成记录。

## 6. 不做（YAGNI）

- 真实地理地图/州域边界绘制——格阵图即可。
- 基星/五福/游神入分野图——用户指定仅四神；三基走十二支不入此表。
- Compose UI 测试——无既有设施，逻辑收口模型层单测。

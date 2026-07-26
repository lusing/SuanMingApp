package cn.xulun.bazi.data

import android.content.SharedPreferences
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.SavedChart
import org.json.JSONArray

/** 命盘本地存储（SharedPreferences + JSON 数组），对应参考应用 ChartStorage */
class ChartStorage(private val prefs: SharedPreferences) {

    private val key = "saved_charts_v1"

    /** 读取全部命盘（按保存时间倒序） */
    fun loadAll(): List<SavedChart> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        val arr = JSONArray(raw)
        val charts = (0 until arr.length()).map { arr.getJSONObject(it).toSavedChart() }
        return charts.sortedByDescending { it.savedAt }
    }

    /** 保存一个命盘（id 相同则覆盖，插到队首） */
    fun save(chart: SavedChart) {
        val charts = loadAll().toMutableList()
        charts.removeAll { it.id == chart.id }
        charts.add(0, chart)
        writeAll(charts)
    }

    /** 删除指定命盘 */
    fun delete(id: String) {
        val charts = loadAll().filterNot { it.id == id }
        writeAll(charts)
    }

    private fun writeAll(charts: List<SavedChart>) {
        val arr = JSONArray()
        charts.forEach { arr.put(it.toJsonObject()) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    companion object {
        private fun org.json.JSONObject.toSavedChart(): SavedChart = SavedChart(
            id = getString("id"),
            name = getString("name"),
            year = getInt("year"),
            month = getInt("month"),
            day = getInt("day"),
            hour = getInt("hour"),
            gender = Gender.valueOf(getString("gender")),
            savedAt = getLong("savedAt"),
        )

        private fun SavedChart.toJsonObject(): org.json.JSONObject = org.json.JSONObject().apply {
            put("id", id)
            put("name", name)
            put("year", year)
            put("month", month)
            put("day", day)
            put("hour", hour)
            put("gender", gender.name)
            put("savedAt", savedAt)
        }
    }
}

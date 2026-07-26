#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
一次性生成 core/SolarTermTable.kt（1899-2100 节气查表）。

数据来源：F:/code/bazi/lib/core/solar_term_table.dart
（sxtwl 寿星天文历精密星历，北京时间 UTC+8，精度 1 分钟，数值打包为 MMDDHHMM）。

用法： python tool/gen_solar_term_table.py
输出： app/src/main/java/cn/xulun/bazi/core/SolarTermTable.kt
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(HERE)
SRC_DART = r"F:/code/bazi/lib/core/solar_term_table.dart"
OUT_KT = os.path.join(
    PROJECT_ROOT, "app", "src", "main", "java", "cn", "xulun", "bazi", "core",
    "SolarTermTable.kt",
)

ROW_RE = re.compile(r"(\d{4}):\s*\[([\d,\s]+)\]")


def main():
    if not os.path.isfile(SRC_DART):
        sys.exit("找不到源文件：%s" % SRC_DART)
    with open(SRC_DART, "r", encoding="utf-8") as f:
        text = f.read()

    rows = []  # [(year, [int, ...]), ...]
    for m in ROW_RE.finditer(text):
        year = int(m.group(1))
        nums = [int(x.strip()) for x in m.group(2).split(",") if x.strip()]
        if len(nums) != 24:
            sys.exit("年份 %d 的节气数 != 24：%d" % (year, len(nums)))
        rows.append((year, nums))

    if not rows:
        sys.exit("未解析到任何节气行")
    rows.sort(key=lambda r: r[0])

    lines = []
    lines.append("// GENERATED FILE — 由 tool/gen_solar_term_table.py 转换自")
    lines.append("// F:/code/bazi/lib/core/solar_term_table.dart，请勿手改。")
    lines.append("// 数据来源: sxtwl 寿星天文历（精密星历），北京时间 UTC+8，精度 1 分钟")
    lines.append("// 数值打包: MMDDHHMM（如 2041627 = 2月4日16:27）")
    lines.append("// 每年 24 项，顺序: 立春(0) 雨水 惊蛰 春分 清明 谷雨 立夏 小满")
    lines.append("//   芒种 夏至 小暑 大暑 立秋 处暑 白露 秋分 寒露 霜降 立冬 小雪")
    lines.append("//   大雪 冬至 小寒(22, 次年1月) 大寒(23, 次年1月)")
    lines.append("@file:Suppress(\"unused\")")
    lines.append("")
    lines.append("package cn.xulun.bazi.core")
    lines.append("")
    lines.append("/** 节气精确时刻查表（1899-2100），每年 24 项 MMDDHHMM 打包整数 */")
    lines.append("val kSolarTermTable: Map<Int, IntArray> = buildMap {")
    for year, nums in rows:
        lines.append("    put(%d, intArrayOf(%s))" % (year, ", ".join(str(n) for n in nums)))
    lines.append("}")
    lines.append("")

    os.makedirs(os.path.dirname(OUT_KT), exist_ok=True)
    with open(OUT_KT, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print("生成完成：%d 年 → %s" % (len(rows), OUT_KT))


if __name__ == "__main__":
    main()

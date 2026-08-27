package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.LiuQinResult

/**
 * 六亲分析入口（容器）。各亲属块独立计算，后续章节在此聚合。
 * 喜忌统一由 [LiuQinXiJi] 按教材口径派生。
 */
object LiuQinCalculator {

    fun analyze(bazi: BaziResult, gender: Gender): LiuQinResult =
        LiuQinResult(
            zuYe = ZuYeCalculator.analyze(bazi),
            waiGong = WaiGongCalculator.analyze(bazi),
            fuMu = FuMuCalculator.analyze(bazi, gender),
            xiongDi = XiongDiCalculator.analyze(bazi, gender),
            ziNv = ZiNvCalculator.analyze(bazi, gender),
        )
}

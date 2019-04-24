package com.zhiyi.videotrimmerlibrary.vo

import com.zhiyi.videotrimmerlibrary.DefaultConfig
import com.zhiyi.videotrimmerlibrary.callback.IConfig

class ConfigVo {

    private var iConfig: IConfig = DefaultConfig()

    var videoPath: String? = null
    var width: Int = 0
    var height: Int = 0
    var durationL: Long = 0L

    var thumbItemWidth = 0
    var thumbItemHeight = 0

    var isShowPosTextViews = iConfig.isShowTrimmerTextViews()

    var trimmerTimeL = iConfig.getTrimmerTime()
    var offsetValue = iConfig.getTrimmerOffsetValue()
    var visiableThumbCount = iConfig.getVisiableThumbCount()
    var adapterUpdateCount = iConfig.getThumbListUpdateCount()
    var minTrimmerThumbCount = iConfig.getMinTrimmerThumbCount()
    var seekBarShaowColor = iConfig.getTrimmerSeekBarShaowColor()
    var seekBarStrokeColor = iConfig.getTrimmerSeekBarTrimmerStrokeColor()
    var seekBarStrokeWidth = iConfig.getTrimmerSeekBarTrimmerStrokeWidth()
    var leftCursorBitmap = iConfig.getLeftCursorBitmap()
    var rightCursorBitmap = iConfig.getRightCursorBitmap()

    fun updateIConfig(icg: IConfig) {

        isShowPosTextViews = icg.isShowTrimmerTextViews()

        trimmerTimeL = icg.getTrimmerTime()
        visiableThumbCount = icg.getVisiableThumbCount()
        adapterUpdateCount = icg.getThumbListUpdateCount()
        seekBarShaowColor = icg.getTrimmerSeekBarShaowColor()
        minTrimmerThumbCount = icg.getMinTrimmerThumbCount()
        seekBarStrokeColor = icg.getTrimmerSeekBarTrimmerStrokeColor()
        seekBarStrokeWidth = icg.getTrimmerSeekBarTrimmerStrokeWidth()
        leftCursorBitmap = iConfig.getLeftCursorBitmap()
        rightCursorBitmap = iConfig.getRightCursorBitmap()
    }

    fun getVisiableEndPos(): Long = Math.min(trimmerTimeL, durationL)
}
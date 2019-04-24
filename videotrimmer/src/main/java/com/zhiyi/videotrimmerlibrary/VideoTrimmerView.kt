package com.zhiyi.videotrimmerlibrary

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import com.bumptech.glide.Glide
import com.zhiyi.videotrimmerlibrary.callback.IConfig
import com.zhiyi.videotrimmerlibrary.controls.RecyclerViewControl
import com.zhiyi.videotrimmerlibrary.controls.RegulatorControl
import com.zhiyi.videotrimmerlibrary.utils.DensityUtils
import com.zhiyi.videotrimmerlibrary.utils.UriUtils
import com.zhiyi.videotrimmerlibrary.vo.ConfigVo
import kotlinx.android.synthetic.main.video_trimmer_view.view.*

class VideoTrimmerView : FrameLayout {


    private var mLayoutId = R.layout.video_trimmer_view

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(mLayoutId, this)
        getRecyclerView().layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        initialControlIcon()
        RegulatorControl.getInstance(this)
    }

    private fun initialControlIcon() {
//        rootView.videoViewWraper.setOnClickListener {
//            val visibility: Int = when (rootView.control.visibility) {
//                View.VISIBLE -> View.INVISIBLE
//                View.INVISIBLE -> View.VISIBLE
//                else -> View.INVISIBLE
//            }
//            rootView.control.visibility = visibility
//            if (rootView.videoView.isPlaying) videoView.pause() else videoView.start()
//        }
        setControlIcon(R.drawable.video_play_icon)
    }

    fun setControlIcon(resId: Int) {
        Glide.with(context).load(resId).into(rootView.control)
    }

    @SuppressLint("WrongViewCast")
    fun getVideoView(): VideoView = findViewById(R.id.videoView)

    @SuppressLint("WrongViewCast")
    fun getSeekBar(): SeekBar = findViewById(R.id.seekBar)

    @SuppressLint("WrongViewCast")
    fun getRecyclerView(): RecyclerView = findViewById(R.id.recyclerView)

    @SuppressLint("WrongViewCast")
    fun getTrimmerSeekBar(): TrimmerSeekBar = findViewById(R.id.trimmerSeekBar)

    @SuppressLint("WrongViewCast")
    fun getLeftPosTextView(): TextView = findViewById(R.id.leftPos)

    @SuppressLint("WrongViewCast")
    fun getRightPosTextView(): TextView = findViewById(R.id.rightPos)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        RegulatorControl.getInstance(this)
                .initialThumbItemWH(arrayOf(MeasureSpec.getSize(widthMeasureSpec) - DensityUtils.dip2px(context, 25f * 2), recyclerView.measuredHeight))
    }

    fun setVideoPath(videoPath: String): VideoTrimmerView {
        RegulatorControl.getInstance().setVideoPath(videoPath)
        return this
    }

    fun getConfigVo(): ConfigVo = RegulatorControl.getInstance().getConfigVo()

    fun setVideoPath(videoPath: Uri): VideoTrimmerView {
        RegulatorControl.getInstance().setVideoPath(UriUtils.getPath(context, videoPath)!!)
        return this
    }

    fun setIConfig(icg: IConfig?) {
        if (icg == null) {
            return
        }
        RegulatorControl.getInstance().setIConfig(icg)
    }

    fun handle() {
        RegulatorControl.getInstance().handle()
    }

    fun release() {
        RegulatorControl.getInstance().release()
    }

    fun getTrimmerPos(): LongArray = RegulatorControl.getInstance().getTrimmerPos()

    fun setOnScelcetedContentChangedListener(l: RegulatorControl.OnScelcetedContentChangedListener) {
        RegulatorControl.getInstance(this).setOnScelcetedContentChangedListener(l)
    }

    fun setOnBottomVideoThumbCompletedListener(l: RecyclerViewControl.OnBottomVideoThumbLoadCompletedListener) {
        RecyclerViewControl.getInstance().setOnBottomVideoThumbLoadCompletedListener(l)
    }
}
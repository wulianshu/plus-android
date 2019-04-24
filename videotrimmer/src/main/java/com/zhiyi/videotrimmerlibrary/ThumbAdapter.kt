package com.zhiyi.videotrimmerlibrary

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.zhiyi.videotrimmerlibrary.vo.ThumbVo

class ThumbAdapter : RecyclerView.Adapter<ThumbAdapter.ThumViewHolder>() {

    var mDatas: ArrayList<ThumbVo> = arrayListOf()

    fun addDatas(datas: ArrayList<ThumbVo>) {
        val positionStart = mDatas.size
        mDatas.addAll(datas)
        notifyItemRangeInserted(positionStart, datas.size)
    }

    override fun getItemCount(): Int = mDatas.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumViewHolder {
        val rootView = LayoutInflater.from(parent.context).inflate(R.layout.thumb_item, parent, false)
        rootView.findViewById<ImageView>(R.id.mImageView).layoutParams.width = MediaHandleManager.getInstance().getConfigVo().thumbItemWidth
        rootView.findViewById<ImageView>(R.id.mImageView).layoutParams.height = MediaHandleManager.getInstance().getConfigVo().thumbItemHeight
        return ThumViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: ThumViewHolder, position: Int) {
        holder.render(mDatas[position].bitmapByte)
    }

    inner class ThumViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        fun render(bitmapByte: ByteArray) {
            Glide.with(itemView.context).load(bitmapByte)
                    .centerCrop()
                    .into(itemView.findViewById<ImageView>(R.id.mImageView))
        }
    }
}

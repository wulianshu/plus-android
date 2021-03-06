package com.zhiyicx.thinksnsplus.modules.circle.detailv2.adapter;

import android.content.Context;

import com.zhiyicx.thinksnsplus.R;
import com.zhiyicx.thinksnsplus.data.beans.CirclePostListBean;
import com.zhy.adapter.recyclerview.base.ViewHolder;

/**
 * @author Jliuer
 * @Date 2017/11/30/14:41
 * @Email Jliuer@aliyun.com
 * @Description
 */
public class CirclePostListItemForThreeImage extends CirclePostListBaseItem {

    private static final int IMAGE_COUNTS = 3;// 动态列表图片数量
    private static final int CURREN_CLOUMS = 3;// 当前列数
    
    public CirclePostListItemForThreeImage(Context context) {
        super(context);
    }

    @Override
    public int getItemViewLayoutId() {
        return R.layout.item_dynamic_list_three_image;
    }

    @Override
    protected int getCurrenCloums() {
        return CURREN_CLOUMS;
    }

    @Override
    public int getImageCounts() {
        return IMAGE_COUNTS;
    }

    @Override
    public void convert(ViewHolder holder, CirclePostListBean circlePostListBean, CirclePostListBean lastT, int position, int itemCounts) {
        super.convert(holder, circlePostListBean, lastT, position, itemCounts);
        initImageView(holder, holder.getView(R.id.siv_0), circlePostListBean, 0,1); // 数字 0 代表 image 当前的位置， 1 代表他相对与 CURREN_CLOUMS 的份数
        initImageView(holder, holder.getView(R.id.siv_1), circlePostListBean, 1,1);
        initImageView(holder, holder.getView(R.id.siv_2), circlePostListBean, 2,1);
    }
}

package com.zhiyicx.thinksnsplus.widget.coordinatorlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


/**
 * @author Jliuer
 * @Date 17/11/20 12:00
 * @Email Jliuer@aliyun.com
 * @Description 图片放大回弹
 * 个人信息布局的top和botoom跟随图片位移
 * toolbar背景变色
 */
public class AppBarLayoutOverScrollViewBehavior extends AppBarLayout.Behavior {
    private static final String TAG = "overScroll";
    private static final String TAG_TOOLBAR = "toolbar";
    private static final String TAG_MIDDLE = "middle";
    private static final float TARGET_HEIGHT = 2300;
    private View mTargetView;
    private int mParentHeight;
    private int mTargetViewHeight;
    private float mTotalDy;
    private float mLastScale;
    private int mLastBottom;
    private boolean isAnimate;
    private Toolbar mToolBar;
    private ViewGroup middleLayout;
    private int mMiddleHeight;
    private int mMiddleWidth;
    private int mStopHeight;
    private int mFirstTop;
    private int mSeccondTop;
    private boolean isRecovering = false;//是否正在自动回弹中
    private boolean isRefreshing = false;
    private boolean isDoRefresh = false;

    private final float MAX_REFRESH_LIMIT = 0.3f;//达到这个下拉临界值就开始刷新动画

    public AppBarLayoutOverScrollViewBehavior() {
    }

    public AppBarLayoutOverScrollViewBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, AppBarLayout abl, int layoutDirection) {
        boolean handled = super.onLayoutChild(parent, abl, layoutDirection);

        if (mToolBar == null) {
            mToolBar = (Toolbar) parent.findViewWithTag(TAG_TOOLBAR);
        }
        if (middleLayout == null) {
            middleLayout = (ViewGroup) parent.findViewWithTag(TAG_MIDDLE);
        }
        // 需要在调用过super.onLayoutChild()方法之后获取
        if (mTargetView == null) {
            mTargetView = parent.findViewWithTag(TAG);
            if (mTargetView != null) {
                initial(abl);
            }
        }
        parent.setOnTouchListener(new View.OnTouchListener() {
            float y = 0;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        y = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (y == 0) {
                            return false;
                        }
                        float distance = event.getY() - y;
                        int canAcessDistance = (int) distance / 2;
                        if (distance > 0 && middleLayout.getTop() == 0) {
                            // 下拉
                            scale(abl, null, -canAcessDistance);
                        } else {
                            int bottom = abl.getBottom();
                            if (bottom <= mParentHeight) {
                                return false;
                            } else {
                                scale(abl, null, -canAcessDistance);
                            }
                        }
                        y = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        int bottom = abl.getBottom();
                        if (bottom > mParentHeight) {
                            recovery(abl);
                        }

                        break;
                    default:
                }

                return true;
            }
        });
        abl.addOnOffsetChangedListener((appBarLayout, i) -> {
                    float point = (float) Math.abs(i) / (float) appBarLayout.getTotalScrollRange();
                    middleLayout.setAlpha(1f - point);
                    if (onRefreshChangeListener != null) {
                        onRefreshChangeListener.alphaChange(point);
                    }
                }
        );
        return handled;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dx, int dy, int[] consumed, int type) {
        if (!isRecovering && type == ViewCompat.TYPE_TOUCH) {
            if (mTargetView != null && ((dy < 0 && child.getBottom() >= mParentHeight)
                    || (dy > 0 && child.getBottom() > mParentHeight))) {
                if (dy > 0) {
                    consumed[1] = dy;
                }
                scale(child, target, dy);
                return;
            }
        }
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes, int type) {
        isAnimate = true;
        return super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type);
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout abl, View target, int type) {
        recovery(abl);
        super.onStopNestedScroll(coordinatorLayout, abl, target, type);
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, float velocityX, float velocityY) {
        if (velocityY > 100) {//当y速度>100,就秒弹回
            isAnimate = false;
        }
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }

    public void initial(AppBarLayout abl) {
        if (abl == null) {
            return;
        }
        abl.setClipChildren(false);
        mParentHeight = abl.getHeight();
        mTargetViewHeight = mTargetView.getHeight();
        mMiddleHeight = middleLayout.getHeight();
        mMiddleWidth = middleLayout.getWidth();
        mFirstTop = middleLayout.getTop();
        setRefreshing(false);
    }

    private void scale(AppBarLayout abl, View target, int dy) {
        if (isRecovering || isDoRefresh) {
            return;
        }
        mTotalDy += -dy;
        mTotalDy = Math.min(mTotalDy, TARGET_HEIGHT);
        mLastScale = Math.max(1f, 1f + mTotalDy / TARGET_HEIGHT);
        ViewCompat.setScaleX(mTargetView, mLastScale);
        ViewCompat.setScaleY(mTargetView, mLastScale);
        mLastBottom = mParentHeight + (int) (mTargetViewHeight / 2 * (mLastScale - 1));
        abl.setBottom(mLastBottom);
        // RecyclerView does not support scrolling to an absolute position. Use scrollToPosition instead
//         target . setScrollY(0);
        middleLayout.setTranslationY(mLastBottom - mParentHeight);


        if (onRefreshChangeListener != null) {
            //计算0~1的进度
            float progress = Math.min((mLastScale - 1) / MAX_REFRESH_LIMIT, 1);
            if (progress >= 0.6 && !isRefreshing && progress < 0.7 && !isRecovering) {
                isRefreshing = true;
                onRefreshChangeListener.onRefreshShow();
            }
        }

    }

    public interface onRefreshChangeListener {
        void onRefreshShow();

        void doRefresh();

        void alphaChange(float a);
    }

    public void setOnRefreshChangeListener(onRefreshChangeListener onRefreshChangeListener) {
        this.onRefreshChangeListener = onRefreshChangeListener;
    }

    onRefreshChangeListener onRefreshChangeListener;

    private void recovery(final AppBarLayout abl) {
        if (isRecovering) {
            return;
        }

        if (onRefreshChangeListener != null && isRefreshing) {
            isDoRefresh = true;
            onRefreshChangeListener.doRefresh();
        }
        if (mTotalDy > 0) {
            isRecovering = true;
            mTotalDy = 0;
            if (isAnimate) {
                ValueAnimator anim = ValueAnimator.ofFloat(mLastScale, 1f).setDuration(200);
                anim.addUpdateListener(
                        animation -> {
                            float value = (float) animation.getAnimatedValue();
                            ViewCompat.setScaleX(mTargetView, value);
                            ViewCompat.setScaleY(mTargetView, value);

                            int ablBottom = (int) (mLastBottom - (mLastBottom - mParentHeight) * animation.getAnimatedFraction());
                            abl.setBottom(ablBottom);
                            middleLayout.setTranslationY(ablBottom - mParentHeight);
                        }
                );
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isRecovering = false;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                anim.start();
            } else {
                ViewCompat.setScaleX(mTargetView, 1f);
                ViewCompat.setScaleY(mTargetView, 1f);
                abl.setBottom(mParentHeight);
                middleLayout.setTranslationY(0);
                isRecovering = false;
            }
        }
    }

    public void setRefreshing(boolean refreshing) {
        isRefreshing = refreshing;
        isDoRefresh = refreshing;
    }
}
package com.zhiyicx.thinksnsplus.modules.q_a.detail.question;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.trycatch.mysnackbar.Prompt;
import com.zhiyicx.baseproject.base.TSListFragment;
import com.zhiyicx.baseproject.config.ImageZipConfig;
import com.zhiyicx.baseproject.widget.DynamicDetailMenuView;
import com.zhiyicx.baseproject.widget.InputPasswordView;
import com.zhiyicx.baseproject.widget.popwindow.ActionPopupWindow;
import com.zhiyicx.baseproject.widget.popwindow.CenterAlertPopWindow;
import com.zhiyicx.baseproject.widget.popwindow.PayPopWindow;
import com.zhiyicx.common.config.MarkdownConfig;
import com.zhiyicx.common.utils.RegexUtils;
import com.zhiyicx.common.utils.TextViewUtils;
import com.zhiyicx.common.widget.popwindow.CustomPopupWindow;
import com.zhiyicx.thinksnsplus.R;
import com.zhiyicx.thinksnsplus.base.AppApplication;
import com.zhiyicx.thinksnsplus.base.BaseWebLoad;
import com.zhiyicx.thinksnsplus.data.beans.AnswerInfoBean;
import com.zhiyicx.thinksnsplus.data.beans.QAPublishBean;
import com.zhiyicx.thinksnsplus.data.beans.UserInfoBean;
import com.zhiyicx.thinksnsplus.data.beans.qa.QAListInfoBean;
import com.zhiyicx.thinksnsplus.data.beans.report.ReportResourceBean;
import com.zhiyicx.thinksnsplus.modules.password.findpassword.FindPasswordActivity;
import com.zhiyicx.thinksnsplus.modules.q_a.answer.EditeAnswerDetailFragment;
import com.zhiyicx.thinksnsplus.modules.q_a.answer.PublishType;
import com.zhiyicx.thinksnsplus.modules.q_a.detail.adapter.AnswerEmptyItem;
import com.zhiyicx.thinksnsplus.modules.q_a.detail.adapter.AnswerListItem;
import com.zhiyicx.thinksnsplus.modules.q_a.detail.adapter.AnswerListItem.OnGoToWatchClickListener;
import com.zhiyicx.thinksnsplus.modules.q_a.detail.answer.AnswerDetailsActivity;
import com.zhiyicx.thinksnsplus.modules.q_a.detail.question.comment.QuestionCommentActivity;
import com.zhiyicx.thinksnsplus.modules.q_a.publish.question.PublishQuestionActivity;
import com.zhiyicx.thinksnsplus.modules.q_a.reward.QARewardActivity;
import com.zhiyicx.thinksnsplus.modules.report.ReportActivity;
import com.zhiyicx.thinksnsplus.modules.report.ReportType;
import com.zhiyicx.thinksnsplus.utils.ImageUtils;
import com.zhiyicx.thinksnsplus.utils.TSUerPerMissonUtil;
import com.zhiyicx.thinksnsplus.widget.QuestionSelectListTypePopWindow;
import com.zhy.adapter.recyclerview.MultiItemTypeAdapter;
import com.zhy.adapter.recyclerview.MultiItemTypeAdapter.OnItemClickListener;
import com.zhy.adapter.recyclerview.wrapper.HeaderAndFooterWrapper;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;

import static android.app.Activity.RESULT_OK;
import static com.zhiyicx.baseproject.widget.popwindow.ActionPopupWindow.POPUPWINDOW_ALPHA;
import static com.zhiyicx.common.config.ConstantConfig.JITTER_SPACING_TIME;
import static com.zhiyicx.thinksnsplus.modules.q_a.detail.answer.AnswerDetailsFragment.BUNDLE_ANSWER;
import static com.zhiyicx.thinksnsplus.modules.q_a.detail.answer.AnswerDetailsFragment.BUNDLE_SOURCE_ID;
import static com.zhiyicx.thinksnsplus.modules.q_a.detail.question.QuestionDetailActivity.BUNDLE_QUESTION_BEAN;
import static com.zhiyicx.thinksnsplus.modules.q_a.publish.question.PublishQuestionFragment.BUNDLE_PUBLISHQA_BEAN;
import static com.zhiyicx.thinksnsplus.modules.q_a.reward.QARewardFragment.BUNDLE_QUESTION_ID;
import static com.zhiyicx.thinksnsplus.widget.QuestionSelectListTypePopWindow.Builder;
import static com.zhiyicx.thinksnsplus.widget.QuestionSelectListTypePopWindow.OnOrderTypeSelectListener;

/**
 * @author Catherine
 * @describe
 * @date 2017/8/15
 * @contact email:648129313@qq.com
 */

public class
QuestionDetailFragment extends TSListFragment<QuestionDetailContract.Presenter,
        AnswerInfoBean> implements QuestionDetailContract.View, QuestionDetailHeader.OnActionClickListener,
        OnOrderTypeSelectListener, OnItemClickListener, OnGoToWatchClickListener,
        TextViewUtils.OnSpanTextClickListener, BaseWebLoad.OnWebLoadListener {

    public static final int REWARD_CODE = 1;

    @BindView(R.id.tv_toolbar_left)
    TextView mTvToolbarLeft;
    @BindView(R.id.qa_detail_tool)
    DynamicDetailMenuView mQaDetailTool;
    @BindView(R.id.behavior_demo_coordinatorLayout)
    CoordinatorLayout mCoordinatorLayout;


    private QAListInfoBean mQaListInfoBean;
    private QuestionDetailHeader mQuestionDetailHeader;
    private String mCurrentOrderType;

    private QuestionSelectListTypePopWindow mOrderTypeSelectPop; // 选择排序的弹框
    private ActionPopupWindow mMorePop; // 更多弹框
    private PayPopWindow mPayImagePopWindow; // 申请精选
    private PayPopWindow mPayWatchPopWindow; // 围观答案
    private CenterAlertPopWindow mDeleteQuestionPopWindow; // 删除问题提示框
    private boolean mIsMine = false;
    private int mCurrentPosition;

    public static QuestionDetailFragment instance(Bundle bundle) {
        QuestionDetailFragment fragment = new QuestionDetailFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mQaListInfoBean = (QAListInfoBean) getArguments().getSerializable(BUNDLE_QUESTION_BEAN);
    }

    @Override
    protected void initView(View rootView) {
        super.initView(rootView);
        Long userId = mQaListInfoBean.getUser_id();
        if (userId != null) {
            mIsMine = AppApplication.getMyUserIdWithdefault() == userId;
        }
        initHeaderView();
        initBottomToolStyle();
        initBottomToolListener();
        initListener();
        initPopWindow();
        mCurrentOrderType = mQuestionDetailHeader.getCurrentOrderType();
    }

    @Override
    public void onSureClick(View v, String text, InputPasswordView.PayNote payNote) {

        if (payNote.parent_id != null) {
            // 这里 parent_id 用来记录 mCurrentPosition,用户围观答案
            mPresenter.payForOnlook(payNote.id, payNote.parent_id.intValue(), payNote.psd);
        } else {
            mPresenter.applyForExcellent(payNote.id, payNote.psd);
        }
    }

    @Override
    public void onForgetPsdClick() {
        showInputPsdView(false);
        startActivity(new Intent(getActivity(), FindPasswordActivity.class));
    }

    @Override
    public void onCancle() {
        dismissSnackBar();
        mPresenter.canclePay();
        showInputPsdView(false);
    }

    @Override
    protected boolean setUseInputPsdView() {
        return true;
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        MultiItemTypeAdapter multiItemTypeAdapter = new MultiItemTypeAdapter<>(getActivity(),
                mListDatas);
        AnswerListItem answerListItem = new AnswerListItem(mPresenter, getActivity());
        answerListItem.setOnGoToWatchClickListener(this);
        answerListItem.setOnSpanTextClickListener(this);
        multiItemTypeAdapter.addItemViewDelegate(answerListItem);
        multiItemTypeAdapter.addItemViewDelegate(new AnswerEmptyItem());
        multiItemTypeAdapter.setOnItemClickListener(this);
        return multiItemTypeAdapter;
    }

    @Override
    public void onItemClick(View view, RecyclerView.ViewHolder holder, int position) {
        mCurrentPosition = position - mHeaderAndFooterWrapper
                .getHeadersCount();
        AnswerInfoBean answerInfoBean = mListDatas.get(mCurrentPosition);

        boolean canNotLook = TextUtils.isEmpty(answerInfoBean.getBody());

        if (answerInfoBean.getInvited() != 1 && canNotLook) {
            return;
        }

        // 开启了围观并且不是作者本人点击
        if (canNotLook) {
            mPayWatchPopWindow.show();
        } else {
            startToAnswerDetail(answerInfoBean);
        }

    }

    @Override
    public void refreshData(int index) {
        super.refreshData(index);
        try {
            mQuestionDetailHeader.setDetail(getCurrentQuestion(), mPresenter.getSystemConfig().getQuestionConfig().getOnlookers_amount(),
                    mPresenter.getRatio());
        } catch (Exception e) {
        }
    }

    @Override
    public void setSpanText(int position, int note, long amount, TextView view, boolean canNotRead) {
        onToWatchClick(null, position, canNotRead);
    }

    private void startToAnswerDetail(AnswerInfoBean answerInfoBean) {
        if (answerInfoBean.getId() == null) {
            return;
        }
        Intent intent = new Intent(getActivity(), AnswerDetailsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putLong(BUNDLE_SOURCE_ID, answerInfoBean.getId());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(View view, RecyclerView.ViewHolder holder, int position) {
        return false;
    }

    @Override
    protected int getBodyLayoutId() {
        return R.layout.fragment_qusetion_detail;
    }

    @Override
    protected boolean showToolBarDivider() {
        return false;
    }

    @Override
    protected int getstatusbarAndToolbarHeight() {
        return getResources().getDimensionPixelOffset(com.zhiyicx.baseproject.R.dimen.toolbar_height) + getResources().getDimensionPixelOffset(com
                .zhiyicx.baseproject.R.dimen.divider_line);
    }

    @Override
    protected boolean showToolbar() {
        return false;
    }

    @Override
    protected boolean setUseCenterLoading() {
        return true;
    }

    @Override
    public void setQuestionDetail(QAListInfoBean questionDetail, boolean isLoadMore) {
        this.mQaListInfoBean = questionDetail;

        // 加入这两句，因为目前 问题详情不支持 id 跳转  2018-1-30 15:19:54 by tym
        mIsMine = questionDetail.getUser_id().equals(AppApplication.getMyUserIdWithdefault());
        if (mQaDetailTool != null) {
            mQaDetailTool.showQuestionTool(mIsMine);
        }


        onNetResponseSuccess(mQaListInfoBean.getAnswerInfoBeanList(), isLoadMore);
        mQuestionDetailHeader.setDetail(questionDetail, mPresenter.getSystemConfig().getQuestionConfig().getOnlookers_amount(), mPresenter.getRatio
                ());
    }

    @Override
    public QAListInfoBean getCurrentQuestion() {
        return mQaListInfoBean;
    }

    @Override
    public String getCurrentOrderType() {
        return mCurrentOrderType;
    }

    @Override
    protected Long getMaxId(@NotNull List<AnswerInfoBean> data) {
        long size = mListDatas.size();
        if (mQaListInfoBean != null) {
            if (mQaListInfoBean.getInvitations() != null) {
                size -= mQaListInfoBean.getInvitations().size();
            }
            if (mQaListInfoBean.getAdoption_answers() != null) {
                size -= mQaListInfoBean.getAdoption_answers().size();
            }
        }
        return size;
    }

    @Override
    public void updateFollowState() {
        mQuestionDetailHeader.updateFollowState(mQaListInfoBean, mPresenter.getRatio());
    }

    @Override
    public void updateAnswerCount() {
        mQuestionDetailHeader.updateAnswerView(mQaListInfoBean);
        mQuestionDetailHeader.updateIsAddedAnswerState(mQaListInfoBean);
    }

    @Override
    protected void snackViewDismissWhenTimeOut(Prompt prompt, String message) {
        super.snackViewDismissWhenTimeOut(prompt, message);
        if (getActivity() != null && Prompt.SUCCESS == prompt && getString(R.string.qa_question_delete_success).equals(message)) {
            deleteSuccess();
        }
    }

    @Override
    public void deleteSuccess() {
        mDeleteQuestionPopWindow.dismiss();
        mMorePop.dismiss();
        getActivity().finish();
    }

    @Override
    public void handleLoading(boolean isLoading, boolean success, String message) {
        if (isLoading) {
            showSnackLoadingMessage(message);
        } else {
            if (success) {
                if (TextUtils.isEmpty(message)) {
                    mDeleteQuestionPopWindow.dismiss();
                    mMorePop.dismiss();
                } else {
                    showSnackSuccessMessage(message);
                }
            } else {
                showSnackErrorMessage(message);
            }
        }
    }

    @Override
    public void onNetResponseSuccess(@NotNull List<AnswerInfoBean> data, boolean isLoadMore) {
        if (!isLoadMore) {
            if (data.isEmpty()) {
                AnswerInfoBean emptyData = new AnswerInfoBean();
                data.add(emptyData);
            }
        }
        super.onNetResponseSuccess(data, isLoadMore);
    }

    @Override
    public void onLoadFinish() {
        closeLoadingView();
    }

    @Override
    public void onResume() {
        mQuestionDetailHeader.getMarkdownView().onResume();
        mQuestionDetailHeader.getMarkdownView().resumeTimers();
        super.onResume();
    }

    @Override
    public void onPause() {
        mQuestionDetailHeader.getMarkdownView().onPause();
        mQuestionDetailHeader.getMarkdownView().pauseTimers();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mQuestionDetailHeader.destroyedWeb();
        dismissPop(mOrderTypeSelectPop);
        dismissPop(mDeleteQuestionPopWindow);
        dismissPop(mMorePop);
        dismissPop(mPayImagePopWindow);
        dismissPop(mPayWatchPopWindow);
        super.onDestroyView();
    }

    @Override
    public void onResponseError(Throwable throwable, boolean isLoadMore) {
        setLoadViewHolderImag(R.mipmap.img_default_internet);
        showLoadViewLoadError();
        hideRefreshState(isLoadMore);
    }

    @Override
    protected void setLoadingViewHolderClick() {
        super.setLoadingViewHolderClick();
        onEmptyViewClick();
    }

    @Override
    public void onFollowClick() {
        mPresenter.handleFollowState(mQaListInfoBean.getId() + "", !mQaListInfoBean.getWatched());
    }

    @Override
    public void onRewardTypeClick(List<UserInfoBean> invitations, int rewardType) {
        // 仅自己发布的可以跳转设置
        if (mQaListInfoBean.getUser_id().equals(AppApplication.getmCurrentLoginAuth().getUser_id())) {
            // 跳转设置悬赏
            Intent intent = new Intent(getActivity(), QARewardActivity.class);
            Bundle bundle = new Bundle();
            bundle.putLong(BUNDLE_QUESTION_ID, mQaListInfoBean.getId());
            intent.putExtras(bundle);
            startActivityForResult(intent, REWARD_CODE);
        }
    }

    @Override
    public void onAddAnswerClick(AnswerInfoBean answerInfoBean) {
        if (answerInfoBean != null) {
            // 点击跳转到 回答详情 查看自己的回答
            Intent intent = new Intent(getContext(), AnswerDetailsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(BUNDLE_ANSWER, answerInfoBean);
            bundle.putLong(BUNDLE_SOURCE_ID, answerInfoBean.getId());
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            // 跳转发布回答
            EditeAnswerDetailFragment.startQActivity(getActivity(), PublishType
                            .PUBLISH_ANSWER, mQaListInfoBean.getId()
                    , null, mQaListInfoBean.getSubject(), 0);
        }
    }

    @Override
    public void onChangeListOrderClick(String orderType) {
        // 弹出排序选择框
        mOrderTypeSelectPop.show();
    }

    private void initHeaderView() {
        mHeaderAndFooterWrapper = new HeaderAndFooterWrapper(mAdapter);
        mQuestionDetailHeader = new QuestionDetailHeader(getActivity(), null/*mPresenter
        .getAdvert()*/);
        mQuestionDetailHeader.setWebLoadListener(this);
        mQuestionDetailHeader.setOnActionClickListener(this);
        mHeaderAndFooterWrapper.addHeaderView(mQuestionDetailHeader.getQuestionHeaderView());
        View mFooterView = new View(getContext());
        mFooterView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams
                .MATCH_PARENT, 1));
        mHeaderAndFooterWrapper.addFootView(mFooterView);
        mRvList.setAdapter(mHeaderAndFooterWrapper);
        mHeaderAndFooterWrapper.notifyDataSetChanged();
    }

    private void initBottomToolStyle() {
        // 初始化底部工具栏数据
        mQaDetailTool.setImageNormalResourceIds(new int[]{R.mipmap.home_ico_good_normal
                , R.mipmap.home_ico_comment_normal, R.mipmap.detail_ico_share_normal
                , R.mipmap.home_ico_more, R.mipmap.detail_ico_edit_normal, R.mipmap
                .detail_ico_good_uncollect
        });
        mQaDetailTool.setImageCheckedResourceIds(new int[]{R.mipmap.home_ico_good_high
                , R.mipmap.home_ico_comment_normal, R.mipmap.detail_ico_share_normal
                , R.mipmap.home_ico_more, R.mipmap.detail_ico_edit_normal, R.mipmap
                .detail_ico_collect
        });
        mQaDetailTool.setButtonText(new int[]{R.string.dynamic_like, R.string.comment
                , R.string.share, R.string.more, R.string.qa_detail_edit, R.string.collect});
        mQaDetailTool.showQuestionTool(mIsMine);
        mQaDetailTool.setData();
    }

    private void initBottomToolListener() {
        mQaDetailTool.setItemOnClick((parent, v, position) -> {
            mQaDetailTool.getTag(R.id.view_data);
            switch (position) {
                case DynamicDetailMenuView.ITEM_POSITION_1:// 跳转评论页
                    Intent intent = new Intent(getActivity(), QuestionCommentActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(BUNDLE_QUESTION_BEAN, mQaListInfoBean);
                    intent.putExtra(BUNDLE_QUESTION_BEAN, bundle);
                    startActivity(intent);
                    break;
                case DynamicDetailMenuView.ITEM_POSITION_2:
                    // 分享
                    mPresenter.shareQuestion(mQuestionDetailHeader.getShareBitmap());
                    break;
                case DynamicDetailMenuView.ITEM_POSITION_4:
                    // 编辑
                    QAPublishBean qaPublishBean = QAPublishBean.qaListInfo2QAPublishBean(mQaListInfoBean);
                    mPresenter.saveQuestion(qaPublishBean);

                    Intent publishQaIntent = new Intent(getActivity(), PublishQuestionActivity.class);
                    Bundle publishQaBundle = new Bundle();

                    publishQaBundle.putParcelable(BUNDLE_PUBLISHQA_BEAN, qaPublishBean);
                    publishQaIntent.putExtras(publishQaBundle);

                    startActivity(publishQaIntent);
                    break;
                case DynamicDetailMenuView.ITEM_POSITION_5:// 收藏
                    // 非发布者
                    break;
                case DynamicDetailMenuView.ITEM_POSITION_3:// 更多
                    mMorePop.show();
                    break;
                default:
                    break;
            }
        });
    }

    private void initListener() {
        mCoordinatorLayout.setEnabled(false);
        RxView.clicks(mTvToolbarLeft)
                .throttleFirst(JITTER_SPACING_TIME, TimeUnit.SECONDS)
                .subscribe(aVoid -> getActivity().finish());
    }

    @Override
    protected boolean setUseShadowView() {
        return true;
    }

    @Override
    protected void onShadowViewClick() {
        showInputPsdView(false);
    }

    private void initPopWindow() {
        if (mOrderTypeSelectPop == null) {
            mOrderTypeSelectPop = Builder()
                    .with(getActivity())
                    .parentView(mQuestionDetailHeader.getQuestionHeaderView())
                    .alpha(1f)
                    .setListener(this)
                    .build();
        }
        if (mMorePop == null) {
            mMorePop = ActionPopupWindow.builder()
                    .with(getActivity())
                    .isOutsideTouch(true)
                    .isFocus(true)
                    .backgroundAlpha(POPUPWINDOW_ALPHA)
                    .item1Str(mIsMine ? getString(R.string.qa_apply_for_excellent) : getString(R
                            .string.empty))
                    .item2Str((mIsMine || TSUerPerMissonUtil.getInstance().canDeleteQuestion()) ? getString(R.string.qa_question_delete) : "")
                    .item3Str(mIsMine || TSUerPerMissonUtil.getInstance().canDeleteQuestion() ? "" : getString(R.string.report))
                    .item2Color(ContextCompat.getColor(getContext(), R.color.important_for_note))
                    .bottomStr(getString(R.string.cancel))
                    .item1ClickListener(() -> {
                        mMorePop.hide();
                        if (mIsMine) {
                            if (mQaListInfoBean.getExcellent() != 1) {
                                // 申请精选问答
                                mPayImagePopWindow.show();
                            } else {
                                showSnackErrorMessage(getString(R.string
                                        .qa_question_excellent_reapply));
                            }
                        }
                    })
                    .item2ClickListener(() -> {
                        mMorePop.hide();
                        if (mDeleteQuestionPopWindow != null) {
                            mDeleteQuestionPopWindow.show();
                        }
                    })
                    .item3ClickListener(() -> {
                        // 举报帖子
                        String img = "";

                        int id = RegexUtils.getImageIdFromMarkDown(MarkdownConfig.IMAGE_FORMAT, mQaListInfoBean.getBody());
                        if (id > 0) {
                            img = ImageUtils.imagePathConvertV2(id, getResources()
                                            .getDimensionPixelOffset(R.dimen.report_resource_img), getResources()
                                            .getDimensionPixelOffset(R.dimen.report_resource_img),
                                    ImageZipConfig.IMAGE_80_ZIP);
                        }
                        String des = RegexUtils.replaceImageId(MarkdownConfig.IMAGE_FORMAT, mQaListInfoBean.getBody()); // 预览的文字
                        ReportActivity.startReportActivity(mActivity, new ReportResourceBean(mQaListInfoBean.getUser(), String.valueOf
                                (mQaListInfoBean.getId()),
                                mQaListInfoBean.getSubject(), img, des, ReportType.QA));
                        mMorePop.hide();
                    })
                    .bottomClickListener(() -> mMorePop.hide())
                    .build();
        }

        if (mPayImagePopWindow == null) {
            mPayImagePopWindow = PayPopWindow.builder()
                    .with(getActivity())
                    .isWrap(true)
                    .isFocus(true)
                    .isOutsideTouch(true)
                    .buildLinksColor1(R.color.themeColor)
                    .buildLinksColor2(R.color.important_for_content)
                    .contentView(R.layout.ppw_for_center)
                    .backgroundAlpha(POPUPWINDOW_ALPHA)
                    .buildDescrStr(String.format(getString(R.string.qa_pay_for_excellent_hint) + getString(R
                                    .string.buy_pay_member),
                            mPresenter != null && mPresenter.getSystemConfig().getQuestionConfig() != null ? mPresenter.getSystemConfig()
                                    .getQuestionConfig().getApply_amount() : ""
                            , mPresenter != null ? mPresenter.getGoldName() : ""))
                    .buildLinksStr(getString(R.string.qa_pay_for_excellent))
                    .buildTitleStr(getString(R.string.qa_pay_for_excellent))
                    .buildItem1Str(getString(R.string.buy_pay_in_payment))
                    .backgroundDrawable(new ColorDrawable(0x000000))
                    .buildItem2Str(getString(R.string.buy_pay_out))
                    .buildMoneyStr(getString(R.string.buy_pay_integration, "" + (mPresenter.getSystemConfig()
                            .getQuestionConfig() != null ? mPresenter.getSystemConfig()
                            .getQuestionConfig().getApply_amount() : 0)))
                    .buildCenterPopWindowItem1ClickListener(() -> {
                        if (mPresenter.usePayPassword()) {
                            mIlvPassword.setPayNote(new InputPasswordView.PayNote(null, mQaListInfoBean.getId()));
                            showInputPsdView(true);
                        } else {
                            mPresenter.applyForExcellent(mQaListInfoBean.getId(), null);
                        }
                        mPayImagePopWindow.hide();
                    })
                    .buildCenterPopWindowItem2ClickListener(() -> mPayImagePopWindow.hide())
                    .buildCenterPopWindowLinkClickListener(new PayPopWindow
                            .CenterPopWindowLinkClickListener() {
                        @Override
                        public void onLongClick() {

                        }

                        @Override
                        public void onClicked() {

                        }
                    })
                    .build();
        }

        if (mPayWatchPopWindow == null) {
            mPayWatchPopWindow = PayPopWindow.builder()
                    .with(getActivity())
                    .isWrap(true)
                    .isFocus(true)
                    .isOutsideTouch(true)
                    .buildLinksColor1(R.color.themeColor)
                    .buildLinksColor2(R.color.important_for_content)
                    .contentView(R.layout.ppw_for_center)
                    .backgroundAlpha(POPUPWINDOW_ALPHA)
                    .buildDescrStr(String.format(getString(R.string.qa_pay_for_watch_answer_hint) + getString(R
                                    .string.buy_pay_member),
                            mPresenter.getSystemConfig().getQuestionConfig().getOnlookers_amount()
                            , mPresenter.getGoldName()))
                    .buildLinksStr(getString(R.string.qa_pay_for_watch))
                    .buildTitleStr(getString(R.string.qa_pay_for_watch))
                    .buildItem1Str(getString(R.string.buy_pay_in_payment))
                    .buildItem2Str(getString(R.string.buy_pay_out))
                    .buildMoneyStr(getString(R.string.buy_pay_integration, "" + mPresenter.getSystemConfig().getQuestionConfig()
                            .getOnlookers_amount()))
                    .buildCenterPopWindowItem1ClickListener(() -> {
                        AnswerInfoBean answerInfoBean = mListDatas.get(mCurrentPosition);
                        if (answerInfoBean == null || answerInfoBean.getId() == null) {
                            showSnackErrorMessage(getString(R.string.pay_fail));
                        } else {
                            if (mPresenter.usePayPassword()) {
                                mIlvPassword.setPayNote(new InputPasswordView.PayNote(null, answerInfoBean.getId(), (long) mCurrentPosition));
                                showInputPsdView(true);
                            } else {
                                mPresenter.payForOnlook(answerInfoBean.getId(), mCurrentPosition, null);
                            }
                        }
                        mPayWatchPopWindow.hide();
                    })
                    .buildCenterPopWindowItem2ClickListener(() -> mPayWatchPopWindow.hide())
                    .buildCenterPopWindowLinkClickListener(new PayPopWindow
                            .CenterPopWindowLinkClickListener() {
                        @Override
                        public void onLongClick() {

                        }

                        @Override
                        public void onClicked() {

                        }
                    })
                    .build();
        }

        if (mDeleteQuestionPopWindow == null) {
            mDeleteQuestionPopWindow = CenterAlertPopWindow.builder()
                    .with(getActivity())
                    .parentView(getView())
                    .isOutsideTouch(true)
                    .isFocus(true)
                    .backgroundAlpha(POPUPWINDOW_ALPHA)
                    .animationStyle(R.style.style_actionPopupAnimation)
                    .backgroundAlpha(CustomPopupWindow.POPUPWINDOW_ALPHA)
                    .titleStr(getString(R.string.qa_question_delete))
                    .desStr(getString(R.string.qa_question_delete_alert))
                    .buildCenterPopWindowItem1ClickListener(new CenterAlertPopWindow.CenterPopWindowItemClickListener() {
                        @Override
                        public void onRightClicked() {
                            // 删除问题
                            mPresenter.deleteQuestion(mQaListInfoBean.getId());
                            mDeleteQuestionPopWindow.hide();
                        }

                        @Override
                        public void onLeftClicked() {
                            mDeleteQuestionPopWindow.hide();

                        }
                    })

                    .build();
        }

    }

    private void onLookToAnswerDetail(boolean isNeedOnlook) {
        // 跳转查看 围观肯定是第一个
        AnswerInfoBean answerInfoBean = mListDatas.get(mCurrentPosition);
        if (answerInfoBean != null && isNeedOnlook) {
            answerInfoBean.setOnlookers_count(answerInfoBean.getOnlookers_count() + 1);
            mQaListInfoBean.getInvitation_answers().get(0).setOnlookers_count(answerInfoBean.getOnlookers_count() + 1);
            refreshData();
        }
        startToAnswerDetail(answerInfoBean);
    }

    @Override
    public void onOrderTypeSelected(int type) {
        mQuestionDetailHeader.setCurrentOrderType(type);
        mCurrentOrderType = type == 0 ? QuestionDetailHeader.ORDER_DEFAULT : QuestionDetailHeader
                .ORDER_BY_TIME;
        requestNetData(0L, false);
        if (mOrderTypeSelectPop != null && mOrderTypeSelectPop.isShowing()) {
            mOrderTypeSelectPop.dismiss();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REWARD_CODE && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            if (bundle != null) {
                Double amount = bundle.getDouble(BUNDLE_QUESTION_ID, 0);
                mQaListInfoBean.setAmount(amount);
                mQuestionDetailHeader.updateRewardType(mQaListInfoBean, mPresenter.getRatio());
            }
        }
    }

    @Override
    public void onToWatchClick(AnswerInfoBean answerInfoBean, int position, boolean canNotRead) {
        // 发布者信息
        boolean isMineAnwser = answerInfoBean.getUser_id() == AppApplication.getMyUserIdWithdefault();
        // 是自己发布的，并且没有被采纳,设置采纳按钮
        boolean isMineQuestion = mQaListInfoBean.getUser_id() == AppApplication.getMyUserIdWithdefault();
        boolean hasNoAdoptionAnswers = mQaListInfoBean.getAdoption_answers() == null || mQaListInfoBean.getAdoption_answers().isEmpty();
        boolean hasAdoptionInviteAnswer = mPresenter.getCurrentQuestion().getInvitation_answers() != null
                && !mPresenter.getCurrentQuestion().getInvitation_answers().isEmpty()
                && mPresenter.getCurrentQuestion().getInvitation_answers().get(0).getAdoption() == 1;
        if (!isMineAnwser && isMineQuestion && hasNoAdoptionAnswers && !hasAdoptionInviteAnswer) {
            //采纳
            mPresenter.adoptionAnswer(mQaListInfoBean, answerInfoBean);
        } else {
            //围观
            mCurrentPosition = position - mHeaderAndFooterWrapper.getHeadersCount();
            if (canNotRead) {
                mPayWatchPopWindow.show();
            } else {
                onLookToAnswerDetail(false);
            }
        }
    }

}

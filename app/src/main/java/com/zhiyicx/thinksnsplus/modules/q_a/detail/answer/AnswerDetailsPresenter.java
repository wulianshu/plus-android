package com.zhiyicx.thinksnsplus.modules.q_a.detail.answer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;

import com.zhiyicx.baseproject.base.TSFragment;
import com.zhiyicx.baseproject.config.ImageZipConfig;
import com.zhiyicx.baseproject.em.manager.util.TSEMConstants;
import com.zhiyicx.baseproject.impl.share.UmengSharePolicyImpl;
import com.zhiyicx.baseproject.share.OnShareCallbackListener;
import com.zhiyicx.baseproject.share.Share;
import com.zhiyicx.baseproject.share.ShareContent;
import com.zhiyicx.baseproject.share.SharePolicy;
import com.zhiyicx.common.base.BaseFragment;
import com.zhiyicx.common.base.BaseJsonV2;
import com.zhiyicx.common.config.MarkdownConfig;
import com.zhiyicx.common.dagger.scope.FragmentScoped;
import com.zhiyicx.common.utils.ConvertUtils;
import com.zhiyicx.common.utils.DeviceUtils;
import com.zhiyicx.common.utils.RegexUtils;
import com.zhiyicx.common.utils.TimeUtils;
import com.zhiyicx.common.utils.log.LogUtils;
import com.zhiyicx.thinksnsplus.R;
import com.zhiyicx.thinksnsplus.base.AppApplication;
import com.zhiyicx.thinksnsplus.base.AppBasePresenter;
import com.zhiyicx.thinksnsplus.base.BaseSubscribeForV2;
import com.zhiyicx.thinksnsplus.config.ErrorCodeConfig;
import com.zhiyicx.thinksnsplus.config.EventBusTagConfig;
import com.zhiyicx.thinksnsplus.data.beans.AnswerCommentListBean;
import com.zhiyicx.thinksnsplus.data.beans.AnswerDigListBean;
import com.zhiyicx.thinksnsplus.data.beans.AnswerInfoBean;
import com.zhiyicx.thinksnsplus.data.beans.Letter;
import com.zhiyicx.thinksnsplus.data.beans.RealAdvertListBean;
import com.zhiyicx.thinksnsplus.data.beans.RewardsCountBean;
import com.zhiyicx.thinksnsplus.data.beans.SendDynamicDataBean;
import com.zhiyicx.thinksnsplus.data.beans.UserInfoBean;
import com.zhiyicx.thinksnsplus.data.source.local.AllAdvertListBeanGreenDaoImpl;
import com.zhiyicx.thinksnsplus.data.source.local.AnswerCommentListBeanGreenDaoImpl;
import com.zhiyicx.thinksnsplus.data.source.local.AnswerInfoListBeanGreenDaoImpl;
import com.zhiyicx.thinksnsplus.data.source.repository.BaseQARepository;
import com.zhiyicx.thinksnsplus.data.source.repository.UserInfoRepository;
import com.zhiyicx.thinksnsplus.modules.chat.private_letter.ChooseFriendActivity;
import com.zhiyicx.thinksnsplus.modules.dynamic.send.SendDynamicActivity;
import com.zhiyicx.thinksnsplus.service.backgroundtask.BackgroundTaskHandler;
import com.zhiyicx.thinksnsplus.utils.ImageUtils;
import com.zhiyicx.thinksnsplus.utils.TSShareUtils;

import org.jetbrains.annotations.NotNull;
import org.simple.eventbus.EventBus;
import org.simple.eventbus.Subscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.zhiyicx.baseproject.config.ApiConfig.APP_PATH_SHARE_QA_ANSWER_DETAIL;
import static com.zhiyicx.thinksnsplus.config.EventBusTagConfig.EVENT_SEND_INFO_LIST_COLLECT;
import static com.zhiyicx.thinksnsplus.data.beans.InfoCommentListBean.SEND_ING;

/**
 * @Author Jliuer
 * @Date 2017/03/15
 * @Email Jliuer@aliyun.com
 * @Description
 */
@FragmentScoped
public class AnswerDetailsPresenter extends AppBasePresenter<AnswerDetailsConstract.View> implements AnswerDetailsConstract.Presenter, OnShareCallbackListener {

    @Inject
    public SharePolicy mSharePolicy;

    @Inject
    UserInfoRepository mUserInfoRepository;

    @Inject
    AnswerCommentListBeanGreenDaoImpl mAnswerCommentListBeanGreenDao;

    @Inject
    AnswerInfoListBeanGreenDaoImpl mAnswerInfoListBeanGreenDao;

    @Inject
    AllAdvertListBeanGreenDaoImpl mAllAdvertListBeanGreenDao;

    @Inject
    BaseQARepository mBaseQARepository;
    private Subscription subscription;

    @Inject
    public AnswerDetailsPresenter(AnswerDetailsConstract
                                          .View rootView) {
        super(rootView);
    }


    @Override
    public void requestNetData(Long maxId, final boolean isLoadMore) {
        Long id = mRootView.getAnswerInfo().getId();
        getAnswerDetail(id, maxId, isLoadMore);

//        if (mRootView.getAnswerInfo().getCommentList() == null) {
//
//        } else {
//            mBaseQARepository.getAnswerCommentList(mRootView.getAnswerId(), maxId)
//                    .subscribe(new BaseSubscribeForV2<List<AnswerCommentListBean>>() {
//                        @Override
//                        protected void onSuccess(List<AnswerCommentListBean> data) {
//                            mRootView.onChildNetResponseSuccess(data, isLoadMore);
//                        }
//
//                        @Override
//                        protected void onFailure(String message, int code) {
//                            super.onFailure(message, code);
//                            handleInfoHasBeDeleted(code);
//                        }
//
//                        @Override
//                        protected void onException(Throwable throwable) {
//                            super.onException(throwable);
//                            mRootView.onResponseError(throwable, isLoadMore);
//                        }
//                    });
//        }
    }

    private void handleInfoHasBeDeleted(int code) {
        if (code == ErrorCodeConfig.DATA_HAS_BE_DELETED) {
            mAnswerInfoListBeanGreenDao.deleteSingleCache(mRootView.getAnswerInfo());
            mRootView.infoMationHasBeDeleted();
        } else {
            mRootView.loadAllError();
        }
    }

    @Override
    public void canclePay() {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void payForOnlook(long answer_id, String psd) {

        subscription = handleIntegrationBlance((long) getSystemConfigBean().getQuestionConfig().getOnlookers_amount())
                .doOnSubscribe(() -> mRootView.showSnackLoadingMessage(mContext.getString(R
                        .string.pay_alert_ing)))
                .flatMap(o -> mBaseQARepository.payForOnlook(answer_id, psd))
                .subscribe(new BaseSubscribeForV2<BaseJsonV2<AnswerInfoBean>>() {
                    @Override
                    protected void onSuccess(BaseJsonV2<AnswerInfoBean> data) {
                        mRootView.paySuccess();
                        getAnswerDetail(answer_id, 0L, false);
                    }

                    @Override
                    protected void onFailure(String message, int code) {
                        super.onFailure(message, code);
                        if (usePayPassword()){
                            mRootView.payFailed(message);
                            return;
                        }
                        mRootView.showSnackErrorMessage(message);
                    }

                    @Override
                    protected void onException(Throwable throwable) {
                        super.onException(throwable);
                        if (isIntegrationBalanceCheck(throwable)) {
                            mRootView.paySuccess();
                            mRootView.finish();
                            return;
                        }
                        if (usePayPassword()){
                            mRootView.payFailed(mContext.getString(R.string.pay_alert_failed));
                            return;
                        }
                        mRootView.showSnackErrorMessage(mContext.getString(R.string.pay_alert_failed));
                    }
                });
        addSubscrebe(subscription);

    }

    @Override
    public void shareAnswer(Bitmap bitmap) {
        ((UmengSharePolicyImpl) mSharePolicy).setOnShareCallbackListener(this);
        ShareContent shareContent = new ShareContent();
        shareContent.setTitle(mContext.getString(R.string.app_name_anster, mContext.getString(R.string.app_name)));
        shareContent.setUrl(TSShareUtils.convert2ShareUrl(String.format(Locale.getDefault(), APP_PATH_SHARE_QA_ANSWER_DETAIL,
                mRootView.getAnswerInfo().getQuestion_id(), mRootView.getAnswerInfo().getId())));
        String shargeContent = mRootView.getAnswerInfo().getText_body();
        if (TextUtils.isEmpty(shargeContent)) {
            shargeContent = mRootView.getAnswerInfo().getBody();
        }
        shareContent.setContent(shargeContent);

        if (bitmap == null) {
            shareContent.setBitmap(ConvertUtils.drawBg4Bitmap(Color.WHITE, BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.icon)));
        } else {
            shareContent.setBitmap(bitmap);
        }
        int id = RegexUtils.getImageIdFromMarkDown(MarkdownConfig.IMAGE_FORMAT, mRootView.getAnswerInfo().getBody());

        String shareUrl = "";
        if (id > 0) {
            int w = DeviceUtils.getScreenWidth(mContext);
            int h = mContext.getResources().getDimensionPixelOffset(R.dimen.qa_info_iamge_height);
            shareUrl = ImageUtils.imagePathConvertV2(id, w, h, ImageZipConfig.IMAGE_80_ZIP);
        }
        shareContent.setImage(shareUrl);

        mSharePolicy.setShareContent(shareContent);

        UmengSharePolicyImpl.ShareBean forward = new UmengSharePolicyImpl.ShareBean(R.mipmap.detail_share_forwarding, mContext.getString(R.string.share_forward), Share.FORWARD);
        UmengSharePolicyImpl.ShareBean letter = new UmengSharePolicyImpl.ShareBean(R.mipmap.detail_share_sent, mContext.getString(R.string.share_letter), Share.LETTER);

        List<UmengSharePolicyImpl.ShareBean> data = new ArrayList<>();
        data.add(forward);
        data.add(letter);

        mSharePolicy.showShare(((TSFragment) mRootView).getActivity(), data);
    }

    @Override
    public void handleCollect(boolean isUnCollected, long answer_id) {
        if (AppApplication.getmCurrentLoginAuth() == null) {
            return;
        }
        mRootView.setCollect(isUnCollected);
        mRootView.getAnswerInfo().setCollected(isUnCollected);

        mAnswerInfoListBeanGreenDao.insertOrReplace(mRootView.getAnswerInfo());
        EventBus.getDefault().post(mRootView.getAnswerInfo(), EVENT_SEND_INFO_LIST_COLLECT);
        mBaseQARepository.handleCollect(isUnCollected, answer_id);
    }

    @Override
    public void handleLike(boolean isLiked, long news_id) {
        if (AppApplication.getmCurrentLoginAuth() == null) {
            return;
        }
        UserInfoBean userInfoBean = mUserInfoBeanGreenDao
                .getSingleDataFromCache(AppApplication.getMyUserIdWithdefault());
        AnswerDigListBean digListBean = new AnswerDigListBean();
        digListBean.setUser_id(userInfoBean.getUser_id());
        digListBean.setId(System.currentTimeMillis());
        digListBean.setDiggUserInfo(userInfoBean);
        if (mRootView.getAnswerInfo().getLikes() == null) {
            mRootView.getAnswerInfo().setLikes(new ArrayList<>());
        }
        if (isLiked) {
            mRootView.getAnswerInfo().getLikes().add(0, digListBean);// 放到第一个
            mRootView.getAnswerInfo().setLikes_count(mRootView.getAnswerInfo().getLikes_count() + 1);
        } else {
            for (AnswerDigListBean answerDigListBean : mRootView.getAnswerInfo().getLikes()) {
                if (answerDigListBean.getUser_id().equals(userInfoBean.getUser_id())) {
                    mRootView.getAnswerInfo().getLikes().remove(answerDigListBean);
                    mRootView.getAnswerInfo().setLikes_count(mRootView.getAnswerInfo().getLikes_count() - 1);
                    break;
                }
            }
        }
        mRootView.getAnswerInfo().setLiked(isLiked);
        mRootView.setDigg(isLiked);
        Bundle bundle = new Bundle();
        bundle.putSerializable(EventBusTagConfig.EVENT_UPDATE_ANSWER_LIST_LIKE, mRootView.getAnswerInfo());
        EventBus.getDefault().post(bundle, EventBusTagConfig.EVENT_UPDATE_ANSWER_LIST_LIKE);
        mAnswerInfoListBeanGreenDao.insertOrReplace(mRootView.getAnswerInfo());
        mBaseQARepository.handleAnswerLike(isLiked, news_id);
    }

    @Override
    protected boolean useEventBus() {
        return true;
    }

    @Override
    public void reqReWardsData(int id) {
        getAnswerDetail((long) id, 0L, false);
    }

    @Override
    public void adoptionAnswer(long question_id, long answer_id) {
        Subscription subscription = mBaseQARepository.adoptionAnswer(question_id, answer_id)
                .doOnSubscribe(() -> mRootView.showSnackLoadingMessage(mContext.getString(R.string.bill_doing)))
                .subscribe(new BaseSubscribeForV2<BaseJsonV2<Object>>() {
                    @Override
                    protected void onSuccess(BaseJsonV2<Object> data) {
                        mRootView.getAnswerInfo().setAdoption(1);
                        List<AnswerInfoBean> answers = new ArrayList<>();
                        answers.add(mRootView.getAnswerInfo());
                        mRootView.getAnswerInfo().getQuestion().setHas_adoption(true);
                        mRootView.getAnswerInfo().getQuestion().setAdoption_answers(answers);
                        mRootView.showSnackSuccessMessage(mContext.getString(R.string.qa_question_answer_adopt));
                    }

                    @Override
                    protected void onFailure(String message, int code) {
                        super.onFailure(message, code);
                        mRootView.showSnackErrorMessage(message);
                    }

                    @Override
                    protected void onException(Throwable throwable) {
                        super.onException(throwable);
                        mRootView.showSnackErrorMessage(mContext.getString(R.string.err_net_not_work));
                    }
                });
        addSubscrebe(subscription);
    }

    @Override
    public void getAnswerDetail(Long answerId, Long maxId, boolean isLoadMore) {
        Subscription subscription = Observable.zip(mBaseQARepository.getAnswerDetail(answerId),
                mBaseQARepository.getAnswerCommentList(answerId, maxId), (answerInfoBean, answerCommentListBeen) -> {
                    answerInfoBean.setCommentList(answerCommentListBeen);
                    return answerInfoBean;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscribeForV2<AnswerInfoBean>() {
                    @Override
                    protected void onSuccess(AnswerInfoBean data) {
                        mAnswerInfoListBeanGreenDao.saveSingleData(data);
                        mRootView.updateReWardsView(new RewardsCountBean(data.getRewarder_count(), "" + data
                                        .getRewards_amount(), getGoldName()),
                                data.getRewarders());
                        mRootView.updateAnswerHeader(data, isLoadMore);
                    }

                    @Override
                    protected void onFailure(String message, int code) {
                        super.onFailure(message, code);
                        handleInfoHasBeDeleted(code);
                    }

                    @Override
                    protected void onException(Throwable throwable) {
                        super.onException(throwable);
                        mRootView.onResponseError(throwable, isLoadMore);
                    }

                });
        addSubscrebe(subscription);
    }

    @Override
    public void deleteAnswer() {
        mBaseQARepository.deleteAnswer(mRootView.getAnswerInfo().getId(), new BackgroundTaskHandler.OnNetResponseCallBack() {
            @Override
            public void onSuccess(Object data) {
                mRootView.deleteAnswer();
            }

            @Override
            public void onFailure(String message, int code) {
                mRootView.showSnackErrorMessage(message);
            }

            @Override
            public void onException(Throwable throwable) {
                mRootView.showSnackErrorMessage(throwable.getMessage());
            }
        });

    }

    @Override
    public void deleteComment(AnswerCommentListBean data) {
        mAnswerCommentListBeanGreenDao.deleteSingleCache(data);
        mRootView.getListDatas().remove(data);
        mRootView.getAnswerInfo().setComments_count(mRootView.getAnswerInfo().getComments_count() - 1);
        if (mRootView.getListDatas().isEmpty()) {
            AnswerCommentListBean emptyData = new AnswerCommentListBean();
            mRootView.getListDatas().add(emptyData);
        }
        mRootView.refreshData();
        mBaseQARepository.deleteComment(mRootView.getAnswerId().intValue(), data.getId().intValue());
    }

    @Override
    public void handleFollowUser(UserInfoBean userInfoBean) {
        mUserInfoRepository.handleFollow(userInfoBean);
        mRootView.upDateFollowFansState(userInfoBean.isFollower());
    }

    @Override
    public List<RealAdvertListBean> getAdvert() {
        if (!com.zhiyicx.common.BuildConfig.USE_ADVERT || mAllAdvertListBeanGreenDao.getInfoDetailAdvert() == null) {
            return new ArrayList<>();
        }
        return mAllAdvertListBeanGreenDao.getInfoDetailAdvert().getMRealAdvertListBeen();
    }


    /**
     * 处理发送动态数据
     */
    @Subscriber(tag = EventBusTagConfig.EVENT_SEND_COMMENT_TO_ANSWER_LIST)
    public void handleSendComment(AnswerCommentListBean answerCommentListBean) {
        Subscription subscribe = Observable.just(answerCommentListBean)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(answerCommentListBean1 -> {
                    int size = mRootView.getListDatas().size();
                    int infoPosition = -1;
                    for (int i = 0; i < size; i++) {
                        if (mRootView.getListDatas().get(i).getComment_mark()
                                == answerCommentListBean1.getComment_mark()) {
                            infoPosition = i;
                            mRootView.getListDatas().get(i).setState(answerCommentListBean1
                                    .getState());
                            mRootView.getListDatas().get(i).setId(answerCommentListBean1.getId());
                            mRootView.getListDatas().get(i).setComment_mark
                                    (answerCommentListBean1.getComment_mark());
                            break;
                        }
                    }
                    return infoPosition;
                })
                .subscribe(integer -> {
                    if (integer > 0) {
                        mRootView.refreshData(); // 加上 header
                    }

                }, Throwable::printStackTrace);
        addSubscrebe(subscribe);

    }

    @Subscriber(tag = EventBusTagConfig.EVENT_UPDATE_ANSWER_OR_QUESTION)
    public void updateData(Long tag) {
        requestNetData(tag, false);
    }

    @Override
    public void sendComment(int reply_id, String content) {
        AnswerCommentListBean createComment = new AnswerCommentListBean();

        createComment.setState(SEND_ING);

        createComment.setBody(content);

        createComment.setReply_user((long) reply_id);

        createComment.setCreated_at(TimeUtils.getCurrenZeroTimeStr());

        createComment.setUser_id(AppApplication.getmCurrentLoginAuth().getUser_id());

        String comment_mark = AppApplication.getmCurrentLoginAuth().getUser_id()
                + "" + System.currentTimeMillis();
        createComment.setComment_mark(Long.parseLong(comment_mark));

        if (reply_id == 0) {
            UserInfoBean userInfoBean = new UserInfoBean();
            userInfoBean.setUser_id(0L);
            createComment.setToUserInfoBean(userInfoBean);
        } else {
            createComment.setToUserInfoBean(mUserInfoBeanGreenDao.getSingleDataFromCache(
                    (long) reply_id));
        }
        createComment.setFromUserInfoBean(mUserInfoBeanGreenDao.getSingleDataFromCache(
                AppApplication.getmCurrentLoginAuth().getUser_id()));
        mAnswerCommentListBeanGreenDao.insertOrReplace(createComment);
        if (mRootView.getListDatas() != null && mRootView.getListDatas().get(0).getBody() == null) {
            mRootView.getListDatas().remove(0);// 去掉占位图
        }
        mRootView.getListDatas().add(0, createComment);
        mRootView.getAnswerInfo().setComments_count(mRootView.getAnswerInfo().getComments_count() + 1);
        mRootView.refreshData();
        mBaseQARepository.sendComment(content, mRootView.getAnswerId(), reply_id,
                createComment.getComment_mark());
    }

    @Override
    public void onStart(Share share) {
        Letter letter;
        switch (share) {
            case FORWARD:
                letter = new Letter(TSEMConstants.BUNDLE_CHAT_MESSAGE_FORWARD_TYPE_ANSWERS);
                letter.setName(mRootView.getAnswerInfo().getQuestion().getSubject());
                String content = RegexUtils.replaceAllLines(mRootView.getAnswerInfo().getBody());
                content = RegexUtils.replaceImageId(MarkdownConfig.IMAGE_FORMAT, content);
                letter.setContent(content);
                letter.setId(mRootView.getAnswerId() + "");
                SendDynamicDataBean sendWordsDynamicDataBean = new SendDynamicDataBean();
                sendWordsDynamicDataBean.setDynamicBelong(SendDynamicDataBean.NORMAL_DYNAMIC);
                sendWordsDynamicDataBean.setDynamicType(SendDynamicDataBean.TEXT_ONLY_DYNAMIC);
                SendDynamicActivity.startToSendDynamicActivity(((BaseFragment) mRootView).getActivity(), sendWordsDynamicDataBean, letter);
                break;
            case LETTER:
                letter = new Letter(TSEMConstants.BUNDLE_CHAT_MESSAGE_LETTER_TYPE_ANSWER);
                letter.setName(mRootView.getAnswerInfo().getQuestion().getSubject());
                letter.setContent(RegexUtils.replaceImageId(MarkdownConfig.IMAGE_FORMAT, RegexUtils.replaceAllLines(mRootView.getAnswerInfo().getBody())));
                letter.setId(mRootView.getAnswerId() + "");
                ChooseFriendActivity.startChooseFriendActivity(((BaseFragment) mRootView).getActivity(), letter);
                break;
            default:
        }
    }

    @Override
    public void onSuccess(Share share) {
        mRootView.showSnackSuccessMessage(mContext.getString(R.string.share_sccuess));
    }

    @Override
    public void onError(Share share, Throwable throwable) {
        mRootView.showSnackErrorMessage(mContext.getString(R.string.share_fail));
    }

    @Override
    public void onCancel(Share share) {
        mRootView.showSnackSuccessMessage(mContext.getString(R.string.share_cancel));
    }

    @Override
    public void requestCacheData(Long maxId, boolean isLoadMore) {
        mRootView.onCacheResponseSuccess(null, isLoadMore);
    }

    @Override
    public boolean insertOrUpdateData(@NotNull List<AnswerCommentListBean> data, boolean isLoadMore) {
        return false;
    }


}

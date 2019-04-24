package com.zhiyicx.thinksnsplus.modules.wallet;

import com.zhiyicx.baseproject.base.IBaseTouristPresenter;
import com.zhiyicx.baseproject.base.SystemConfigBean;
import com.zhiyicx.common.mvp.i.IBaseView;

import rx.Observable;

/**
 * @Describe
 * @Author Jungle68
 * @Date 2017/4/26
 * @Contact master.jungle68@gmail.com
 */

public interface WalletContract {

    interface View extends IBaseView<Presenter> {
        /**
         * update balance
         *
         * @param balance current user's balance
         */
        void updateBalance(double balance);

        /**
         * handle request loading
         *
         * @param isShow true ,showComment loading
         */
        void handleLoading(boolean isShow);

        /**
         * wallet callback
         *
         * @param walletConfigBean wallet config info
         * @param tag              action tag, 1 recharge 2 withdraw
         */
        void walletConfigCallBack(SystemConfigBean.WalletConfigBean walletConfigBean, int tag);

    }

    interface Presenter extends IBaseTouristPresenter {
        /**
         * update user info
         */
        void updateUserInfo();

        /**
         * @return true when first looking wallet page
         */
        boolean checkIsNeedTipPop();

        String getTipPopRule();

        /**
         * check wallet config info, if walletconfig has cach used it or get it from server
         *
         * @param tag action tag
         */
        void checkWalletConfig(int tag, final boolean isNeedTip);
    }
}

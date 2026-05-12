package com.test.mall.portal.component.trade.alipay.model.result;

import com.alipay.api.response.AlipayTradeRefundResponse;
import com.test.mall.portal.component.trade.alipay.model.TradeStatus;

/**
 * Created by liuyangkly on 15/8/27.
 */
public class AlipayF2FRefundResult implements Result {
    private TradeStatus tradeStatus;
    private AlipayTradeRefundResponse response;

    public AlipayF2FRefundResult(AlipayTradeRefundResponse response) {
        this.response = response;
    }

    public TradeStatus getTradeStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(TradeStatus tradeStatus) {
        this.tradeStatus = tradeStatus;
    }

    public AlipayTradeRefundResponse getResponse() {
        return response;
    }

    public void setResponse(AlipayTradeRefundResponse response) {
        this.response = response;
    }

    @Override
    public boolean isTradeSuccess() {
        return response != null &&
                TradeStatus.SUCCESS.equals(tradeStatus);
    }
}

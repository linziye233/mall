package com.test.mall.portal.component.trade.alipay.model.result;

import com.alipay.api.response.AlipayTradePayResponse;
import com.test.mall.portal.component.trade.alipay.model.TradeStatus;

/**
 * Created by liuyangkly on 15/8/26.
 */
public class AlipayF2FPayResult implements Result {
    private TradeStatus tradeStatus;
    private AlipayTradePayResponse response;

    public AlipayF2FPayResult(AlipayTradePayResponse response) {
        this.response = response;
    }

    public TradeStatus getTradeStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(TradeStatus tradeStatus) {
        this.tradeStatus = tradeStatus;
    }

    public AlipayTradePayResponse getResponse() {
        return response;
    }

    public void setResponse(AlipayTradePayResponse response) {
        this.response = response;
    }

    @Override
    public boolean isTradeSuccess() {
        return response != null &&
                TradeStatus.SUCCESS.equals(tradeStatus);
    }
}

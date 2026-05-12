package com.test.mall.portal.component.trade.alipay.model.result;

import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.test.mall.portal.component.trade.alipay.model.TradeStatus;

/**
 * Created by liuyangkly on 15/8/27.
 */
public class AlipayF2FPrecreateResult implements Result {
    private TradeStatus tradeStatus;
    private AlipayTradePrecreateResponse response;

    public AlipayF2FPrecreateResult(AlipayTradePrecreateResponse response) {
        this.response = response;
    }

    public TradeStatus getTradeStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(TradeStatus tradeStatus) {
        this.tradeStatus = tradeStatus;
    }

    public AlipayTradePrecreateResponse getResponse() {
        return response;
    }

    public void setResponse(AlipayTradePrecreateResponse response) {
        this.response = response;
    }

    @Override
    public boolean isTradeSuccess() {
        return response != null &&
                TradeStatus.SUCCESS.equals(tradeStatus);
    }
}

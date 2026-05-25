package com.test.mall.agent.supervisor;

import lombok.Getter;

/**
 * 用户意图分类
 */
@Getter
public enum Intent {
    ORDER("订单相关", "下单/付款/物流/查询订单状态等"),
    PRODUCT("商品相关", "价格/库存/规格/商品推荐等"),
    AFTERSALES("售后相关", "退款/退货/换货/投诉等"),
    GENERAL("通用咨询", "优惠活动/账户问题/其他咨询");

    private final String label;
    private final String description;

    Intent(String label, String description) {
        this.label = label;
        this.description = description;
    }
}

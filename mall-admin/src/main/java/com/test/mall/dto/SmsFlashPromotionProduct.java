package com.test.mall.dto;

import com.test.mall.model.PmsProduct;
import com.test.mall.model.SmsFlashPromotionProductRelation;
import lombok.Getter;
import lombok.Setter;

/**
 * 限时购及商品信息封装
 * Created on 2018/11/16.
 */
public class SmsFlashPromotionProduct extends SmsFlashPromotionProductRelation {
    @Getter
    @Setter
    private PmsProduct product;
}

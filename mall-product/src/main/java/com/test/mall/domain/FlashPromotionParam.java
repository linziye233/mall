package com.test.mall.domain;

import com.test.mall.model.SmsFlashPromotion;
import com.test.mall.model.SmsFlashPromotionProductRelation;
import lombok.Data;

import java.util.List;

@Data
public class FlashPromotionParam extends SmsFlashPromotion {
    //一个产品只能与该秒杀活动的一个活动场次关联，比如参加了10点场，就不能再参加12点场
    private List<SmsFlashPromotionProductRelation> relation;
}

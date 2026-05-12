package com.test.mall.portal.feignapi.pms;

import com.test.mall.common.api.CommonResult;
import com.test.mall.portal.domain.FlashPromotionProduct;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@FeignClient(name = "tulingmall-product",path = "/pms")
public interface PmsProductFeignApi {
    
    @RequestMapping(value = "/flashPromotion/getHomeSecKillProductList", method = RequestMethod.GET)
    @ResponseBody
    CommonResult<List<FlashPromotionProduct>> getHomeSecKillProductList();
}

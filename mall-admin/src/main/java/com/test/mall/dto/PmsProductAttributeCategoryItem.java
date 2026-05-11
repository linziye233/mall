package com.test.mall.dto;


import com.test.mall.model.PmsProductAttribute;
import com.test.mall.model.PmsProductAttributeCategory;

import java.util.List;

/**
 * 包含有分类下属性的dto
 * Created on 2018/5/24.
 */
public class PmsProductAttributeCategoryItem extends PmsProductAttributeCategory {
    private List<PmsProductAttribute> productAttributeList;

    public List<PmsProductAttribute> getProductAttributeList() {
        return productAttributeList;
    }

    public void setProductAttributeList(List<PmsProductAttribute> productAttributeList) {
        this.productAttributeList = productAttributeList;
    }
}

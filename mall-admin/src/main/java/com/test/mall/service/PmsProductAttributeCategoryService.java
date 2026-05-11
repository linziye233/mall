package com.test.mall.service;

import com.test.mall.dto.PmsProductAttributeCategoryItem;
import com.test.mall.model.PmsProductAttributeCategory;

import java.util.List;

/**
 * 商品属性分类Service
 * Created on 2018/4/26.
 * Created on 2018/4/26.
 */
public interface PmsProductAttributeCategoryService {
    int create(String name);

    int update(Long id, String name);

    int delete(Long id);

    PmsProductAttributeCategory getItem(Long id);

    List<PmsProductAttributeCategory> getList(Integer pageSize, Integer pageNum);

    List<PmsProductAttributeCategoryItem> getListWithAttr();
}

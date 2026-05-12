package com.test.mall.portal.dao;

import com.test.mall.portal.domain.PmsCommentParam;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface PortalProductCommentDao {

    List<PmsCommentParam> getCommentList(Long productId);

    Integer selectUserOrder(@Param("userId") Long userId, @Param("productId") Long productId);
}

package com.test.mall.dao;


import com.test.mall.domain.PortalMemberInfo;

public interface PortalMemberInfoDao {
    /**
     * 查询会员信息
     * @param memberId
     * @return
     */
    PortalMemberInfo getMemberInfo(Long memberId);
}

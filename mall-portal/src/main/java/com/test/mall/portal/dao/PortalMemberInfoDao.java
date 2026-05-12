package com.test.mall.portal.dao;


import com.test.mall.portal.domain.PortalMemberInfo;

/**

 * @description: 会员信息获取
 **/
public interface PortalMemberInfoDao {
    /**
     * 查询会员信息
     * @param memberId
     * @return
     */
    PortalMemberInfo getMemberInfo(Long memberId);
}

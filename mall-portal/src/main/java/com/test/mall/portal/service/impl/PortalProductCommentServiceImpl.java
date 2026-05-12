package com.test.mall.portal.service.impl;

import com.github.pagehelper.PageHelper;
import com.test.mall.common.api.CommonResult;
import com.test.mall.mapper.PmsCommentMapper;
import com.test.mall.mapper.PmsCommentReplayMapper;
import com.test.mall.model.PmsComment;
import com.test.mall.model.PmsCommentReplay;
import com.test.mall.model.UmsMember;
import com.test.mall.portal.dao.PortalProductCommentDao;
import com.test.mall.portal.domain.PmsCommentParam;
import com.test.mall.portal.feignapi.ums.UmsMemberFeignApi;
import com.test.mall.portal.service.PortalProductCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PortalProductCommentServiceImpl implements PortalProductCommentService {
    @Autowired
    private PortalProductCommentDao productCommentDao;

    @Autowired
    private PmsCommentMapper pmsMapper;

    @Autowired
    private PmsCommentReplayMapper replayMapper;

    @Autowired
    private UmsMemberFeignApi umsMemberFeignApi;

    /**
     * 获取评论列表
     * @param productId
     * @return
     */
    @Override
    public CommonResult<List<PmsCommentParam>> getCommentList(Long productId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        return CommonResult.success(productCommentDao.getCommentList(productId));
    }

    /**
     * 用户评价
     * @param pmsComment
     * @return
     */
    @Override
    public Integer insertProductComment(PmsComment pmsComment) {
        UmsMember member = umsMemberFeignApi.getMemberById().getData();
        //判断一下当前用户是否购买过当前评论的商品
        Integer status = productCommentDao.selectUserOrder(member.getId(), pmsComment.getProductId());
        if(status > 0){
            pmsComment.setCreateTime(new Date());
            pmsComment.setShowStatus(0);
            pmsComment.setMemberNickName(member.getNickname());
            pmsComment.setMemberIcon(member.getIcon());
            return pmsMapper.insert(pmsComment);
        }
        return -1;
    }

    /**
     * 用户评价回复
     * @param reply
     * @return
     */
    @Override
    public Integer insertCommentReply(PmsCommentReplay reply) {
        UmsMember member = umsMemberFeignApi.getMemberById().getData();
        reply.setCreateTime(new Date());
        reply.setMemberNickName(member.getNickname());
        reply.setMemberIcon(member.getIcon());
        reply.setType(0);
        return replayMapper.insert(reply);
    }
}

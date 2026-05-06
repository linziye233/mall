package com.test.mall.mapper;

import com.test.mall.model.OauthClientDetails;
import com.test.mall.model.OauthClientDetailsExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface OauthClientDetailsMapper {
    long countByExample(OauthClientDetailsExample example);

    int deleteByExample(OauthClientDetailsExample example);

    int deleteByPrimaryKey(String clientId);

    int insert(OauthClientDetails row);

    int insertSelective(OauthClientDetails row);

    List<OauthClientDetails> selectByExample(OauthClientDetailsExample example);

    OauthClientDetails selectByPrimaryKey(String clientId);

    int updateByExampleSelective(@Param("row") OauthClientDetails row, @Param("example") OauthClientDetailsExample example);

    int updateByExample(@Param("row") OauthClientDetails row, @Param("example") OauthClientDetailsExample example);

    int updateByPrimaryKeySelective(OauthClientDetails row);

    int updateByPrimaryKey(OauthClientDetails row);
}
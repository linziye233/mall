package com.test.mall.portal.domain;

import com.test.mall.model.UmsMember;
import com.test.mall.model.UmsMemberLevel;
import lombok.Data;


@Data
public class PortalMemberInfo extends UmsMember {
    private UmsMemberLevel umsMemberLevel;
}

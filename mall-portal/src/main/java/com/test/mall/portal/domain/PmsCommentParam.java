package com.test.mall.portal.domain;

import com.test.mall.model.PmsComment;
import com.test.mall.model.PmsCommentReplay;
import lombok.Data;

import java.util.List;


@Data
public class PmsCommentParam extends PmsComment {
    private List<PmsCommentReplay> pmsCommentReplayList;
}

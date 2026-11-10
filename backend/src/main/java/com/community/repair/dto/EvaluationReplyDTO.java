package com.community.repair.dto;

import com.community.repair.constant.EvaluationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 回复评价请求参数（对应接口 E-04：POST /api/evaluations/{evaluationId}/reply）。
 *
 * <p>依据：接口文档 4.7.4；需求 FR-6-06、BR-6-07「管理员回复一条评价只能回复一次，
 * 回复后不可修改」。</p>
 *
 * <p>回复人身份（管理员 ID 与姓名）由服务端从 JWT（{@code UserContext}）读取，
 * <b>不接受前端传入</b>。</p>
 */
@Data
public class EvaluationReplyDTO implements Serializable {

    /**
     * 回复内容，<b>必填</b>，5~200 字。
     *
     * <p>{@code @NotBlank} 拦截空串与纯空格；Service 层会再做一次 {@code trim} 与长度校验，
     * 保证「两个字的半截回复」这类脏数据不会落库。</p>
     */
    @NotBlank(message = "回复内容不能为空")
    @Size(min = EvaluationConstants.REPLY_MIN_LENGTH, max = EvaluationConstants.REPLY_MAX_LENGTH,
            message = EvaluationConstants.MSG_REPLY_INVALID)
    private String replyContent;
}

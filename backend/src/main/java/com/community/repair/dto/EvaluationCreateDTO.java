package com.community.repair.dto;

import com.community.repair.constant.EvaluationConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 提交服务评价请求参数（对应接口 E-01：POST /api/evaluations）。
 *
 * <p>依据：接口文档 4.7.1；需求 FR-6-02、FR-6-03、BR-6-01 ~ BR-6-05。</p>
 *
 * <p>评分维度共三项（服务态度 / 响应速度 / 维修质量），综合评分由服务端对三项求平均
 * 并四舍五入得出（{@code totalScore}），<b>不由前端上传</b>，避免客户端伪造综合分。</p>
 *
 * <p><b>参数校验分两层</b>：</p>
 * <ol>
 *   <li>{@code @NotNull} / {@code @Min} / {@code @Max} / {@code @Size} 由 {@code @Valid}
 *       完成第一层（评分取值域 1~5、内容长度上限 200）；</li>
 *   <li>Service 层完成第二层：纯空格内容归一化为 {@code null}、
 *       图片张数与总长校验、匿名标识取值域兜底。</li>
 * </ol>
 *
 * <p><b>不接收 {@code residentId} 入参</b>：评价人身份由服务端从 JWT（{@code UserContext}）
 * 读取；{@code workerId} 亦由服务端从工单的 {@code worker_id} 派生，
 * 前端无法通过任何入参伪造被评价人。</p>
 */
@Data
public class EvaluationCreateDTO implements Serializable {

    /**
     * 被评价工单 ID，<b>必填</b>。
     *
     * <p>服务端将校验：工单存在（404）→ 当前用户为该工单报修人（403，BR-6-06）
     * → 工单状态为「已验收(5)」（409，BR-6-01）→ 工单尚未被评价（409，BR-6-02）。</p>
     */
    @NotNull(message = "被评价工单ID不能为空")
    private Long orderId;

    /** 服务态度评分，1~5 分，必填（BR-6-03） */
    @NotNull(message = "服务态度评分不能为空")
    @Min(value = EvaluationConstants.SCORE_MIN, message = EvaluationConstants.MSG_SCORE_INVALID)
    @Max(value = EvaluationConstants.SCORE_MAX, message = EvaluationConstants.MSG_SCORE_INVALID)
    private Integer scoreService;

    /** 响应速度评分，1~5 分，必填（BR-6-03） */
    @NotNull(message = "响应速度评分不能为空")
    @Min(value = EvaluationConstants.SCORE_MIN, message = EvaluationConstants.MSG_SCORE_INVALID)
    @Max(value = EvaluationConstants.SCORE_MAX, message = EvaluationConstants.MSG_SCORE_INVALID)
    private Integer scoreSpeed;

    /** 维修质量评分，1~5 分，必填（BR-6-03） */
    @NotNull(message = "维修质量评分不能为空")
    @Min(value = EvaluationConstants.SCORE_MIN, message = EvaluationConstants.MSG_SCORE_INVALID)
    @Max(value = EvaluationConstants.SCORE_MAX, message = EvaluationConstants.MSG_SCORE_INVALID)
    private Integer scoreQuality;

    /**
     * 评价内容，选填，0~200 字。
     *
     * <p>长度上限在 DTO 声明为 200（业务口径），数据库列宽 VARCHAR(500) 为冗余上限。
     * Service 层会对纯空格做归一化处理（落库为 {@code null}）。</p>
     */
    @Size(max = EvaluationConstants.CONTENT_MAX_LENGTH, message = EvaluationConstants.MSG_CONTENT_TOO_LONG)
    private String content;

    /**
     * 评价图片路径，多张以英文逗号分隔，最多 3 张，总长 ≤ 1000。
     *
     * <p>图片先经模块零 U-15「上传图片」接口落盘并返回相对路径，再以逗号拼接提交。</p>
     */
    @Size(max = EvaluationConstants.IMAGES_MAX_LENGTH, message = EvaluationConstants.MSG_IMAGES_TOO_LENGTH)
    private String images;

    /**
     * 是否匿名：0 实名 / 1 匿名，默认 0。
     *
     * <p>匿名评价（{@code isAnonymous = 1}）对<b>非本人</b>隐藏评价人信息
     * （{@code residentId} 置 null、{@code residentName} 返回「匿名用户」），
     * 依据 BR-6-06 保护评价人隐私。</p>
     */
    @Min(value = EvaluationConstants.ANONYMOUS_NO, message = EvaluationConstants.MSG_ANONYMOUS_INVALID)
    @Max(value = EvaluationConstants.ANONYMOUS_YES, message = EvaluationConstants.MSG_ANONYMOUS_INVALID)
    private Integer isAnonymous;
}

package com.community.repair.dto;

import com.community.repair.constant.OrderConstants;
import com.community.repair.constant.UserConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提交报修工单请求参数（对应接口 O-01：POST /api/orders）。
 *
 * <p>依据：接口文档 4.3.1；需求 FR-2-01、FR-2-02、FR-2-03、FR-2-07、FR-2-08，
 * 业务规则 BR-2-01 ~ BR-2-03、BR-2-08。</p>
 *
 * <p><b>注意：本 DTO 刻意<b>不包含</b> {@code residentId} 与 {@code status} 字段。</b></p>
 * <ul>
 *   <li>{@code residentId} 由服务端从 JWT 解析（{@code UserContext}）后自动关联，
 *       不接受前端传入，防止越权代他人报修（BR-2-06）；</li>
 *   <li>{@code status} 由服务端固定写入 8「待审核」，不接受前端传入（BR-2-04）。</li>
 * </ul>
 *
 * <p>@Valid 完成第一道校验（空值、长度、格式），Service 层再做第二道业务校验
 * （类目启用状态、期望时间不得早于当前时间、图片张数），两类提示分别对应
 * 接口文档 400 的两种触发场景。</p>
 */
@Data
public class OrderCreateDTO implements Serializable {

    /** 报修类目ID，必填；必须是「启用」状态的类目（BR-1-05） */
    @NotNull(message = "请选择报修类目")
    private Long categoryId;

    /** 报修标题，必填，2~100 字 */
    @NotBlank(message = "请填写报修标题")
    @Size(min = OrderConstants.TITLE_MIN_LENGTH, max = OrderConstants.TITLE_MAX_LENGTH,
            message = "报修标题长度须为 2~100 个字符")
    private String title;

    /** 故障详细描述，必填，10~500 字（BR-2-03） */
    @NotBlank(message = "请填写故障详细描述")
    @Size(min = OrderConstants.DESCRIPTION_MIN_LENGTH, max = OrderConstants.DESCRIPTION_MAX_LENGTH,
            message = "故障描述长度需在 10~500 字之间")
    private String description;

    /**
     * 故障现场图片路径，选填。
     * <p>多张以英文逗号分隔，最多 3 张（FR-2-07），总长 ≤ 1000；
     * 具体张数与总长校验在 Service 层完成（@Size 只管总长）。</p>
     */
    @Size(max = OrderConstants.IMAGES_MAX_LENGTH, message = "现场照片路径总长度不能超过 1000 个字符")
    private String images;

    /** 上门维修地址，必填，2~255 字；前端默认回填居民 {@code sys_user.address}（BR-8-05） */
    @NotBlank(message = "请填写上门维修地址")
    @Size(min = OrderConstants.ADDRESS_MIN_LENGTH, max = OrderConstants.ADDRESS_MAX_LENGTH,
            message = "上门维修地址长度须为 2~255 个字符")
    private String address;

    /** 联系人姓名，必填，2~50 字 */
    @NotBlank(message = "请填写联系人姓名")
    @Size(min = OrderConstants.CONTACT_NAME_MIN_LENGTH, max = OrderConstants.CONTACT_NAME_MAX_LENGTH,
            message = "联系人姓名长度须为 2~50 个字符")
    private String contactName;

    /** 联系人电话，必填，11 位手机号（BR-2-02） */
    @NotBlank(message = "请填写联系人电话")
    @Pattern(regexp = UserConstants.REGEX_PHONE, message = "请输入正确的 11 位手机号")
    private String contactPhone;

    /** 紧急程度，选填，默认 0 普通；取值范围校验在 Service 层（FR-2-08） */
    private Integer urgency;

    /**
     * 居民期望上门时间，选填，格式 {@code yyyy-MM-dd HH:mm:ss}。
     * <p>由全局 {@code JacksonConfig} 统一完成字符串 ↔ LocalDateTime 的转换；
     * 「不得早于当前时间」的业务校验在 Service 层完成（BR-2-08）。</p>
     */
    private LocalDateTime expectTime;
}

package com.community.repair.dto;

import com.community.repair.constant.OrderConstants;
import com.community.repair.constant.UserConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 修改工单请求参数（对应接口 O-04：PUT /api/orders/{orderId}）。
 *
 * <p>依据：接口文档 4.3.4；需求 FR-2-01。</p>
 *
 * <p><b>局部更新语义</b>：本 DTO 的所有字段<b>均允许为 null</b>，
 * null 表示「该项不修改」。配合 MyBatis-Plus 默认的 NOT_NULL 字段更新策略，
 * null 字段不会进入 UPDATE 语句，天然实现局部更新——这与模块一
 * {@code CategoryUpdateDTO} 的设计思路完全一致。</p>
 *
 * <p>因此字段校验采用「非 null 才校验」的注解写法（{@code @Size} / {@code @Pattern}
 * 本身对 null 放行），业务性更强的校验（类目启用状态、状态域、期望时间）
 * 统一放在 Service 层，避免误判为 400。</p>
 *
 * <p><b>不包含 status 字段</b>：修改不改变工单状态（8 → 8，0 → 0），不触发重新审核（接口文档 4.3.4）。</p>
 */
@Data
public class OrderUpdateDTO implements Serializable {

    /** 报修类目ID，选填；若填写则必须为启用类目 */
    private Long categoryId;

    /** 报修标题，选填，2~100 字 */
    @Size(min = OrderConstants.TITLE_MIN_LENGTH, max = OrderConstants.TITLE_MAX_LENGTH,
            message = "报修标题长度须为 2~100 个字符")
    private String title;

    /** 故障详细描述，选填，10~500 字 */
    @Size(min = OrderConstants.DESCRIPTION_MIN_LENGTH, max = OrderConstants.DESCRIPTION_MAX_LENGTH,
            message = "故障描述长度需在 10~500 字之间")
    private String description;

    /**
     * 故障现场图片路径，选填，最多 3 张。
     * <p>允许传空字符串以「清空全部照片」，因此本字段<b>不设长度下限</b>，只限制总长。</p>
     */
    @Size(max = OrderConstants.IMAGES_MAX_LENGTH, message = "现场照片路径总长度不能超过 1000 个字符")
    private String images;

    /** 上门维修地址，选填，2~255 字 */
    @Size(min = OrderConstants.ADDRESS_MIN_LENGTH, max = OrderConstants.ADDRESS_MAX_LENGTH,
            message = "上门维修地址长度须为 2~255 个字符")
    private String address;

    /** 联系人姓名，选填，2~50 字 */
    @Size(min = OrderConstants.CONTACT_NAME_MIN_LENGTH, max = OrderConstants.CONTACT_NAME_MAX_LENGTH,
            message = "联系人姓名长度须为 2~50 个字符")
    private String contactName;

    /** 联系人电话，选填，11 位手机号 */
    @Pattern(regexp = UserConstants.REGEX_PHONE, message = "请输入正确的 11 位手机号")
    private String contactPhone;

    /** 紧急程度，选填：0 普通 / 1 紧急 */
    @Min(value = OrderConstants.URGENCY_NORMAL, message = "紧急程度取值只能是 0（普通）或 1（紧急）")
    @Max(value = OrderConstants.URGENCY_URGENT, message = "紧急程度取值只能是 0（普通）或 1（紧急）")
    private Integer urgency;

    /** 居民期望上门时间，选填，不得早于当前时间 */
    private LocalDateTime expectTime;
}

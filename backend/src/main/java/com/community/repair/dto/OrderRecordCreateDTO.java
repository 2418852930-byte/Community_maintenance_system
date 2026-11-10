package com.community.repair.dto;

import com.community.repair.constant.ProgressConstants;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 追加维修进度请求参数（对应接口 R-02：POST /api/orders/{orderId}/records）。
 *
 * <p>依据：接口文档 4.5.2；需求 FR-4-04、FR-4-05、BR-4-01、BR-4-02、BR-4-03、BR-4-05。</p>
 *
 * <p>维修人员在维修过程中<b>多次</b>追加进度说明（如「已联系住户」「等待配件」），
 * 可上传维修过程照片、登记更换的配件与本次费用。每次追加独立成一行
 * {@code repair_record}，构成进度时间轴的中间节点（BR-4-05：记录不可修改、不可删除）。</p>
 *
 * <p><b>本对象不含 orderId</b>：工单 ID 由路径参数传入，避免「路径 ID 与 Body ID 不一致」的歧义；
 * <b>也不含 workerId / recordTime</b>：操作人一律由服务端从 JWT 解析
 * （{@code UserContext}），记录时间默认取系统当前时间。</p>
 */
@Data
public class OrderRecordCreateDTO implements Serializable {

    /**
     * 记录类型：2 到达现场 / 3 维修进度 / 4 更换配件。
     *
     * <p>1 接单、5 完工、6 转派退回分别由 D-08 / R-04 / D-09 专有接口写入，
     * <b>不允许</b>通过本接口伪造；Service 层会二次校验取值域
     * （{@link com.community.repair.enums.RecordTypeEnum#isAppendable}）。</p>
     */
    @NotNull(message = "请选择记录类型")
    private Integer recordType;

    /**
     * 进度说明，2~200 字（FR-4-04），落库为 {@code repair_record.progress_content}。
     *
     * <p>「等待配件 / 暂停」这类中间态按接口文档 Q5 的统一口径，
     * 以「维修进度（3）」+ 本字段文本承载，不新增状态码。</p>
     */
    @Size(min = ProgressConstants.PROGRESS_CONTENT_MIN_LENGTH,
            max = ProgressConstants.PROGRESS_CONTENT_MAX_LENGTH,
            message = "进度说明长度须为 2~200 个字符")
    private String progressContent;

    /** 维修过程照片路径，多张以英文逗号分隔，最多 3 张（FR-4-05）；可为空 */
    @Size(max = ProgressConstants.IMAGES_MAX_LENGTH, message = "维修过程照片路径总长度不能超过 1000 个字符")
    private String images;

    /** 本次使用 / 更换的配件，≤ 255 字；记录类型为「更换配件(4)」时建议填写 */
    @Size(max = ProgressConstants.PARTS_USED_MAX_LENGTH, message = "配件说明长度不能超过 255 个字符")
    private String partsUsed;

    /** 本次费用（元），默认 0.00，不可为负；会累加至工单 {@code total_cost} */
    @DecimalMin(value = "0.00", message = "本次费用不能为负数")
    private BigDecimal cost;

    /** 记录发生时间，默认取当前时间；允许补录稍早的时间（如离线补登） */
    private LocalDateTime recordTime;
}

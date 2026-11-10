package com.community.repair.constant;

import java.util.List;

/**
 * 公共文件上传（U-15）业务常量。
 *
 * <p>对应接口文档 4.1.15「上传图片」与需求 FR-2-07、FR-4-05、NFR-S-05。</p>
 *
 * <p><b>为什么由模块二补齐该接口？</b>U-15 在接口文档中归属「模块零 登录注册与用户管理」，
 * 是供报修现场照片（本模块 FR-2-07）、维修过程照片（模块四 FR-4-05）、评价图片、
 * 头像共同复用的公共支撑接口。模块一交付说明的「后续阶段待办」第 1 条已明确：
 * 「图片上传属公共支撑接口 U-15（本地存储 + jpg/png ≤ 5MB 校验），随模块二一并实现」。
 * 由于本模块「上传故障图片」强依赖该能力，故在此按接口文档原样补齐，
 * 实现上落在公共层（{@code controller/FileController}），供后续模块直接复用、
 * <b>不重复开发</b>。</p>
 */
public final class FileConstants {

    private FileConstants() {
    }

    // ------------------------- 存储路径 -------------------------

    /** 上传根目录默认值（相对后端运行目录），可通过 file.upload.root-path 覆盖 */
    public static final String DEFAULT_ROOT_PATH = "./uploads";

    /** 图片访问 URL 前缀（对应静态资源映射 /uploads/**） */
    public static final String URL_PREFIX = "/uploads";

    /** 工单现场照片的子目录名（接口文档示例：/uploads/order/20260917/xxx.jpg） */
    public static final String DIR_ORDER = "order";

    /** 子目录中的日期格式，按天分目录便于清理与排查 */
    public static final String DIR_DATE_PATTERN = "yyyyMMdd";

    // ------------------------- 类型与大小限制（NFR-S-05） -------------------------

    /** 允许的图片扩展名（小写），接口文档 4.1.15：仅允许 jpg/jpeg/png */
    public static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");

    /** 单张图片大小上限：5MB */
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    /** 单次请求大小上限：15MB（预留多张照片场景的空间） */
    public static final long MAX_REQUEST_SIZE = 15L * 1024 * 1024;

    // ------------------------- 统一提示语 -------------------------

    /** 未选择文件（400） */
    public static final String MSG_FILE_EMPTY = "请选择要上传的图片";

    /** 文件类型不支持（415） */
    public static final String MSG_FILE_TYPE = "仅支持 jpg / jpeg / png 格式的图片";

    /** 文件过大（413） */
    public static final String MSG_FILE_TOO_LARGE = "图片大小不能超过 5MB";

    /** 保存失败（500） */
    public static final String MSG_FILE_SAVE_FAIL = "图片保存失败，请稍后重试";
}

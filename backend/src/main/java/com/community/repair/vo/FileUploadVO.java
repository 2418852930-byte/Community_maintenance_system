package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传返回结果 VO（对应接口 U-15 的 data 结构）。
 *
 * <p>依据：接口文档 4.1.15。{@code url} 为可直接访问的相对路径
 * （如 {@code /uploads/order/20260918/a1b2c3d4.jpg}），
 * 前端把多张的 url 以英文逗号拼接后作为 {@code images} 字段提交给 O-01。</p>
 */
@Data
public class FileUploadVO implements Serializable {

    /** 图片访问相对路径 */
    private String url;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;
}

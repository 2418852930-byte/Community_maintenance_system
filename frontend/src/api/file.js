import request from '@/utils/request'

/**
 * 公共文件接口（U-15 上传图片）
 * 对应后端 FileController（/api/files/**）
 *
 * 定位：公共支撑接口，接口文档归属「模块零 登录注册与用户管理」，
 * 供报修现场照片（本模块 FR-2-07）、维修过程照片、评价图片、头像共同复用。
 * 模块一交付说明的待办中已明确「随模块二一并实现」，因此在此补齐全项目共用的上传封装，
 * 后续模块直接 import 本方法即可，不重复开发。
 */

/**
 * U-15 上传图片
 * @param {File} file 图片文件，仅 jpg / jpeg / png，单张 ≤ 5MB
 * @returns {Promise<Object>} res.data = { url, fileName, fileSize }
 */
export function uploadImage (file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/files/upload',
    method: 'post',
    data: formData,
    // 上传耗时高于普通接口，单独放宽超时；Content-Type 交给 axios 自动带 boundary
    timeout: 30000
  })
}

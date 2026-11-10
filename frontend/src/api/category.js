import request from '@/utils/request'

/**
 * 报修类目管理类接口（C-01 ~ C-07）
 * 对应后端 RepairCategoryController（/api/categories/**）
 *
 * 权限说明：
 *  - C-01 / C-02 / C-03 全部登录用户可访问；
 *  - C-04 ~ C-07 仅系统管理员，服务端由 @RequireRole + JwtInterceptor 强制校验。
 */

/** C-01 类目分页列表，params: { pageNum, pageSize, categoryName, status, parentId, onlyEnabled } */
export function pageCategories (params) {
  return request({
    url: '/categories',
    method: 'get',
    params
  })
}

/** C-02 类目树查询（两级），onlyEnabled 为 true 时仅返回启用类目及其启用子类 */
export function getCategoryTree (onlyEnabled = false) {
  return request({
    url: '/categories/tree',
    method: 'get',
    params: { onlyEnabled }
  })
}

/** C-03 类目详情 */
export function getCategoryDetail (categoryId) {
  return request({
    url: `/categories/${categoryId}`,
    method: 'get'
  })
}

/** C-04 新增类目 */
export function createCategory (data) {
  return request({
    url: '/categories',
    method: 'post',
    data
  })
}

/** C-05 修改类目 */
export function updateCategory (categoryId, data) {
  return request({
    url: `/categories/${categoryId}`,
    method: 'put',
    data
  })
}

/** C-06 删除类目（逻辑删除） */
export function deleteCategory (categoryId) {
  return request({
    url: `/categories/${categoryId}`,
    method: 'delete'
  })
}

/** C-07 启用 / 停用类目 */
export function changeCategoryStatus (categoryId, status) {
  return request({
    url: `/categories/${categoryId}/status`,
    method: 'put',
    data: { status }
  })
}

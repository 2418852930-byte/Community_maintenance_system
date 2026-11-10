package com.community.repair.service;

import com.community.repair.common.PageResult;
import com.community.repair.dto.CategoryCreateDTO;
import com.community.repair.dto.CategoryQueryDTO;
import com.community.repair.dto.CategoryStatusUpdateDTO;
import com.community.repair.dto.CategoryUpdateDTO;
import com.community.repair.vo.CategorySimpleVO;
import com.community.repair.vo.CategoryStatusVO;
import com.community.repair.vo.CategoryTreeVO;
import com.community.repair.vo.CategoryVO;

import java.util.List;

/**
 * 报修类目管理模块 业务接口。
 *
 * <p>对应需求 FR-1-01 ~ FR-1-06，接口 C-01 ~ C-07。</p>
 *
 * <p>方法命名与接口编号的对应关系：</p>
 * <pre>
 *   C-01 pageCategories    类目分页列表（全部登录用户）
 *   C-02 getCategoryTree   类目树查询（全部登录用户）—— 供下游工单提交模块的级联选择器使用
 *   C-03 getCategoryDetail 类目详情（全部登录用户）
 *   C-04 createCategory    新增类目（管理员）
 *   C-05 updateCategory    修改类目（管理员）
 *   C-06 deleteCategory    删除类目（管理员，逻辑删除）
 *   C-07 changeStatus      启用 / 停用类目（管理员）
 * </pre>
 */
public interface RepairCategoryService {

    /** C-01 类目分页列表 */
    PageResult<CategoryVO> pageCategories(CategoryQueryDTO query);

    /** C-02 类目树查询（两级） */
    List<CategoryTreeVO> getCategoryTree(Boolean onlyEnabled);

    /** C-03 类目详情 */
    CategoryVO getCategoryDetail(Long categoryId);

    /** C-04 新增类目 */
    CategorySimpleVO createCategory(CategoryCreateDTO dto);

    /** C-05 修改类目 */
    CategoryVO updateCategory(Long categoryId, CategoryUpdateDTO dto);

    /** C-06 删除类目（逻辑删除） */
    void deleteCategory(Long categoryId);

    /** C-07 启用 / 停用类目 */
    CategoryStatusVO changeStatus(Long categoryId, CategoryStatusUpdateDTO dto);
}

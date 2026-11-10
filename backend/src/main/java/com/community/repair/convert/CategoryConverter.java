package com.community.repair.convert;

import com.community.repair.constant.CategoryConstants;
import com.community.repair.entity.RepairCategory;
import com.community.repair.vo.CategorySimpleVO;
import com.community.repair.vo.CategoryStatusVO;
import com.community.repair.vo.CategoryTreeVO;
import com.community.repair.vo.CategoryVO;

/**
 * 类目实体 -> VO 转换器（与已有的 {@code UserConverter} 保持同样的组织方式）。
 *
 * <p>集中处理两件事，避免在 Service 中散落重复代码：</p>
 * <ol>
 *   <li>状态码 0/1 翻译为中文名（statusName）；</li>
 *   <li>补全父级类目名称（parentName），该字段需由调用方传入以避免 N+1 查询。</li>
 * </ol>
 */
public final class CategoryConverter {

    private CategoryConverter() {
    }

    /**
     * 实体 -> 列表元素 / 详情 VO（C-01、C-03、C-05）。
     *
     * @param category   类目实体
     * @param parentName 父级类目名称；一级类目传 null
     */
    public static CategoryVO toVO(RepairCategory category, String parentName) {
        if (category == null) {
            return null;
        }
        CategoryVO vo = new CategoryVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setCategoryName(category.getCategoryName());
        vo.setParentId(category.getParentId());
        vo.setParentName(parentName);
        vo.setIcon(category.getIcon());
        vo.setDescription(category.getDescription());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        vo.setStatusName(statusName(category.getStatus()));
        vo.setCreateTime(category.getCreateTime());
        vo.setUpdateTime(category.getUpdateTime());
        return vo;
    }

    /**
     * 实体 -> 树节点 VO（C-02）。
     * <p>children 在 VO 中已初始化为空列表，由 Service 负责装配子节点。</p>
     */
    public static CategoryTreeVO toTreeVO(RepairCategory category) {
        if (category == null) {
            return null;
        }
        CategoryTreeVO vo = new CategoryTreeVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setCategoryName(category.getCategoryName());
        vo.setParentId(category.getParentId());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        return vo;
    }

    /** 实体 -> 新增结果 VO（C-04） */
    public static CategorySimpleVO toSimpleVO(RepairCategory category) {
        if (category == null) {
            return null;
        }
        CategorySimpleVO vo = new CategorySimpleVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setCategoryName(category.getCategoryName());
        vo.setCreateTime(category.getCreateTime());
        return vo;
    }

    /** 实体 -> 启停结果 VO（C-07） */
    public static CategoryStatusVO toStatusVO(RepairCategory category) {
        if (category == null) {
            return null;
        }
        CategoryStatusVO vo = new CategoryStatusVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setStatus(category.getStatus());
        vo.setUpdateTime(category.getUpdateTime());
        return vo;
    }

    /** 类目状态 -> 中文名 */
    public static String statusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == CategoryConstants.STATUS_ENABLED ? "启用" : "停用";
    }
}

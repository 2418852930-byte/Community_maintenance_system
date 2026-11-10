package com.community.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.repair.common.BusinessException;
import com.community.repair.common.PageResult;
import com.community.repair.common.ResultCode;
import com.community.repair.constant.CategoryConstants;
import com.community.repair.convert.CategoryConverter;
import com.community.repair.dto.CategoryCreateDTO;
import com.community.repair.dto.CategoryQueryDTO;
import com.community.repair.dto.CategoryStatusUpdateDTO;
import com.community.repair.dto.CategoryUpdateDTO;
import com.community.repair.entity.RepairCategory;
import com.community.repair.mapper.RepairCategoryMapper;
import com.community.repair.security.UserContext;
import com.community.repair.service.RepairCategoryService;
import com.community.repair.vo.CategorySimpleVO;
import com.community.repair.vo.CategoryStatusVO;
import com.community.repair.vo.CategoryTreeVO;
import com.community.repair.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报修类目管理模块 业务实现。
 *
 * <p>本类集中承载需求文档 5.1 节 FR-1-01 ~ FR-1-06 与 BR-1-01 ~ BR-1-06 的全部业务规则，
 * 每个方法上方的注释都标注了对应的规则编号，便于论文与答辩溯源。</p>
 *
 * <p>复用的基础组件（不重复创建）：</p>
 * <ul>
 *   <li>统一响应体 {@code Result} / 分页结构 {@code PageResult}；</li>
 *   <li>业务异常 {@code BusinessException} + 全局异常处理器 {@code GlobalExceptionHandler}；</li>
 *   <li>状态码字典 {@code ResultCode}；</li>
 *   <li>鉴权与角色校验 {@code JwtInterceptor} / {@code @RequireRole} / {@code UserContext}；</li>
 *   <li>字段自动填充 {@code MyMetaObjectHandler}、分页插件 {@code PaginationInnerInterceptor}。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepairCategoryServiceImpl implements RepairCategoryService {

    private final RepairCategoryMapper repairCategoryMapper;

    // ==================================================================================
    // 一、查询类接口（C-01 ~ C-03，全部登录用户可访问）
    // ==================================================================================

    /**
     * C-01 类目分页列表。
     *
     * <p>对应 FR-1-01、BR-1-06。支持类目名称模糊查询、状态筛选、父级类目筛选，
     * 以及 {@code onlyEnabled} 快速过滤（供居民端报修选择器使用）。</p>
     *
     * <p>排序规则（BR-1-06）：<b>先按 sort_order 升序，排序号相同时按 create_time 倒序</b>。</p>
     */
    @Override
    public PageResult<CategoryVO> pageCategories(CategoryQueryDTO query) {
        // 分页参数保护：pageNum >= 1，1 <= pageSize <= 100（接口文档 2.5 节）
        int pageNum = (query.getPageNum() == null || query.getPageNum() < 1) ? 1 : query.getPageNum();
        int pageSize = (query.getPageSize() == null || query.getPageSize() < 1)
                ? CategoryConstants.DEFAULT_PAGE_SIZE
                : Math.min(query.getPageSize(), CategoryConstants.MAX_PAGE_SIZE);

        LambdaQueryWrapper<RepairCategory> wrapper = new LambdaQueryWrapper<>();
        // 类目名称模糊匹配
        wrapper.like(StringUtils.hasText(query.getCategoryName()),
                RepairCategory::getCategoryName,
                StringUtils.hasText(query.getCategoryName()) ? query.getCategoryName().trim() : null);
        // 状态筛选；onlyEnabled = true 时强制只查启用类目（BR-1-05）
        if (Boolean.TRUE.equals(query.getOnlyEnabled())) {
            wrapper.eq(RepairCategory::getStatus, CategoryConstants.STATUS_ENABLED);
        } else {
            wrapper.eq(query.getStatus() != null, RepairCategory::getStatus, query.getStatus());
        }
        // 按父级类目筛选（用于查询某个一级类目下的子类目）
        wrapper.eq(query.getParentId() != null, RepairCategory::getParentId, query.getParentId());
        // BR-1-06：排序号升序 + 创建时间倒序
        wrapper.orderByAsc(RepairCategory::getSortOrder).orderByDesc(RepairCategory::getCreateTime);

        Page<RepairCategory> page = repairCategoryMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 一次性查出本页所有父级类目名称，避免逐条查询导致的 N+1 问题
        Map<Long, String> parentNameMap = loadParentNameMap(page.getRecords());
        return PageResult.of(page, category -> CategoryConverter.toVO(
                category,
                parentNameMap.get(category.getParentId())));
    }

    /**
     * C-02 类目树查询（两级）。
     *
     * <p>对应 FR-1-06、BR-1-03、BR-1-05。</p>
     *
     * <p><b>下游模块接入说明</b>：报修工单提交模块的级联选择器调用本方法并传
     * {@code onlyEnabled = true}，即可获得「启用的一级类目 + 其启用的子类目」，
     * 拿到树的叶子节点 categoryId 直接写入 {@code repair_order.category_id} 完成关联。</p>
     *
     * @param onlyEnabled 为 true 时仅返回启用类目及其启用子类
     */
    @Override
    public List<CategoryTreeVO> getCategoryTree(Boolean onlyEnabled) {
        LambdaQueryWrapper<RepairCategory> wrapper = new LambdaQueryWrapper<>();
        // BR-1-05：停用类目不出现在 onlyEnabled = true 的结果中
        if (Boolean.TRUE.equals(onlyEnabled)) {
            wrapper.eq(RepairCategory::getStatus, CategoryConstants.STATUS_ENABLED);
        }
        wrapper.orderByAsc(RepairCategory::getSortOrder).orderByDesc(RepairCategory::getCreateTime);

        List<RepairCategory> all = repairCategoryMapper.selectList(wrapper);

        // 按 parent_id 分组，便于装配两级结构
        Map<Long, List<CategoryTreeVO>> childrenMap = new HashMap<>();
        List<CategoryTreeVO> roots = new ArrayList<>();
        for (RepairCategory category : all) {
            CategoryTreeVO node = CategoryConverter.toTreeVO(category);
            if (category.getParentId() != null && category.getParentId() == CategoryConstants.ROOT_PARENT_ID) {
                roots.add(node);
            } else {
                childrenMap.computeIfAbsent(category.getParentId(), k -> new ArrayList<>()).add(node);
            }
        }
        // 一级节点挂载其子节点（BR-1-03：最多两级，无需递归）
        for (CategoryTreeVO root : roots) {
            List<CategoryTreeVO> children = childrenMap.get(root.getCategoryId());
            root.setChildren(children == null ? new ArrayList<>() : children);
        }
        return roots;
    }

    /**
     * C-03 类目详情。
     *
     * <p>对应 FR-1-01。用于类目编辑弹窗回显，返回结构同 C-01 列表元素。</p>
     */
    @Override
    public CategoryVO getCategoryDetail(Long categoryId) {
        RepairCategory category = selectExistingCategory(categoryId);
        return CategoryConverter.toVO(category, findParentName(category.getParentId()));
    }

    // ==================================================================================
    // 二、管理类接口（C-04 ~ C-07，仅系统管理员；权限由 @RequireRole + JwtInterceptor 强制校验）
    // ==================================================================================

    /**
     * C-04 新增类目。
     *
     * <p>对应 FR-1-02、BR-1-01（同父级名称唯一）、BR-1-02（名称 2~20 字符）、
     * BR-1-03（最多两级）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategorySimpleVO createCategory(CategoryCreateDTO dto) {
        String categoryName = dto.getCategoryName() == null ? null : dto.getCategoryName().trim();
        // BR-1-02 名称长度校验（二次校验，@Valid 已拦截空值，这里兜住纯空格的情况）
        validateNameLength(categoryName);
        validateDescriptionLength(dto.getDescription());

        // 父级类目规范化：null 视为一级类目
        Long parentId = dto.getParentId() == null ? CategoryConstants.ROOT_PARENT_ID : dto.getParentId();
        // BR-1-03 校验父级类目存在且必须是一级类目
        checkParentUsable(parentId);
        // BR-1-01 同一父类目下名称不可重复
        checkNameDuplicate(parentId, categoryName, null);

        RepairCategory category = new RepairCategory();
        category.setCategoryName(categoryName);
        category.setParentId(parentId);
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        // 状态默认启用
        category.setStatus(dto.getStatus() == null ? CategoryConstants.STATUS_ENABLED : dto.getStatus());
        validateStatus(category.getStatus());
        category.setDeleted(0);

        repairCategoryMapper.insert(category);
        // BR-8-20 同类做法：写操作记录操作人，便于答辩演示与日志排查
        log.info("新增类目成功：categoryId={}, categoryName={}, parentId={}, 操作人 userId={}",
                category.getCategoryId(), category.getCategoryName(), parentId, UserContext.getUserId());
        return CategoryConverter.toSimpleVO(category);
    }

    /**
     * C-05 修改类目。
     *
     * <p>对应 FR-1-03、BR-1-01、BR-1-04。</p>
     *
     * <p>关键校验：</p>
     * <ol>
     *   <li>类目必须存在，否则 404；</li>
     *   <li>若尝试变更层级（parentId 变化）：
     *       <ul>
     *         <li>已被工单引用 → 409（BR-1-04：仅允许改名称与排序号）；</li>
     *         <li>自身已带子类目 → 409（否则会出现三级类目，违反 BR-1-03）；</li>
     *         <li>上级类目必须存在且为一级类目；</li>
     *       </ul>
     *   </li>
     *   <li>名称在同一父级下唯一（BR-1-01）。</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO updateCategory(Long categoryId, CategoryUpdateDTO dto) {
        RepairCategory category = selectExistingCategory(categoryId);

        // ---------- ① 名称处理 ----------
        String newName = category.getCategoryName();
        if (StringUtils.hasText(dto.getCategoryName())) {
            newName = dto.getCategoryName().trim();
            validateNameLength(newName);
        }
        validateDescriptionLength(dto.getDescription());

        // ---------- ② 层级处理 ----------
        Long oldParentId = category.getParentId();
        Long newParentId = dto.getParentId() == null ? oldParentId : dto.getParentId();
        if (!oldParentId.equals(newParentId)) {
            // 不能把自己设为上级
            if (newParentId.equals(categoryId)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, CategoryConstants.MSG_PARENT_CANNOT_BE_SELF);
            }
            // BR-1-04 已被工单引用不允许变更层级
            if (repairCategoryMapper.countOrdersByCategoryId(categoryId) > 0) {
                throw new BusinessException(ResultCode.CONFLICT, CategoryConstants.MSG_CATEGORY_REFERENCED_NO_MOVE);
            }
            // 自身已有子类目时不能再降为二级类目，否则会出现三级
            if (newParentId != CategoryConstants.ROOT_PARENT_ID
                    && repairCategoryMapper.countChildrenByParentId(categoryId) > 0) {
                throw new BusinessException(ResultCode.CONFLICT, CategoryConstants.MSG_HAS_CHILDREN_CANNOT_DEMOTE);
            }
            // 新上级必须存在且为一级类目（BR-1-03）
            checkParentUsable(newParentId);
        }
        // BR-1-01 名称在同一父级下唯一（排除自身）
        checkNameDuplicate(newParentId, newName, categoryId);

        // ---------- ③ 状态处理（C-05 也允许顺带维护状态，与接口文档 C-05 的 Body 定义一致） ----------
        if (dto.getStatus() != null) {
            validateStatus(dto.getStatus());
        }

        // ---------- ④ 组装更新对象 ----------
        // 说明：MyBatis-Plus 默认字段策略为 NOT_NULL，null 字段不会进入 UPDATE 语句，
        // 因此这里天然实现了「只更新传了的字段」的局部更新语义；update_time 由
        // MyMetaObjectHandler 自动填充（建表方案 A 要求业务代码维护该列）。
        RepairCategory update = new RepairCategory();
        update.setCategoryId(categoryId);
        update.setCategoryName(newName);
        update.setParentId(newParentId);
        update.setIcon(dto.getIcon());
        update.setDescription(dto.getDescription());
        update.setSortOrder(dto.getSortOrder());
        update.setStatus(dto.getStatus());
        repairCategoryMapper.updateById(update);

        log.info("修改类目成功：categoryId={}, categoryName={}, parentId={}, 操作人 userId={}",
                categoryId, newName, newParentId, UserContext.getUserId());

        return CategoryConverter.toVO(repairCategoryMapper.selectById(categoryId), findParentName(newParentId));
    }

    /**
     * C-06 删除类目（逻辑删除）。
     *
     * <p>对应 FR-1-04、BR-1-04。两条前置校验：</p>
     * <ul>
     *   <li>已被工单引用 → 409「该类目已被工单使用，无法删除，可改为停用」；</li>
     *   <li>存在下级类目 → 409「请先删除其下级类目」。</li>
     * </ul>
     *
     * <p>实现说明：{@code repair_category} 采用逻辑删除（deleted 字段 + @TableLogic），
     * 这里用 LambdaUpdateWrapper 显式设置 deleted = 1 与 update_time，
     * 保证「删除」动作同样留下更新时间痕迹；删除后历史工单中的 category_id 不受影响。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long categoryId) {
        selectExistingCategory(categoryId);

        // BR-1-04 已被工单引用的类目不允许删除
        if (repairCategoryMapper.countOrdersByCategoryId(categoryId) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, CategoryConstants.MSG_CATEGORY_REFERENCED);
        }
        // 存在下级类目时须先删除子类目
        if (repairCategoryMapper.countChildrenByParentId(categoryId) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, CategoryConstants.MSG_HAS_CHILDREN);
        }

        repairCategoryMapper.update(null, new LambdaUpdateWrapper<RepairCategory>()
                .eq(RepairCategory::getCategoryId, categoryId)
                .set(RepairCategory::getDeleted, 1)
                .set(RepairCategory::getUpdateTime, LocalDateTime.now()));

        log.info("删除类目成功（逻辑删除）：categoryId={}, 操作人 userId={}", categoryId, UserContext.getUserId());
    }

    /**
     * C-07 启用 / 停用类目。
     *
     * <p>对应 FR-1-05、BR-1-05。停用后居民端报修选择器不再展示该选项
     * （即 C-01 的 onlyEnabled = true 与 C-02 的 onlyEnabled = true 都会过滤掉），
     * 但历史工单数据不受影响。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryStatusVO changeStatus(Long categoryId, CategoryStatusUpdateDTO dto) {
        selectExistingCategory(categoryId);
        validateStatus(dto.getStatus());

        RepairCategory update = new RepairCategory();
        update.setCategoryId(categoryId);
        update.setStatus(dto.getStatus());
        repairCategoryMapper.updateById(update);

        RepairCategory latest = repairCategoryMapper.selectById(categoryId);
        log.info("类目状态变更成功：categoryId={}, status={}, 操作人 userId={}",
                categoryId, dto.getStatus(), UserContext.getUserId());
        return CategoryConverter.toStatusVO(latest);
    }

    // ==================================================================================
    // 三、私有辅助方法
    // ==================================================================================

    /**
     * 查询类目，不存在或已被逻辑删除时抛 404（接口文档统一约定）。
     */
    private RepairCategory selectExistingCategory(Long categoryId) {
        RepairCategory category = categoryId == null ? null : repairCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, CategoryConstants.MSG_CATEGORY_NOT_FOUND);
        }
        return category;
    }

    /**
     * 批量加载父级类目名称映射（key = 父级ID，value = 父级名称）。
     * <p>用于列表接口避免 N+1 查询：一页 N 条数据只额外查 1 次。</p>
     */
    private Map<Long, String> loadParentNameMap(List<RepairCategory> records) {
        List<Long> parentIds = records.stream()
                .map(RepairCategory::getParentId)
                .filter(id -> id != null && id != CategoryConstants.ROOT_PARENT_ID)
                .distinct()
                .collect(Collectors.toList());
        if (parentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<RepairCategory> parents = repairCategoryMapper.selectBatchIds(parentIds);
        Map<Long, String> map = new HashMap<>();
        for (RepairCategory parent : parents) {
            map.put(parent.getCategoryId(), parent.getCategoryName());
        }
        return map;
    }

    /**
     * 查询单个父级类目名称，一级类目返回 null（接口文档约定 parentName 一级为 null）。
     */
    private String findParentName(Long parentId) {
        if (parentId == null || parentId == CategoryConstants.ROOT_PARENT_ID) {
            return null;
        }
        RepairCategory parent = repairCategoryMapper.selectById(parentId);
        return parent == null ? null : parent.getCategoryName();
    }

    /**
     * 校验上级类目是否可用：必须是「已存在的一级类目」（BR-1-03 最多两级）。
     *
     * @param parentId 上级类目 ID；0 表示一级类目，无需校验
     */
    private void checkParentUsable(Long parentId) {
        if (parentId == null || parentId == CategoryConstants.ROOT_PARENT_ID) {
            return;
        }
        RepairCategory parent = repairCategoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, CategoryConstants.MSG_PARENT_NOT_FOUND);
        }
        // 父级本身已是二级类目 → 再往下挂会形成三级，违反 BR-1-03
        if (parent.getParentId() != null && parent.getParentId() != CategoryConstants.ROOT_PARENT_ID) {
            throw new BusinessException(ResultCode.CONFLICT, CategoryConstants.MSG_LEVEL_EXCEED);
        }
    }

    /**
     * 校验同一父类目下类目名称是否重复（BR-1-01）。
     *
     * @param parentId    父级类目 ID
     * @param categoryName 类目名称
     * @param excludeId   需要排除的类目 ID（修改场景传自身 ID，新增场景传 null）
     */
    private void checkNameDuplicate(Long parentId, String categoryName, Long excludeId) {
        if (!StringUtils.hasText(categoryName)) {
            return;
        }
        LambdaQueryWrapper<RepairCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairCategory::getParentId, parentId)
                .eq(RepairCategory::getCategoryName, categoryName)
                .ne(excludeId != null, RepairCategory::getCategoryId, excludeId);
        if (repairCategoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, CategoryConstants.MSG_NAME_DUPLICATE);
        }
    }

    /** 校验类目名称长度（BR-1-02：2~20 个字符） */
    private void validateNameLength(String categoryName) {
        if (!StringUtils.hasText(categoryName)
                || categoryName.length() < CategoryConstants.NAME_MIN_LENGTH
                || categoryName.length() > CategoryConstants.NAME_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, CategoryConstants.MSG_NAME_LENGTH);
        }
    }

    /** 校验类目说明长度（对应字段 VARCHAR(255)） */
    private void validateDescriptionLength(String description) {
        if (description != null && description.length() > CategoryConstants.DESCRIPTION_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "类目说明最多 " + CategoryConstants.DESCRIPTION_MAX_LENGTH + " 个字符");
        }
    }

    /** 校验类目状态取值只能是 0 / 1 */
    private void validateStatus(Integer status) {
        if (status == null
                || (status != CategoryConstants.STATUS_DISABLED && status != CategoryConstants.STATUS_ENABLED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, CategoryConstants.MSG_STATUS_INVALID);
        }
    }
}

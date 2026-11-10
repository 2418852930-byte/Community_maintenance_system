package com.community.repair.controller;

import com.community.repair.common.PageResult;
import com.community.repair.common.Result;
import com.community.repair.dto.CategoryCreateDTO;
import com.community.repair.dto.CategoryQueryDTO;
import com.community.repair.dto.CategoryStatusUpdateDTO;
import com.community.repair.dto.CategoryUpdateDTO;
import com.community.repair.enums.RoleEnum;
import com.community.repair.security.RequireRole;
import com.community.repair.service.RepairCategoryService;
import com.community.repair.vo.CategorySimpleVO;
import com.community.repair.vo.CategoryStatusVO;
import com.community.repair.vo.CategoryTreeVO;
import com.community.repair.vo.CategoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报修类目管理类接口 Controller（C-01 ~ C-07）。
 *
 * <p>对应需求 FR-1-01 ~ FR-1-06，接口文档 4.2.1 ~ 4.2.7 节。</p>
 *
 * <p><b>权限设计</b>（复用已有 {@code JwtInterceptor} + {@code @RequireRole}，不新增任何鉴权组件）：</p>
 * <ul>
 *   <li>类上<b>不加</b> {@code @RequireRole} → C-01 / C-02 / C-03 只要携带合法 JWT 即可访问
 *       （接口文档标注为「全部登录用户」）；</li>
 *   <li>写操作方法上标注 {@code @RequireRole(RoleEnum.ADMIN)} → C-04 ~ C-07 仅系统管理员可访问，
 *       非管理员访问由拦截器统一返回 403 且不返回任何数据；</li>
 *   <li>拦截器「方法级注解优先于类级注解」，因此两种粒度可以安全地混用。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class RepairCategoryController {

    private final RepairCategoryService repairCategoryService;

    /**
     * C-01 类目分页列表。
     *
     * <p>GET /api/categories　全部登录用户</p>
     *
     * <p>支持 categoryName（模糊）、status、parentId 组合筛选；居民端与下游工单提交模块
     * 传 onlyEnabled=true 只取启用类目（BR-1-05）。排序为 sort_order 升序 + create_time 倒序（BR-1-06）。</p>
     */
    @GetMapping
    public Result<PageResult<CategoryVO>> pageCategories(CategoryQueryDTO query) {
        return Result.success("查询成功", repairCategoryService.pageCategories(query));
    }

    /**
     * C-02 类目树查询（两级）。
     *
     * <p>GET /api/categories/tree　全部登录用户</p>
     *
     * <p>供管理员类目树与<b>下游报修工单提交模块</b>的级联选择器（Cascader）使用；
     * onlyEnabled=true 时仅返回启用类目及其启用子类（BR-1-03、BR-1-05）。</p>
     *
     * <p>映射位置说明：本方法必须声明在 {@code /{categoryId}} 之前语义上才最清晰；
     * Spring MVC 的路径匹配会优先选择精确字面量 {@code /tree}，
     * 因此「tree」不会被当作 categoryId 解析，两者不会冲突。</p>
     */
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> getCategoryTree(
            @RequestParam(value = "onlyEnabled", required = false, defaultValue = "false") Boolean onlyEnabled) {
        return Result.success("查询成功", repairCategoryService.getCategoryTree(onlyEnabled));
    }

    /**
     * C-03 类目详情。
     *
     * <p>GET /api/categories/{categoryId}　全部登录用户</p>
     *
     * <p>用于类目编辑弹窗回显，含父级类目名称；类目不存在返回 404。</p>
     */
    @GetMapping("/{categoryId}")
    public Result<CategoryVO> getCategoryDetail(@PathVariable("categoryId") Long categoryId) {
        return Result.success("查询成功", repairCategoryService.getCategoryDetail(categoryId));
    }

    /**
     * C-04 新增类目。
     *
     * <p>POST /api/categories　仅系统管理员</p>
     *
     * <p>校验 BR-1-01（同父级名称唯一）、BR-1-02（名称 2~20 字符）、BR-1-03（最多两级）。</p>
     */
    @PostMapping
    @RequireRole(RoleEnum.ADMIN)
    public Result<CategorySimpleVO> createCategory(@Valid @RequestBody CategoryCreateDTO dto) {
        return Result.success("新增成功", repairCategoryService.createCategory(dto));
    }

    /**
     * C-05 修改类目。
     *
     * <p>PUT /api/categories/{categoryId}　仅系统管理员</p>
     *
     * <p>已被工单引用的类目仅允许修改名称与排序号，不允许变更层级（BR-1-04）。</p>
     */
    @PutMapping("/{categoryId}")
    @RequireRole(RoleEnum.ADMIN)
    public Result<CategoryVO> updateCategory(@PathVariable("categoryId") Long categoryId,
                                             @Valid @RequestBody CategoryUpdateDTO dto) {
        return Result.success("保存成功", repairCategoryService.updateCategory(categoryId, dto));
    }

    /**
     * C-06 删除类目。
     *
     * <p>DELETE /api/categories/{categoryId}　仅系统管理员</p>
     *
     * <p>逻辑删除；已被工单引用或存在下级类目时返回 409（BR-1-04）。</p>
     */
    @DeleteMapping("/{categoryId}")
    @RequireRole(RoleEnum.ADMIN)
    public Result<Void> deleteCategory(@PathVariable("categoryId") Long categoryId) {
        repairCategoryService.deleteCategory(categoryId);
        return Result.success("删除成功", null);
    }

    /**
     * C-07 启用 / 停用类目。
     *
     * <p>PUT /api/categories/{categoryId}/status　仅系统管理员</p>
     *
     * <p>停用后居民端报修选择器不再展示该选项，历史工单数据不受影响（BR-1-05）。</p>
     */
    @PutMapping("/{categoryId}/status")
    @RequireRole(RoleEnum.ADMIN)
    public Result<CategoryStatusVO> changeStatus(@PathVariable("categoryId") Long categoryId,
                                                 @Valid @RequestBody CategoryStatusUpdateDTO dto) {
        return Result.success("操作成功", repairCategoryService.changeStatus(categoryId, dto));
    }
}

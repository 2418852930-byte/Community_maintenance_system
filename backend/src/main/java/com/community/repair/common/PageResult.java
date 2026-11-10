package com.community.repair.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页返回结构（对应接口文档 2.5 节）。
 *
 * <pre>
 * {
 *   "total": 25,
 *   "pageNum": 1,
 *   "pageSize": 10,
 *   "pages": 3,
 *   "list": [ ... ]
 * }
 * </pre>
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private Long total;

    /** 当前页码，从 1 开始 */
    private Integer pageNum;

    /** 每页条数 */
    private Integer pageSize;

    /** 总页数 */
    private Integer pages;

    /** 当前页数据列表 */
    private List<T> list;

    public PageResult() {
        this.total = 0L;
        this.pageNum = 1;
        this.pageSize = 10;
        this.pages = 0;
        this.list = new ArrayList<>();
    }

    public PageResult(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list == null ? new ArrayList<>() : list;
        // 计算总页数：总记录数 / 每页条数，向上取整
        this.pages = (pageSize == null || pageSize <= 0)
                ? 0
                : (int) ((total + pageSize - 1) / pageSize);
    }

    /**
     * 由 MyBatis-Plus 的 IPage 转换而来。
     *
     * @param page        MyBatis-Plus 分页对象
     * @param entityClass 目标元素类型（用于逐条转换）
     * @param converter   实体 -> VO 的转换函数
     */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        List<T> voList = new ArrayList<>();
        for (E e : page.getRecords()) {
            voList.add(converter.apply(e));
        }
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(), voList);
    }
}

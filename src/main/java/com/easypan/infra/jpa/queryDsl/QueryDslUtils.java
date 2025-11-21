package com.easypan.infra.jpa.queryDsl;

import com.easypan.common.constants.Constants;
import com.easypan.web.dto.response.PaginationResultVo;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryDslUtils {

    @Resource
    private JPAQueryFactory queryFactory;

    /** predicate 防空包装 */
    private Predicate nullSafe(BooleanBuilder b) {
        if (b == null) return null;
        return b.hasValue() ? b : null;
    }

    /**
     * 统计符合条件的总数
     */
    public <T> long countByParam(EntityPathBase<T> from, BooleanBuilder predicate) {
        Long count = queryFactory.select(from.count())
                .from(from)
                .where(nullSafe(predicate))
                .fetchOne();
        return count != null ? count : 0L;
    }

    /**
     * 分页查询数据列表，用于查询投影（即 VO / DTO）
     */
    public <T, R> List<R> findListByParam(EntityPathBase<T> from,
                                          BooleanBuilder predicate,
                                          Integer offset,
                                          Integer limit,
                                          List<OrderSpecifier<?>> orders,
                                          Expression<R> projection) {
        JPAQuery<R> query = queryFactory.select(projection)
                .from(from)
                .where(nullSafe(predicate));

        if (orders != null && !orders.isEmpty()) {
            query.orderBy(orders.toArray(new OrderSpecifier<?>[0]));
        }

        int safeOffset = offset == null ? 0 : Math.max(offset, 0);
        int safeLimit = limit == null ? Constants.PAGE_SIZE : Math.max(limit, 1);

        // 分页
        return query.offset(safeOffset).limit(safeLimit).fetch();
    }

    /**
     * 分页查询数据列表，用于查询本体
     */
    public <T> List<T> findListByParam(EntityPathBase<T> from,
                                       BooleanBuilder predicate,
                                       Integer offset,
                                       Integer limit,
                                       List<OrderSpecifier<?>> orders) {
        return findListByParam(from, predicate, offset, limit, orders, from);
    }

    /**
     * 分页组合查询，用于查询投影
     */
    public <T, R> PaginationResultVo<R> findPageByParam(EntityPathBase<T> from,
                                                        BooleanBuilder predicate,
                                                        Integer pageNo,
                                                        Integer pageSize,
                                                        List<OrderSpecifier<?>> orders,
                                                        Expression<R> projection) {
        int pNo = pageNo == null ? 1 : Math.max(1, pageNo);
        int pSize = pageSize == null ? Constants.PAGE_SIZE : Math.max(1, pageSize);
        int offset = (pNo - 1) * pSize;

        long totalCount = countByParam(from, predicate);
        int totalPages = getPageTotal(totalCount, pSize);
        List<R> list = findListByParam(from, predicate, offset, pSize, orders, projection);
        return new PaginationResultVo<>(totalCount, pSize, pNo, totalPages, list);
    }


    /**
     * 分页组合查询，用于查询本体
     */
    public <T> PaginationResultVo<T> findPageByParam(EntityPathBase<T> from,
                                                     BooleanBuilder predicate,
                                                     Integer pageNo,
                                                     Integer pageSize,
                                                     List<OrderSpecifier<?>> orders) {
        return findPageByParam(from, predicate, pageNo, pageSize, orders, from);
    }

    /**
     * 计算总页数
     */
    private int getPageTotal(long totalCount, int pageSize) {
        if (totalCount == 0) return 0;
        return (int) ((totalCount + pageSize - 1) / pageSize);
    }

}
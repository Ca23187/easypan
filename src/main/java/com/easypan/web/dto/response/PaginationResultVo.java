package com.easypan.web.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PaginationResultVo<T> {
	private Long totalCount;
	private Integer pageSize;
	private Integer pageNo;
	private Integer pageTotal;
	private List<T> list = new ArrayList<>();

	public PaginationResultVo(Long totalCount, Integer pageSize, Integer pageNo, List<T> list) {
		this.totalCount = totalCount;
		this.pageSize = pageSize;
		this.pageNo = pageNo;
		this.list = list;
	}

    public PaginationResultVo(Long totalCount, Integer pageSize, Integer pageNo, Integer pageTotal, List<T> list) {
        if (pageNo == 0) {
            pageNo = 1;
        }
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.pageNo = pageNo;
        this.pageTotal = pageTotal;
        this.list = list;
    }

	public PaginationResultVo(List<T> list) {
		this.list = list;
	}
}

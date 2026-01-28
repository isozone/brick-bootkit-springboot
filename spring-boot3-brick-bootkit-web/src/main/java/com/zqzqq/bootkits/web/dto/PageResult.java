package com.zqzqq.bootkits.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应
 * 
 * @author brick-bootkit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private List<T> records;
    private long total;
    private int page;
    private int size;
    private int pages;
    
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        int pages = (int) Math.ceil((double) total / size);
        return new PageResult<>(records, total, page, size, pages);
    }
}

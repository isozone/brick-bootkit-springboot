package com.zqzqq.bootkits.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一 API 响应
 * 
 * @author brick-bootkit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data, System.currentTimeMillis());
    }
    
    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, "success", null, System.currentTimeMillis());
    }
    
    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null, System.currentTimeMillis());
    }
    
    public static <T> ApiResult<T> error(ErrorCode errorCode) {
        return new ApiResult<>(errorCode.getCode(), errorCode.getMessage(), null, System.currentTimeMillis());
    }
    
    public boolean isSuccess() {
        return code == 200;
    }
}

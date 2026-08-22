/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.zqzqq.bootkits.web.exception;

import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.integration.doctor.PluginFriendlyMessageResolver;
import com.zqzqq.bootkits.integration.doctor.PluginIssueDefinition;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationException;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理授权异常
     */
    @ExceptionHandler(PluginWebAuthorizationException.class)
    public ResponseEntity<ApiResult<Void>> handleAuthorizationException(PluginWebAuthorizationException e) {
        log.warn("Permission denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResult.error(ErrorCode.PERMISSION_DENIED.getCode(), e.getMessage()));
    }

    /**
     * 处理 PluginException
     */
    @ExceptionHandler(PluginException.class)
    public ResponseEntity<ApiResult<Void>> handlePluginException(PluginException e) {
        log.error("Plugin exception: {}", e.getMessage(), e);
        PluginFriendlyMessageResolver.Resolution resolution = PluginFriendlyMessageResolver.resolve(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(resolveErrorCode(resolution.getIssue()),
                        resolution.getMessage(),
                        resolution.getErrorKey(),
                        resolution.getHintPath(),
                        resolution.getHintAnchor()));
    }

    /**
     * 处理文件上传大小超出限制异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("File upload size exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResult.error(ErrorCode.PLUGIN_UPLOAD_FAILED.getCode(), "文件大小超出限制"));
    }

    /**
     * 处理 IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Invalid argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(ErrorCode.PARAM_INVALID.getCode(), e.getMessage()));
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception e) {
        log.error("System error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误: " + e.getMessage()));
    }

    private ErrorCode resolveErrorCode(PluginIssueDefinition issue) {
        if (issue == null) {
            return ErrorCode.SYSTEM_ERROR;
        }
        return switch (issue) {
            case MAIN_PACKAGE_MISSING -> ErrorCode.PLUGIN_MAIN_PACKAGE_MISSING;
            case PLUGIN_PATH_EMPTY, PLUGIN_PATH_INVALID, PLUGIN_PATH_NOT_DIRECTORY -> ErrorCode.PLUGIN_PATH_INVALID;
            case PLUGIN_PATH_MISSING -> ErrorCode.PLUGIN_PATH_MISSING;
            case PLUGIN_PATH_NOT_READABLE -> ErrorCode.PLUGIN_PATH_NOT_READABLE;
            case UPLOAD_TEMP_INVALID -> ErrorCode.PLUGIN_UPLOAD_TEMP_INVALID;
            case UPLOAD_TEMP_NOT_WRITABLE, UPLOAD_TEMP_CREATE_FAILED -> ErrorCode.PLUGIN_UPLOAD_TEMP_NOT_WRITABLE;
            case WEB_AUTHORIZER_MISSING -> ErrorCode.PLUGIN_WEB_AUTHORIZER_MISSING;
            case NO_PLUGINS_FOUND -> ErrorCode.PLUGIN_SCAN_EMPTY;
            case PLUGIN_MANAGER_UNAVAILABLE -> ErrorCode.PLUGIN_MANAGER_UNAVAILABLE;
            case PLUGIN_PACKAGE_INVALID -> ErrorCode.PLUGIN_FILE_INVALID;
        };
    }
}

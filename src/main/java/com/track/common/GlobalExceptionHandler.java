package com.track.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理 - 返回 HTTP 500 以便前端识别并展示错误信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleBadRequest(IllegalArgumentException e) {
        // 业务参数校验类错误：可将简短提示返回给前端
        String msg = e.getMessage() != null ? e.getMessage() : "请求参数错误";
        log.warn("请求参数错误: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error("系统异常，请稍后重试"));
    }
}

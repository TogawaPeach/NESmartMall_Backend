package com.nemall.auth.exception;

import com.nemall.common.response.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Void>> handle(
            ResponseStatusException exception) {

        // 取得异常携带的 HTTP 状态，例如 401
        int code = exception.getStatusCode().value();

        // 取得提示，例如“账号或密码错误”
        String message = exception.getReason();

        // 包装成项目统一的响应结构
        Result<Void> result = new Result<>(code, message, null);

        return ResponseEntity.status(code).body(result);
    }
}
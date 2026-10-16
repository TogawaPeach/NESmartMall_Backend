package com.nemall.cart.exception;

import com.nemall.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class CartExceptionHandler {

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequest(
            Exception exception) {

        String message = exception instanceof HttpMessageNotReadableException
                ? "请求体或字段格式不正确"
                : exception.getMessage();

        return ResponseEntity.badRequest().body(
                new Result<>(400, message, null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleServerError(
            Exception exception) {

        log.error("购物车请求处理失败", exception);

        return ResponseEntity.internalServerError().body(
                new Result<>(500, "购物车服务异常，请稍后重试", null)
        );
    }
}

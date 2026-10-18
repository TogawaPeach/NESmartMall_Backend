package com.nemall.order.exception;

import com.nemall.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(OrderBusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(
            OrderBusinessException exception) {

        return ResponseEntity.status(exception.getCode()).body(
                new Result<>(
                        exception.getCode(),
                        exception.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleBadRequest(
            IllegalArgumentException exception) {

        return ResponseEntity.badRequest().body(
                new Result<>(400, exception.getMessage(), null)
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<Void>> handleInvalidState(
            IllegalStateException exception) {

        log.error("订单数据或服务状态异常", exception);

        return ResponseEntity.internalServerError().body(
                new Result<>(500, "订单服务异常，请稍后重试", null)
        );
    }
}

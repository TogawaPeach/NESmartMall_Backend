package com.nemall.product.exception;

import com.nemall.common.response.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProductExceptionHandler {

    @ExceptionHandler(ProductBusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(
            ProductBusinessException exception) {

        Result<Void> result = new Result<>(
                exception.getCode(),
                exception.getMessage(),
                null
        );

        return ResponseEntity
                .status(exception.getCode())
                .body(result);
    }
}
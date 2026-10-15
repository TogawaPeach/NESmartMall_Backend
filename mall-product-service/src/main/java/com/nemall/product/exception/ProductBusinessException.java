package com.nemall.product.exception;

import lombok.Getter;

@Getter
public class ProductBusinessException extends RuntimeException {

    private final int code;

    public ProductBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
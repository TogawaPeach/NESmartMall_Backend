package com.nemall.order.exception;

import lombok.Getter;

@Getter
public class OrderBusinessException extends RuntimeException {

    private final int code;

    public OrderBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

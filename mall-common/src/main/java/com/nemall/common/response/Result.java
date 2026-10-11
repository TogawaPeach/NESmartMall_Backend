package com.nemall.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//统一封装相应结果
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {


    private Integer code;

    private String message;

    private T data;

}

package com.nemall.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应体，包含业务状态码、说明和具体数据。
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {


    /** 业务状态码；它是响应体字段，不会直接设置 HTTP 状态。 */
    private Integer code;

    /** 本次操作的结果说明。 */
    private String message;

    /** 具体响应数据，由各接口决定结构。 */
    private T data;

}

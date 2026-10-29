package com.nemall.support.dto;

import lombok.Data;

/** 工单号按字符串返回，避免浏览器丢失整数精度。 */
@Data
public class HandoffResultDTO {

    private String ticketNo;

    private String message;
}

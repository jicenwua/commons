package com.xcz.commons.core.domain;

import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * ResponseEntity
 */
@Data
public class ResponseEntity<T> {
    /***返回码**/
    private int code;
    /***返回信息**/
    private String msg;
    /***业务数据（单条或分页列表）**/
    private T data;
    /***总记录数**/
    private Long total;

    public boolean isOk() {
        return this.code == HttpStatus.OK.value();
    }
}

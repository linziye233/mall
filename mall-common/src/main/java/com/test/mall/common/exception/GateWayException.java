package com.test.mall.common.exception;



import com.test.mall.common.api.IErrorCode;
import lombok.Data;

@Data
public class GateWayException extends RuntimeException{

    private long code;

    private String message;

    public GateWayException(IErrorCode iErrorCode) {
        this.code = iErrorCode.getCode();
        this.message = iErrorCode.getMessage();
    }
}

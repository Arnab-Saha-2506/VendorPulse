package com.tcs.VendorPulse.common.error;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final List<ErrorResponse.FieldErrorDetail> details;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = Collections.emptyList();
    }

    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = Collections.emptyList();
    }

    public ApiException(ErrorCode errorCode, String customMessage, List<ErrorResponse.FieldErrorDetail> details) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = details != null ? details : Collections.emptyList();
    }
}


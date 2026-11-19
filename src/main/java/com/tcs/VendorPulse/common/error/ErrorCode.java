package com.tcs.VendorPulse.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Please log in again"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Account disabled"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Unauthorized access"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Field messages in details[]"),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "Invalid file"),
    TOKEN_INVALID_OR_EXPIRED(HttpStatus.BAD_REQUEST, "The reset authorization token is invalid or has expired"),
    EMAIL_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    REGISTRATION_NO_EXISTS(HttpStatus.CONFLICT, "Registration number already exists"),
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "Registration already processed"),
    CATEGORY_EXISTS(HttpStatus.CONFLICT, "Category exists"),
    MATERIAL_EXISTS(HttpStatus.CONFLICT, "Material exists"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "This change is not allowed for the current status"),
    NOT_YOUR_TURN(HttpStatus.CONFLICT, "Waiting for the other party"),
    NEGOTIATION_LIMIT_REACHED(HttpStatus.CONFLICT, "Negotiation limit reached"),
    REVISION_NOT_ALLOWED(HttpStatus.CONFLICT, "Revision not allowed"),
    CANCELLATION_NOT_ALLOWED(HttpStatus.CONFLICT, "Cancellation is no longer allowed for this purchase order"),
    SUPPLIER_NOT_ELIGIBLE(HttpStatus.UNPROCESSABLE_ENTITY, "Supplier not eligible"),
    INVALID_MAPPING(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid mapping"),
    QUANTITY_EXCEEDS_REMAINING(HttpStatus.UNPROCESSABLE_ENTITY, "Delivered quantity is more than the remaining quantity"),
    INVALID_CONFIGURATION(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid configuration"),
    INVALID_SNAPSHOT(HttpStatus.UNPROCESSABLE_ENTITY, "Stock data was refused; the last good data is kept"),
    INSUFFICIENT_DATA(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient historical data"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts, try again in a minute"),
    AGREEMENT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Agreement generation failed"),
    CONNECTOR_FAILURE(HttpStatus.SERVICE_UNAVAILABLE, "Inventory connector failure"),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "The verification code is incorrect"),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "The verification code has expired"),
    OTP_ALREADY_USED(HttpStatus.CONFLICT, "The verification code has already been used"),
    OTP_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Too many verification attempts"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Session not found"),
    SESSION_REVOKED(HttpStatus.UNAUTHORIZED, "Please log in again"),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "Please log in again"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}


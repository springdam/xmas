package com.xmas.game.app.web;

import com.xmas.game.common.error.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorBody> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiErrorBody(ex.getStatus(), ex.getMessage()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorBody> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("X-Session-Token".equalsIgnoreCase(ex.getHeaderName())) {
            return ResponseEntity.status(401).body(new ApiErrorBody(401, "Missing session token"));
        }
        return ResponseEntity.status(400).body(new ApiErrorBody(400, ex.getMessage()));
    }
}

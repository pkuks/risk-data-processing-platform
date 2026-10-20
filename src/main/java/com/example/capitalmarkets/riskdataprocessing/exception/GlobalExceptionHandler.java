package com.example.capitalmarkets.riskdataprocessing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handles missing 'businessDate' query parameters
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex
    ){
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "ERR_MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing."
        );

    }

    // 2. Handles malformed date inputs (e.g., passing '19-10-2026' instead of ISO '2026-10-19')
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' has an invalid value. Expected format is ISO Date (YYYY-MM-DD).", ex.getName());
        return buildResponse(HttpStatus.BAD_REQUEST, "ERR_INVALID_PARAMETER_FORMAT", message);
    }

    // 3. Handles general validation constraints (if you add @Valid annotations later)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException ex) {
        String defaultMessage = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Validation validation constraints failed.";
        return buildResponse(HttpStatus.BAD_REQUEST, "ERR_VALIDATION_FAILURE", defaultMessage);
    }

    // 4. Catch-all safety net for unexpected runtime engineering errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERR_INTERNAL_SERVER_ERROR",
                "An unexpected error occurred while launching or analyzing the batch: " + ex.getMessage()
        );
    }

    private ResponseEntity<Map<String,Object>> buildResponse(
            HttpStatus status, String errorCode, String message
    ){
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDate.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("errorCode", errorCode);
        body.put("message", message);
        return new ResponseEntity<>(body, status);

    }
}

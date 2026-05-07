package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleRequestBodyValidation(MethodArgumentNotValidException ex) {
        return validationResponse(ex.getBindingResult().getFieldErrors());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ValidationErrorResponse> handleFormValidation(BindException ex) {
        return validationResponse(ex.getBindingResult().getFieldErrors());
    }

    private ResponseEntity<ValidationErrorResponse> validationResponse(List<FieldError> fieldErrors) {
        List<ValidationFieldError> errors = fieldErrors.stream()
                .map(error -> new ValidationFieldError(
                        error.getField(),
                        error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
                ))
                .toList();

        String message = errors.isEmpty()
                ? "Validation failed"
                : "Validation failed: " + errors.stream()
                .map(error -> error.field() + " - " + error.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse("Validation failed");

        return ResponseEntity.badRequest().body(new ValidationErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                errors
        ));
    }

    public record ValidationErrorResponse(
            OffsetDateTime timestamp,
            int status,
            String error,
            String message,
            List<ValidationFieldError> validationErrors
    ) {
    }

    public record ValidationFieldError(String field, String message) {
    }
}

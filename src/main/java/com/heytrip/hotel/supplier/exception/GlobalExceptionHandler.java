package com.heytrip.hotel.supplier.exception;

import com.heytrip.common.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用程序中的各种异常
 * 
 * @author  Pax
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        logger.error("Business 处理业务异常: code={}, bizCode={} ,message={}", e.getCode(), e.getBizCode(),e.getMessage());
        Result  result =new Result(e.getCode(),e.getBizCode(), e.getMessage(),"",e.getData(),null);
        return result;
    }
    
    /**
     * 处理供应商异常
     */
    @ExceptionHandler(SupplierException.class)
    public ResponseEntity<ErrorResponse> handleSupplierException(SupplierException e) {
        logger.error("Supplier exception: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(e.getCode())
                .msg(e.getMessage())
                .supplierName(e.getSupplierName())
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        logger.warn("Validation exception: {}", e.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(400)
                .msg("Validation failed")
                .details(fieldErrors)
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        logger.warn("Bind exception: {}", e.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(400)
                .msg("Parameter binding failed")
                .details(fieldErrors)
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        logger.warn("Constraint violation exception: {}", e.getMessage());

        Map<String, String> violations = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(400)
                .msg("Constraint validation failed")
                .details(violations)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        logger.warn("Type mismatch exception: {}", e.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
                e.getValue(), e.getName(), e.getRequiredType().getSimpleName());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(400)
                .msg(message)
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 处理WebClient响应异常
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientResponseException(WebClientResponseException e) {
        logger.error("WebClient response exception: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(e.getRawStatusCode())
                .msg("External service error: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Illegal argument exception: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(400)
                .msg(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        logger.error("Runtime exception: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(500)
                .msg("Internal server error")
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.internalServerError().body(errorResponse);
    }
    
    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        logger.error("Unexpected exception: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(500)
                .msg("An unexpected error occurred")
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.internalServerError().body(errorResponse);
    }
}

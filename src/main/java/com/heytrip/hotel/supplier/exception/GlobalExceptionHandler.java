package com.heytrip.hotel.supplier.exception;

import com.heytrip.common.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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
 * @author Pax
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理供应商业务异常
     */
    @ExceptionHandler(SupplierException.class)
    public Result handleSupplierException(SupplierException e) {
        logger.error("Business 处理业务异常: supplierName={}, code={}, bizCode={} ,message={}", e.getSupplierName(), e.getCode(), e.getBizCode(), e.getMessage());
        Result result = new Result(e.getCode(), e.getBizCode(), e.getMessage(), "", e.getData(), null);
        return result;
    }


    /**
     * 处理通用系统异常
     */
    @ExceptionHandler(BasicException.class)
    public ResponseEntity<ErrorResponse> handleBasicException(BasicException e) {
        logger.warn("系统异常: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(500)
                .msg(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
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
                .msg("参数验证失败")
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
                .msg("参数映射失败")
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
        logger.warn("类型不匹配: {}", e.getMessage());

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
                .msg("第三方服务错误: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("非法参数: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(400)
                .msg(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 处理HTTP请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        logger.warn("请求方法不支持: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(405)
                .msg(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.status(405).body(errorResponse);
    }

    /**
     * 处理SpEL表达式评估异常
     */
    @ExceptionHandler(SpelEvaluationException.class)
    public ResponseEntity<ErrorResponse> handleSpelEvaluationException(SpelEvaluationException e) {
        logger.warn("SpEL表达式异常: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(400)
                .msg(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        logger.warn("非法状态: {}", e.getMessage());
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

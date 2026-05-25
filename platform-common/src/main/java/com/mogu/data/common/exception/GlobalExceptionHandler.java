package com.mogu.data.common.exception;

import com.mogu.data.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Map<String, Object>>> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        HttpStatus status = resolveStatus(e.getCode());
        return ResponseEntity.status(status).body(Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, Object>>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("参数校验异常: {}", e.getMessage(), e);

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        Map<String, Object> payload = new HashMap<>();
        payload.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Result<>("400", "参数校验失败", payload));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Map<String, Object>>> handleMissingParameterException(
            MissingServletRequestParameterException e) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("parameter", e.getParameterName());
        payload.put("type", e.getParameterType());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Result<>("400", "缺少必要参数", payload));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Map<String, Object>>> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error("500", "系统内部错误"));
    }

    private HttpStatus resolveStatus(String code) {
        try {
            int value = Integer.parseInt(code);
            return HttpStatus.resolve(value) != null ? HttpStatus.valueOf(value) : HttpStatus.INTERNAL_SERVER_ERROR;
        } catch (Exception e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}

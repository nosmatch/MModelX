package com.mogu.data.common.result;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果
 */
@Data
@NoArgsConstructor
public class Result<T> {

    private String code;
    private String message;
    private T data;

    public Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功返回
     */
    public static <T> Result<T> success(T data) {
        return new Result<>("200", "success", data);
    }

    /**
     * 成功返回（带消息）
     */
    public static <T> Result<T> success(T data, String message) {
        return new Result<>("200", message, data);
    }

    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>("200", "success", null);
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> error(String message) {
        return new Result<>("500", message, null);
    }

    /**
     * 失败返回（自定义错误码）
     */
    public static <T> Result<T> error(String code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败返回（带状态码）
     */
    public static <T> Result<T> error(int statusCode, String message) {
        return new Result<>(String.valueOf(statusCode), message, null);
    }
}
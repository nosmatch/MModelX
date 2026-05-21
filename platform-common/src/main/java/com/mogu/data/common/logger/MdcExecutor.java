package com.mogu.data.common.logger;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * MDC线程上下文工具
 * 用于异步任务中的日志上下文传递
 */
@Slf4j
public class MdcExecutor {

    /**
     * 包装Runnable，保留MDC上下文
     */
    public static Runnable wrap(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }

    /**
     * 包装Callable，保留MDC上下文
     */
    public static <T> Callable<T> wrap(Callable<T> callable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                return callable.call();
            } finally {
                MDC.clear();
            }
        };
    }
}
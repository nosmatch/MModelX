package com.mogu.data.common.logger;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 日志切面
 * 记录方法调用日志和性能指标
 */
@Slf4j
@Aspect
@Component
public class LoggerAspect {

    @Pointcut("execution(* com.mogu.data..service..*.*(..))")
    public void serviceLayer() {}

    @Pointcut("execution(* com.mogu.data..controller..*.*(..))")
    public void controllerLayer() {}

    @Around("serviceLayer() || controllerLayer()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        long startTime = System.currentTimeMillis();

        log.info("{}.{}() - Start - Args: {}", className, methodName, Arrays.toString(args));

        Object result;
        try {
            result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.info("{}.{}() - End - Result: {} - Execution time: {}ms",
                    className, methodName, result, executionTime);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.error("{}.{}() - Error - Exception: {} - Execution time: {}ms",
                    className, methodName, e.getMessage(), executionTime, e);

            throw e;
        }
    }
}
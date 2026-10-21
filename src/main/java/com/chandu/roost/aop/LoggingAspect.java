
package com.chandu.roost.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingAspect.class);

    @Around("@annotation(com.chandu.roost.aop.LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint)
            throws Throwable {

        long startTime = System.nanoTime();

        try {
            return joinPoint.proceed();
        } finally {
            long durationNanos =
                    System.nanoTime() - startTime;

            long durationMillis = durationNanos / 1_000_000;

            log.info("Method {} executed in {} ms",
                    joinPoint.getSignature().toShortString(),
                    durationMillis);
        }
    }
}

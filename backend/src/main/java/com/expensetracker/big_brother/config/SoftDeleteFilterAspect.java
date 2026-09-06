package com.expensetracker.big_brother.config;

import com.expensetracker.big_brother.common.BypassSoftDelete;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class SoftDeleteFilterAspect {

    private final EntityManager entityManager;

    public SoftDeleteFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Around("execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object processSoftDeleteFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        Session session = entityManager.unwrap(Session.class);
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        if (method.isAnnotationPresent(BypassSoftDelete.class)) {
            session.disableFilter("deletedFilter");
        } else {
            session.enableFilter("deletedFilter");
        }
        return joinPoint.proceed();
    }
}


package com.xcz.member.security.aspect;

import com.xcz.member.core.constant.SecurityConstants;
import com.xcz.member.core.utils.ServletUtils;
import com.xcz.member.core.utils.StringUtils;
import com.xcz.member.security.annotation.InnerAuth;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 内部调用注解
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InnerAuthAspect {

    @Around("@annotation(innerAuth)")
    public Object innerAuthAround(ProceedingJoinPoint proceedingJoinPoint, InnerAuth innerAuth) throws Throwable {
        HttpServletRequest request = ServletUtils.getRequest();
        String header = request.getHeader(SecurityConstants.FROM_SOURCE);
        if(StringUtils.isNotEmpty(header)){
            throw new IllegalAccessException("没有内部访问权限");
        }
        String token = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if(StringUtils.isEmpty(token)){
            throw new IllegalAccessException("用户未登录");
        }

        return proceedingJoinPoint.proceed();


    }
}

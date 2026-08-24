package com.xcz.commons.security.exception;

import com.xcz.commons.core.exception.InnerAuthException;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.exception.auth.NotPermissionException;
import com.xcz.commons.core.exception.auth.NotRoleException;
import com.xcz.commons.core.log.RequestLogAttributes;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.core.web.vo.params.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * 全局异常处理器 (Reactive 版本 - 给 Gateway 用)
 */
@Slf4j
@RestControllerAdvice
public class GlobalReactiveExceptionHandler {

    private void recordRequestException(ServerWebExchange exchange, Throwable e) {
        exchange.getAttributes().put(RequestLogAttributes.EXCEPTION, e);
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<AjaxResult> handleNotPermissionException(NotPermissionException e, ServerHttpRequest request,
                                                                   ServerWebExchange exchange) {
        recordRequestException(exchange, e);
        String requestURI = request.getPath().value();
        log.error("请求地址'{}',权限码校验失败'{}'", requestURI, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AjaxResult.error(HttpStatus.FORBIDDEN.value(), "没有访问权限，请联系管理员授权"));
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<AjaxResult> handleNotRoleException(NotRoleException e, ServerHttpRequest request,
                                                               ServerWebExchange exchange) {
        recordRequestException(exchange, e);
        String requestURI = request.getPath().value();
        log.error("请求地址'{}',角色权限校验失败'{}'", requestURI, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AjaxResult.error(HttpStatus.FORBIDDEN.value(), "没有访问权限，请联系管理员授权"));
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    public ResponseEntity<AjaxResult> handleMethodNotAllowedException(MethodNotAllowedException e, ServerHttpRequest request,
                                                                        ServerWebExchange exchange) {
        recordRequestException(exchange, e);
        String requestURI = request.getPath().value();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getSupportedMethods());
        return ResponseEntity.badRequest().body(AjaxResult.error(e.getMessage()));
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<AjaxResult> handleServiceException(ServiceException e, ServerWebExchange exchange) {
        recordRequestException(exchange, e);
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        return StringUtils.isNotNull(code) ?
                ResponseEntity.status(code).body(AjaxResult.error(code, e.getMessage())) :
                ResponseEntity.badRequest().body(AjaxResult.error(e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AjaxResult> handleRuntimeException(RuntimeException e, ServerHttpRequest request,
                                                             ServerWebExchange exchange) {
        recordRequestException(exchange, e);
        String requestURI = request.getPath().value();
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return ResponseEntity.internalServerError().body(AjaxResult.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> handleException(Exception e, ServerHttpRequest request, ServerWebExchange exchange) {
        recordRequestException(exchange, e);
        String requestURI = request.getPath().value();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return ResponseEntity.internalServerError().body(AjaxResult.error(e.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<AjaxResult> handleWebExchangeBindException(WebExchangeBindException e, ServerWebExchange exchange) {
        recordRequestException(exchange, e);
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(AjaxResult.error(message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<AjaxResult> handleBindException(BindException e, ServerWebExchange exchange) {
        recordRequestException(exchange, e);
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(AjaxResult.error(message));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<AjaxResult> handleServerWebInputException(ServerWebInputException e, ServerWebExchange exchange) {
        recordRequestException(exchange, e);
        log.error("参数解析失败", e);
        return ResponseEntity.badRequest().body(AjaxResult.error("请求参数格式错误: " + e.getReason()));
    }

    @ExceptionHandler(InnerAuthException.class)
    public ResponseEntity<AjaxResult> handleInnerAuthException(InnerAuthException e, ServerWebExchange exchange) {
        recordRequestException(exchange, e);
        return ResponseEntity.badRequest().body(AjaxResult.error(e.getMessage()));
    }
}

package com.xcz.commons.security.exception;


import com.xcz.commons.core.exception.InnerAuthException;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.exception.base.BaseException;
import com.xcz.commons.core.log.RequestLogAttributes;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.core.web.vo.params.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;


/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 将异常写入 request attribute，供请求日志拦截器在 afterCompletion 阶段读取。
     */
    private void recordRequestException(HttpServletRequest request, Throwable e) {
        request.setAttribute(RequestLogAttributes.EXCEPTION, e);
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<AjaxResult> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                          HttpServletRequest request) {
        recordRequestException(request, e);
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return ResponseEntity.badRequest().body(AjaxResult.error(e.getMessage()));
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<AjaxResult> handleServiceException(ServiceException e, HttpServletRequest request) {
        recordRequestException(request, e);
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        return StringUtils.isNotNull(code) ?
                ResponseEntity.status(code).body(AjaxResult.error(code, e.getMessage())) :
                ResponseEntity.badRequest().body(AjaxResult.error(e.getMessage()));
    }

    /**
     * 基础业务异常
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<AjaxResult> handleBaseException(BaseException e, HttpServletRequest request) {
        recordRequestException(request, e);
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生业务异常.'{}'", requestURI, e.getDefaultMessage());
        int code = e.getCode();
        String message = e.getDefaultMessage() != null ? e.getDefaultMessage() : e.getMessage();
        return code != 0 ?
                ResponseEntity.status(code).body(AjaxResult.error(code, message)) :
                ResponseEntity.badRequest().body(AjaxResult.error(message));
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AjaxResult> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        recordRequestException(request, e);
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return ResponseEntity.internalServerError().body(AjaxResult.error(e.getMessage()));
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> handleException(Exception e, HttpServletRequest request) {
        recordRequestException(request, e);
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return ResponseEntity.internalServerError().body(AjaxResult.error(e.getMessage()));
    }

    /**
     * 数据验证异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<AjaxResult> handleBindException(BindException e, HttpServletRequest request) {
        recordRequestException(request, e);
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().getFirst().getDefaultMessage();
        return ResponseEntity.badRequest().body(AjaxResult.error(message));
    }

    /**
     * Json参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AjaxResult> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                                            HttpServletRequest request) {
        recordRequestException(request, e);
        log.error(e.getMessage(), e);
        String message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        return ResponseEntity.badRequest().body(AjaxResult.error(message));
    }

    /**
     * 内部认证异常
     */
    @ExceptionHandler(InnerAuthException.class)
    public ResponseEntity<AjaxResult> handleInnerAuthException(InnerAuthException e, HttpServletRequest request) {
        recordRequestException(request, e);
        return ResponseEntity.badRequest().body(AjaxResult.error(e.getMessage()));
    }
}

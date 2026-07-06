package com.xcz.member.core.exception.file;

import com.xcz.member.core.exception.base.BaseException;

/**
 * 文件信息异常类
 */
public class FileException extends BaseException {
    private static final long serialVersionUID = 1L;

    public FileException(int code, Object[] args) {
        super("file", code, args, null);
    }

}

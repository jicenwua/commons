package com.xcz.member.core.exception.file;

import com.xcz.member.core.constant.Constants;

/**
 * 文件名大小限制异常类
 */
public class FileSizeLimitExceededException extends FileException {
    private static final long serialVersionUID = 1L;

    public FileSizeLimitExceededException(long defaultMaxSize) {
        super(Constants.FAIL, new Object[]{defaultMaxSize});
    }
}

package com.xcz.member.core.exception.file;

import com.xcz.member.core.constant.Constants;

/**
 * 文件名称超长限制异常类
 */
public class FileNameLengthLimitExceededException extends FileException {
    private static final long serialVersionUID = 1L;

    public FileNameLengthLimitExceededException(int defaultFileNameLength) {
        super(Constants.FAIL, new Object[]{defaultFileNameLength});
    }
}

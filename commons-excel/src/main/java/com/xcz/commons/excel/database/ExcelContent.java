package com.xcz.commons.excel.database;

public interface ExcelContent {

    //默认excel sheet
    String DEFAULT_SHEET = "版本";
    //版本（按照 . 分隔）
    String VERSION = "版本";
    //更新时间
    String TIMESTAMP = "更新时间";
    //版本注释
    String COMMENT = "版本注释";

    String  Suffix = ".xlsx";

    String FILE_PATH = "target";
}


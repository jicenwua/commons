package com.xcz.commons.excel.database;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Config {

    /** 配置标识 */
    private String key;

    /** 配置标题 */
    private String title;

    /** 配置值 */
    private String value;

    /** 数据类型 */
    private DataType dataType;

    /** 目标类名 */
    private String clazz;

    /** Excel 目录 */
    private String path;
}

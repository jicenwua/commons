package com.xcz.commons.excel.database.annotation;

import com.xcz.commons.excel.database.ExcelType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ExcelSheet {
    /** Sheet 名称 */
    String sheet() default "";

    /** 起始列 */
    int startList() default 0;

    /** 分组标题 */
    String comment() default "";

    /** 字段类型 */
    ExcelType type() default ExcelType.LIST;

    /** Map 类型 key 列名 */
    String mapKey() default "";

    /** Map 类型 value 列名 */
    String mapValue() default "";
}

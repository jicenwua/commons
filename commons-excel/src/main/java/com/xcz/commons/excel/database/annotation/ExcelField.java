package com.xcz.commons.excel.database.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelField {

    /** 列标题 */
    String label() default "";

    /** 列序号，从 1 开始 */
    int index() default 1;

    /** 列批注 */
    String comment() default "";

    /** 单元格为空时的默认值 */
    String value() default "";
}

package com.xcz.commons.excel.database.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExcelBase {
    /** 版本号 */
    String version() default "0.0.1";

    /** 文件前缀 */
    String name() default "";

    /** 版本注释 */
    String comment() default "";
}

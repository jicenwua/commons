package com.xcz.commons.database.utils;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.List;

/**
 * mybatis-plus代码生成工具
 */
public class GeneraCodeUtil {
    private final DataSourceConfig dataSourceConfig;

    public GeneraCodeUtil(String url, String username, String password) {
        this.dataSourceConfig = new DataSourceConfig.Builder(url, username, password).build();
    }

    /**
     * 获取调用者的根包名
     * 例如：如果调用者在 com.xcz.member.service.impl 包下，则返回 com.xcz.member
     *
     * @param rootPackageLevel 根包层级数（从后往前数的包段数）
     *                         例如：com.xcz.member.utils -> 传入2返回 com.xcz.member
     * @return 根包名
     */
    public static String getRootPackageName(int rootPackageLevel) {
        // 获取调用此方法的栈帧信息
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // stackTrace[0] = getStackTrace
        // stackTrace[1] = getRootPackageName
        // stackTrace[2] = 调用者方法
        if (stackTrace.length > 2) {
            String callerClassName = stackTrace[2].getClassName();
            String[] packages = callerClassName.split("\\.");

            // 从后往前截取指定层级的包名
            if (packages.length >= rootPackageLevel) {
                StringBuilder rootPackage = new StringBuilder();
                for (int i = 0; i < packages.length - rootPackageLevel; i++) {
                    if (i > 0) {
                        rootPackage.append(".");
                    }
                    rootPackage.append(packages[i]);
                }
                return rootPackage.toString();
            }
        }

        // 默认返回 com.xcz.member
        return "com.xcz.member";
    }

    /**
     * 获取调用者的根包名（默认取前3段）
     * 例如：com.xcz.member.utils -> com.xcz.member
     */
    public static String getRootPackageName() {
        return getRootPackageName(2); // 去掉最后2段（如 .utils.GeneraCodeUtil）
    }


    /**
     * 生成单个实体类
     *
     * @param tableNames 表名
     */
    public void generate(List<String> tableNames) {

        // 使用 FastAutoGenerator 快速配置代码生成器
        FastAutoGenerator.create(dataSourceConfig.getUrl(), dataSourceConfig.getUsername(), dataSourceConfig.getPassword())
                .globalConfig(builder -> {
                    builder.outputDir(System.getProperty("user.dir") + "/src/main/java") // 输出目录
                            .disableOpenDir()     // 禁止生成后打开文件夹
                            .commentDate("yyyy-MM-dd"); // 注释日期格式
                })
                .packageConfig(builder -> {
                    builder.parent(getRootPackageName()) // 设置父包名
                            .entity("model") // 设置实体类包名
                            .mapper("mapper") // 设置 Mapper 接口包名
                            .service("service") // 设置 Service 接口包名
                            .serviceImpl("service.impl") // 设置 Service 实现类包名
                            .xml("mapper"); // 设置 Mapper XML 文件包名
                })
                .strategyConfig(builder -> {
                    builder.addInclude(tableNames.toArray(new String[0])) // 设置需要生成的表名
                            .entityBuilder()
                            .enableLombok()    // 启用 Lombok
                            .naming(NamingStrategy.underline_to_camel)   // 表名下划线转驼峰
                            .columnNaming(NamingStrategy.underline_to_camel) // 字段下划线转驼峰
                            .build()
                            .controllerBuilder()
                            .enableRestStyle()   // @RestController
                            .enableHyphenStyle() // url 中驼峰转连字符
                            .build()
                            .serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl")
                            .build()
                            .mapperBuilder()
                            .enableBaseResultMap() // 生成通用 resultMap
                            .enableBaseColumnList() // 生成通用 columnList
                    ;
                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用 Freemarker 模板引擎
                .execute(); // 执行生成


    }
}

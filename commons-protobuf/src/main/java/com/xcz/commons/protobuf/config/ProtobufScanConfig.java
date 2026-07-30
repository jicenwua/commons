package com.xcz.commons.protobuf.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

/**
 * .proto 文件生成扫描配置。
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProtobufScanConfig {

    /** 要扫描的实体包名列表 */
    List<String> scanPackages = new ArrayList<>();

    /**
     * protoc 生成 Java 类的包名。
     * 默认：第一个扫描包 + ".proto"
     */
    String protoJavaPackage;

    /** .proto 文件输出目录，默认 target/generated-proto */
    String outputDirectory = "target/generated-proto";

    /**
     * 解析 protoc 生成 Java 类的包名，未配置时默认「第一个扫描包 + .proto」。
     */
    public String resolveProtoJavaPackage() {
        if (protoJavaPackage != null && !protoJavaPackage.isBlank()) {
            return protoJavaPackage;
        }
        if (scanPackages.isEmpty()) {
            throw new IllegalStateException("scanPackages 不能为空");
        }
        return scanPackages.getFirst() + ".proto";
    }
}

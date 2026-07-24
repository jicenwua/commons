package com.xcz.commons.protobuf.example;

import com.xcz.commons.protobuf.generator.ProtobufGenerator;

import java.util.List;

/**
 * 从 {@code @ProtobufMessage} 实体生成 .proto 文件的示例（可在 IDE 中直接运行 main 方法）。
 *
 * <p>运行前请先编译本模块（Build Project），确保 {@code SampleOrder} 等测试实体已在 classpath 中。
 */
public final class ProtobufCodegenMain {

    private ProtobufCodegenMain() {
    }

    public static void main(String[] args) throws Exception {
        List<String> scanPackages = List.of("com.xcz.commons.protobuf.model");
        String protoOutputDir = "target/example-codegen/proto";
        String protoJavaPackage = "com.xcz.commons.protobuf.model.proto";

        var protoFiles = ProtobufGenerator.generate(scanPackages, protoOutputDir, protoJavaPackage);

        System.out.println("========================================");
        System.out.println("已从实体生成 .proto");
        System.out.println("扫描包    : " + scanPackages);
        System.out.println("输出目录  : " + ProtobufGenerator.resolveOutputPath(protoOutputDir));
        protoFiles.keySet().forEach(name -> System.out.println("  - " + name));
        System.out.println("----------------------------------------");
        System.out.println("生成内容:");
        protoFiles.values().forEach(content -> System.out.println(content.strip()));
        System.out.println("========================================");
        System.out.println("Kafka 编解码请使用各中间件模块适配（如 kafka 模块的 KafkaProtobufAutoConfiguration）");
    }
}

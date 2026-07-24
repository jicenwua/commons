package com.xcz.commons.protobuf.generator;

import com.xcz.commons.protobuf.config.ProtobufScanConfig;

import java.util.List;

/**
 * 通用命令行入口（也可在 IDE 中直接运行）。
 *
 * <p>推荐在业务模块中创建自己的 XxxProtoGeneratorMain，见 kafka 模块示例。
 */
public final class ProtoGeneratorMain {

    private ProtoGeneratorMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        List<String> packages = List.of(args[0].split(","));
        String outputDir = args.length >= 2 ? args[1] : "target/generated-proto";
        String protoJavaPackage = args.length >= 3 ? args[2] : null;

        run(packages, outputDir, protoJavaPackage);
    }

    /**
     * 在代码中直接调用，适合业务项目自定义 main 方法。
     */
    public static void run(List<String> scanPackages, String outputDirectory, String protoJavaPackage) throws Exception {
        ProtobufScanConfig config = ProtobufGenerator.buildConfig(scanPackages, outputDirectory, protoJavaPackage);
        new ProtoSchemaGenerator().generateToDirectory(config);

        System.out.println("========================================");
        System.out.println("Protobuf 生成完成");
        System.out.println("扫描包    : " + scanPackages);
        System.out.println("输出目录  : " + ProtobufGenerator.resolveOutputPath(config.getOutputDirectory()));
        System.out.println("Java 包名 : " + config.resolveProtoJavaPackage());
        System.out.println("========================================");
        System.out.println("可将 .proto 分发给其他服务，或仅作协议文档使用");
    }

    private static void printUsage() {
        System.err.println("用法:");
        System.err.println("  java ProtoGeneratorMain <package1,package2> [outputDir] [protoJavaPackage]");
        System.err.println();
        System.err.println("示例:");
        System.err.println("  java ProtoGeneratorMain com.mq.kafka.config");
        System.err.println("  java ProtoGeneratorMain com.mq.kafka.config target/generated-proto com.mq.kafka.config.proto");
    }
}

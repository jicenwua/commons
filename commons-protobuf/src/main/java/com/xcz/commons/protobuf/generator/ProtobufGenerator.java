package com.xcz.commons.protobuf.generator;

import com.xcz.commons.protobuf.config.ProtobufScanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Protobuf 文件主动生成入口。
 * 在业务项目中创建 main 方法，填入扫描包列表和输出目录后手动运行即可。
 */
public final class ProtobufGenerator {

    private static final Logger log = LoggerFactory.getLogger(ProtobufGenerator.class);

    private ProtobufGenerator() {
    }

    /**
     * 扫描指定包并生成 .proto 文件到目标目录。
     *
     * @param scanPackages     需要扫描的实体包名列表（只处理带 @ProtobufMessage 的类）
     * @param outputDirectory  输出目录，默认建议使用 {@code target/generated-proto}
     * @param protoJavaPackage protoc 生成 Java 类的包名，传 null 则使用「第一个扫描包 + .proto」
     * @return 生成的文件名 → 内容
     */
    public static Map<String, String> generate(
            List<String> scanPackages,
            String outputDirectory,
            String protoJavaPackage) throws IOException {
        ProtobufScanConfig config = buildConfig(scanPackages, outputDirectory, protoJavaPackage);
        ProtoSchemaGenerator generator = new ProtoSchemaGenerator();
        Map<String, String> files = generator.generate(config);

        Path outputDir = resolveOutputPath(config.getOutputDirectory());
        Files.createDirectories(outputDir);
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path filePath = outputDir.resolve(entry.getKey());
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, entry.getValue());
        }
        logGenerationSummary(scanPackages, outputDir, files);
        return files;
    }

    private static void logGenerationSummary(
            List<String> scanPackages,
            Path outputDir,
            Map<String, String> files) {
        log.info("Protobuf 生成完成");
        log.info("扫描包    : {}", scanPackages);
        log.info("输出目录  : {}", outputDir);
        log.info("生成文件数: {}", files.size());
        files.keySet().stream().sorted().forEach(fileName -> log.info("  - {}", fileName));
    }

    /** 使用默认输出目录 {@code target/generated-proto} 生成。 */
    public static Map<String, String> generate(List<String> scanPackages, String protoJavaPackage) throws IOException {
        return generate(scanPackages, "target/generated-proto", protoJavaPackage);
    }

    /** 使用默认输出目录，proto 包名也自动推导。 */
    public static Map<String, String> generate(List<String> scanPackages) throws IOException {
        return generate(scanPackages, "target/generated-proto", null);
    }

    public static ProtobufScanConfig buildConfig(
            List<String> scanPackages,
            String outputDirectory,
            String protoJavaPackage) {
        if (scanPackages == null || scanPackages.isEmpty()) {
            throw new IllegalArgumentException("scanPackages 不能为空");
        }
        ProtobufScanConfig config = new ProtobufScanConfig();
        config.setScanPackages(scanPackages);
        if (outputDirectory != null && !outputDirectory.isBlank()) {
            config.setOutputDirectory(outputDirectory);
        }
        config.setProtoJavaPackage(protoJavaPackage);
        return config;
    }

    public static Path resolveOutputPath(String outputDirectory) {
        return Path.of(outputDirectory).toAbsolutePath().normalize();
    }
}

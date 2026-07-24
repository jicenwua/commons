package com.xcz.commons.protobuf.generator;

import com.xcz.commons.protobuf.config.ProtobufScanConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtoSchemaGeneratorTest {

    @Test
    void shouldGenerateProtoFromAnnotatedClass() {
        ProtobufScanConfig config = new ProtobufScanConfig();
        config.setScanPackages(List.of("com.xcz.commons.protobuf.model"));
        config.setProtoJavaPackage("com.xcz.commons.protobuf.model.proto");

        Map<String, String> files = new ProtoSchemaGenerator().generate(config);
        String content = files.values().stream()
                .filter(file -> file.contains("message SampleOrder {"))
                .findFirst()
                .orElseThrow();

        assertTrue(content.contains("message SampleOrder"));
        assertTrue(content.contains("int64 order_id"));
        assertTrue(content.contains("int64 user_id"));
        assertTrue(!content.contains("user_i_d"));
        assertTrue(content.contains("string amount"));
    }

    @Test
    void shouldGenerateFieldCommentFromAnnotation() {
        ProtobufScanConfig config = new ProtobufScanConfig();
        config.setScanPackages(List.of("com.xcz.commons.protobuf.model"));
        config.setProtoJavaPackage("com.xcz.commons.protobuf.model.proto");

        Map<String, String> files = new ProtoSchemaGenerator().generate(config);
        String content = files.values().stream()
                .filter(file -> file.contains("message SampleOrder {"))
                .findFirst()
                .orElseThrow();

        assertTrue(content.contains("// 订单主键"));
        assertTrue(content.contains("int64 order_id"));
    }
}
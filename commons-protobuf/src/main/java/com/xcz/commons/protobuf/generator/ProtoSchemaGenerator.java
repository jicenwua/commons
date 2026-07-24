package com.xcz.commons.protobuf.generator;

import com.xcz.commons.protobuf.annotation.ProtobufField;
import com.xcz.commons.protobuf.annotation.ProtobufMessage;
import com.xcz.commons.protobuf.config.ProtobufScanConfig;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProtoSchemaGenerator {

    public Map<String, String> generate(ProtobufScanConfig config) {
        Set<Class<?>> classes = ClassScanner.scanPackages(config.getScanPackages());
        if (classes.isEmpty()) {
            throw new IllegalStateException("未扫描到 @ProtobufMessage 标记的类，packages=" + config.getScanPackages());
        }

        Map<String, List<Class<?>>> fileGroups = new LinkedHashMap<>();
        for (Class<?> clazz : classes) {
            String fileName = resolveFileName(clazz);
            fileGroups.computeIfAbsent(fileName, key -> new ArrayList<>()).add(clazz);
        }

        Map<String, String> protoFiles = new LinkedHashMap<>();
        String javaPackage = config.resolveProtoJavaPackage();
        for (Map.Entry<String, List<Class<?>>> entry : fileGroups.entrySet()) {
            protoFiles.put(entry.getKey(), buildProtoFile(entry.getValue(), javaPackage));
        }
        return protoFiles;
    }

    public void generateToDirectory(ProtobufScanConfig config) throws IOException {
        Path outputDir = Path.of(config.getOutputDirectory());
        Files.createDirectories(outputDir);
        Map<String, String> protoFiles = generate(config);
        for (Map.Entry<String, String> entry : protoFiles.entrySet()) {
            Files.writeString(outputDir.resolve(entry.getKey()), entry.getValue());
        }
    }

    private String buildProtoFile(List<Class<?>> classes, String javaPackage) {
        StringBuilder sb = new StringBuilder();
        sb.append("syntax = \"proto3\";\n\n");
        sb.append("option java_package = \"").append(javaPackage).append("\";\n");
        sb.append("option java_multiple_files = true;\n\n");

        List<Class<?>> sorted = new ArrayList<>(classes);
        sorted.sort(Comparator.comparing(Class::getSimpleName));

        List<Class<?>> enums = sorted.stream().filter(Class::isEnum).toList();
        List<Class<?>> messages = sorted.stream().filter(clazz -> !clazz.isEnum()).toList();

        for (Class<?> enumType : enums) {
            appendEnum(sb, enumType);
            sb.append('\n');
        }
        for (Class<?> messageType : messages) {
            appendMessage(sb, messageType);
            sb.append('\n');
        }
        return sb.toString();
    }

    private void appendEnum(StringBuilder sb, Class<?> enumType) {
        ProtobufMessage annotation = enumType.getAnnotation(ProtobufMessage.class);
        String enumName = annotation != null && !annotation.name().isBlank()
                ? annotation.name() : enumType.getSimpleName();
        if (annotation != null && !annotation.comment().isBlank()) {
            sb.append("// ").append(annotation.comment()).append('\n');
        }
        sb.append("enum ").append(enumName).append(" {\n");
        sb.append("  UNSPECIFIED = 0;\n");
        Object[] constants = enumType.getEnumConstants();
        for (int i = 0; i < constants.length; i++) {
            Enum<?> constant = (Enum<?>) constants[i];
            sb.append("  ").append(constant.name()).append(" = ").append(i + 1).append(";\n");
        }
        sb.append("}\n");
    }

    private void appendMessage(StringBuilder sb, Class<?> messageType) {
        ProtobufMessage annotation = messageType.getAnnotation(ProtobufMessage.class);
        String messageName = annotation != null && !annotation.name().isBlank()
                ? annotation.name() : messageType.getSimpleName();
        if (annotation != null && !annotation.comment().isBlank()) {
            sb.append("// ").append(annotation.comment()).append('\n');
        }
        sb.append("message ").append(messageName).append(" {\n");

        List<Field> fields = collectFields(messageType);
        int autoNumber = 1;
        for (Field field : fields) {
            ProtoTypeMapper.ProtoFieldDescriptor descriptor = ProtoTypeMapper.describeField(field);
            if (descriptor == null) {
                continue;
            }
            int fieldNumber = resolveFieldNumber(field, autoNumber++);
            appendFieldComment(sb, field);
            sb.append("  ").append(descriptor.protoType()).append(' ')
                    .append(descriptor.protoName()).append(" = ")
                    .append(fieldNumber).append(";\n");
        }
        sb.append("}\n");
    }

    private List<Field> collectFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        || java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private int resolveFieldNumber(Field field, int autoNumber) {
        ProtobufField annotation = field.getAnnotation(ProtobufField.class);
        if (annotation != null && annotation.number() > 0) {
            return annotation.number();
        }
        return autoNumber;
    }

    private void appendFieldComment(StringBuilder sb, Field field) {
        ProtobufField annotation = field.getAnnotation(ProtobufField.class);
        if (annotation != null && !annotation.comment().isBlank()) {
            sb.append("  // ").append(annotation.comment()).append('\n');
        }
    }

    private String resolveFileName(Class<?> clazz) {
        String pkg = clazz.getPackageName();
        String relative = pkg.replace('.', '/');
        return relative + "/" + clazz.getSimpleName().toLowerCase() + ".proto";
    }
}

package com.xcz.commons.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 日志脱敏：屏蔽密码、令牌等敏感字段。
 */
public final class LogSanitizer {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "paypassword", "pay_password", "oldpassword", "newpassword",
            "token", "accesstoken", "refreshtoken", "authorization", "secret"
    );

    private LogSanitizer() {
    }

    public static String sanitizeJson(String json, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(json)) {
            return json;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            sanitizeNode(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception ignored) {
            return json;
        }
    }

    public static Object sanitizeObject(Object value, ObjectMapper objectMapper) {
        if (value == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.valueToTree(value);
            sanitizeNode(root);
            return objectMapper.treeToValue(root, Object.class);
        } catch (Exception ignored) {
            return value;
        }
    }

    private static void sanitizeNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (isSensitiveKey(entry.getKey())) {
                    objectNode.put(entry.getKey(), "***");
                } else {
                    sanitizeNode(entry.getValue());
                }
            }
            return;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode child : arrayNode) {
                sanitizeNode(child);
            }
        }
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(normalized);
    }
}

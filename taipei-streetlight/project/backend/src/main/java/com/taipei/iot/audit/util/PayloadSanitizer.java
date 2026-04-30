package com.taipei.iot.audit.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class PayloadSanitizer {

    private static final Set<String> SENSITIVE_FIELDS =
            Set.of("secret", "password", "newPassword", "token", "accessToken", "refreshToken");

    private static final String MASK = "***";
    private static final int MAX_LENGTH = 2000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PayloadSanitizer() {}

    public static String sanitize(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            String json = MAPPER.writeValueAsString(args);
            JsonNode node = MAPPER.readTree(json);
            maskSensitiveFields(node);
            String result = MAPPER.writeValueAsString(node);
            if (result.length() > MAX_LENGTH) {
                return result.substring(0, MAX_LENGTH);
            }
            return result;
        } catch (Exception e) {
            return "[sanitize-error]";
        }
    }

    private static void maskSensitiveFields(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (SENSITIVE_FIELDS.contains(entry.getKey())) {
                    obj.put(entry.getKey(), MASK);
                } else {
                    maskSensitiveFields(entry.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskSensitiveFields(child);
            }
        }
    }
}

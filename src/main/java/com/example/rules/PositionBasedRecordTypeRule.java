package com.example.rules;

import java.util.Map;

public class PositionBasedRecordTypeRule implements RecordTypeRule {
    private final int start;
    private final int length;
    private final Map<String, String> mapping;

    public PositionBasedRecordTypeRule(int start, int length, Map<String, String> mapping) {
        this.start = start;
        this.length = length;
        this.mapping = mapping;
    }

    @Override
    public String getType(String line) {
        if (line.length() < start - 1 + length) return "UNKNOWN";
        String key = line.substring(start - 1, start - 1 + length);
        return mapping.getOrDefault(key, "BODY");
    }
}

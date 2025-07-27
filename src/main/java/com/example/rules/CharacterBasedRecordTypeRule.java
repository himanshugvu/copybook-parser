package com.example.rules;

import java.util.Map;

public class CharacterBasedRecordTypeRule implements RecordTypeRule {
    private final int start;
    private final Map<String, String> mapping;

    public CharacterBasedRecordTypeRule(int start, Map<String, String> mapping) {
        this.start = start;
        this.mapping = mapping;
    }

    @Override
    public String getType(String line) {
        if (line.length() < start) return "UNKNOWN";
        String key = line.substring(start - 1, start);
        return mapping.getOrDefault(key, "BODY");
    }
}

package com.example.rules;

import java.util.List;

public class ConditionalBasedRecordTypeRule implements RecordTypeRule {
    public record Condition(String equals, int at, String result) {}

    private final List<Condition> conditions;
    private final String defaultType;

    public ConditionalBasedRecordTypeRule(List<Condition> conditions, String defaultType) {
        this.conditions = conditions;
        this.defaultType = defaultType;
    }

    @Override
    public String getType(String line) {
        for (var c : conditions) {
            if (line.length() >= c.at() && line.substring(c.at() - 1, c.at()).equals(c.equals())) {
                return c.result();
            }
        }
        return defaultType;
    }
}

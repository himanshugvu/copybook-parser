package com.example.rules;

@FunctionalInterface
public interface RecordTypeRule {
    String getType(String line);
}

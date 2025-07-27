package com.example.rules;

public class NoRecordTypeRule implements RecordTypeRule {
    @Override
    public String getType(String line) {
        return "BODY";
    }
}

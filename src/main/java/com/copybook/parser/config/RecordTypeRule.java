package com.copybook.parser.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordTypeRule {

    // Position-based configuration
    private Integer start;
    private Integer length;
    private Map<String, String> values = new HashMap<>();
    private String defaultType = "DATA";

    // Character-based configuration
    private boolean caseSensitive = false;
    private List<String> validCharacters;

    // Conditional-based configuration
    private String conditionalLogic;
    private Map<String, String> conditionalMappings;
    private String conditionalExpression;

    // Pattern-based configuration
    private String pattern;
    private Map<String, String> patternMappings;

    // Multi-field configuration
    private List<FieldRule> multiFieldRules;

    // Advanced options
    private boolean trimValues = true;
    private boolean ignoreCase = false;
    private String fallbackStrategy = "default"; // default, error, skip

    public static RecordTypeRule createDefault() {
        var rule = new RecordTypeRule();
        rule.setStart(1);
        rule.setLength(2);
        rule.getValues().put("00", "HEADER");
        rule.getValues().put("01", "DATA");
        rule.getValues().put("99", "TRAILER");
        rule.setDefaultType("DATA");
        return rule;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldRule {
        private String name;
        private Integer start;
        private Integer length;
        private String expectedValue;
        private String recordType;
        private boolean required = true;
    }
}
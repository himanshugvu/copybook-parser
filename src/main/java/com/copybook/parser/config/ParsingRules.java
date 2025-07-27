package com.copybook.parser.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParsingRules {

    private String ruleType = "position-based";
    private RecordTypeRule recordTypeField;
    private FieldProcessingRule fieldProcessing = new FieldProcessingRule();
    private LayoutGenerationRule layoutGeneration = new LayoutGenerationRule();
    private ValidationRule validation = new ValidationRule();

    public static ParsingRules createDefault() {
        var rules = new ParsingRules();
        rules.setRuleType("position-based");
        rules.setRecordTypeField(RecordTypeRule.createDefault());
        return rules;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationRule {
        private boolean strictMode = false;
        private boolean validateFieldOverlaps = true;
        private boolean validateRedefines = true;
        private boolean allowEmptyFields = false;
        private List<String> customValidationRules;
    }
}
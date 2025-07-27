package com.copybook.parser.engine;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.config.RecordTypeRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RuleInterpreter {

    public void validateRules(ParsingRules rules) {
        log.debug("Validating parsing rules of type: {}", rules.getRuleType());

        switch (rules.getRuleType().toLowerCase()) {
            case "position-based" -> validatePositionBasedRules(rules.getRecordTypeField());
            case "character-based" -> validateCharacterBasedRules(rules.getRecordTypeField());
            case "conditional-based" -> validateConditionalBasedRules(rules.getRecordTypeField());
            case "pattern-based" -> validatePatternBasedRules(rules.getRecordTypeField());
            case "multi-field" -> validateMultiFieldRules(rules.getRecordTypeField());
            case "no-record-type" -> validateNoRecordTypeRules(rules.getRecordTypeField());
            default -> throw new IllegalArgumentException("Unsupported rule type: " + rules.getRuleType());
        }

        validateFieldProcessingRules(rules);
        validateLayoutGenerationRules(rules);
    }

    private void validatePositionBasedRules(RecordTypeRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Position-based rules require recordTypeField configuration");
        }
        if (rule.getStart() == null || rule.getLength() == null) {
            throw new IllegalArgumentException("Position-based rules require start and length");
        }
        if (rule.getStart() < 1) {
            throw new IllegalArgumentException("Start position must be >= 1");
        }
        if (rule.getLength() < 1) {
            throw new IllegalArgumentException("Length must be >= 1");
        }
        if (rule.getValues().isEmpty()) {
            throw new IllegalArgumentException("Position-based rules require value mappings");
        }
    }

    private void validateCharacterBasedRules(RecordTypeRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Character-based rules require recordTypeField configuration");
        }
        if (rule.getStart() == null) {
            throw new IllegalArgumentException("Character-based rules require start position");
        }
        if (rule.getValues().isEmpty() && rule.getValidCharacters() == null) {
            throw new IllegalArgumentException("Character-based rules require value mappings or valid characters");
        }
    }

    private void validateConditionalBasedRules(RecordTypeRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Conditional-based rules require recordTypeField configuration");
        }
        if (rule.getConditionalLogic() == null && rule.getConditionalExpression() == null) {
            throw new IllegalArgumentException("Conditional-based rules require conditional logic or expression");
        }
    }

    private void validatePatternBasedRules(RecordTypeRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Pattern-based rules require recordTypeField configuration");
        }
        if (rule.getPattern() == null) {
            throw new IllegalArgumentException("Pattern-based rules require pattern definition");
        }
        if (rule.getPatternMappings() == null || rule.getPatternMappings().isEmpty()) {
            throw new IllegalArgumentException("Pattern-based rules require pattern mappings");
        }
    }

    private void validateMultiFieldRules(RecordTypeRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Multi-field rules require recordTypeField configuration");
        }
        if (rule.getMultiFieldRules() == null || rule.getMultiFieldRules().isEmpty()) {
            throw new IllegalArgumentException("Multi-field rules require field rule definitions");
        }
    }

    private void validateNoRecordTypeRules(RecordTypeRule rule) {
        log.debug("No record type validation - all lines treated as data records");
    }

    private void validateFieldProcessingRules(ParsingRules rules) {
        var fieldRules = rules.getFieldProcessing();
        if (fieldRules.getRecordLevels().isEmpty()) {
            throw new IllegalArgumentException("At least one record level must be specified");
        }
    }

    private void validateLayoutGenerationRules(ParsingRules rules) {
        var layoutRules = rules.getLayoutGeneration();
        if (layoutRules.getStartPosition() < 0) {
            throw new IllegalArgumentException("Start position cannot be negative");
        }
        if (!layoutRules.getStrategy().matches("single_layout|multi_layout|custom")) {
            throw new IllegalArgumentException("Invalid layout strategy: " + layoutRules.getStrategy());
        }
    }
}

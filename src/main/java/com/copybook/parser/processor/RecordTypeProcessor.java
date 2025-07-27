package com.copybook.parser.processor;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.config.RecordTypeRule;
import com.copybook.parser.model.RecordTypeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
@Slf4j
public class RecordTypeProcessor {

    public RecordTypeResult process(List<String> lines, ParsingRules rules) {
        log.debug("Processing record types with strategy: {}", rules.getRuleType());

        return switch (rules.getRuleType().toLowerCase()) {
            case "position-based" -> processPositionBased(lines, rules.getRecordTypeField());
            case "character-based" -> processCharacterBased(lines, rules.getRecordTypeField());
            case "conditional-based" -> processConditionalBased(lines, rules.getRecordTypeField());
            case "pattern-based" -> processPatternBased(lines, rules.getRecordTypeField());
            case "multi-field" -> processMultiField(lines, rules.getRecordTypeField());
            case "no-record-type" -> processNoRecordType(lines);
            default -> throw new IllegalArgumentException("Unsupported rule type: " + rules.getRuleType());
        };
    }

    private RecordTypeResult processPositionBased(List<String> lines, RecordTypeRule rule) {
        var recordsByType = new HashMap<String, List<String>>();
        var recordCounts = new HashMap<String, Integer>();
        var unrecognizedLines = new ArrayList<String>();

        for (var line : lines) {
            if (isCommentOrEmpty(line)) continue;

            var recordType = extractRecordTypeByPosition(line, rule);
            if (recordType == null) {
                unrecognizedLines.add(line);
                recordType = rule.getDefaultType();
            }

            recordsByType.computeIfAbsent(recordType, k -> new ArrayList<>()).add(line);
            recordCounts.merge(recordType, 1, Integer::sum);
        }

        return RecordTypeResult.builder()
                .recordsByType(recordsByType)
                .processingMethod("position-based")
                .recordCounts(recordCounts)
                .unrecognizedLines(unrecognizedLines)
                .build();
    }

    private RecordTypeResult processCharacterBased(List<String> lines, RecordTypeRule rule) {
        var recordsByType = new HashMap<String, List<String>>();
        var recordCounts = new HashMap<String, Integer>();
        var unrecognizedLines = new ArrayList<String>();

        for (var line : lines) {
            if (isCommentOrEmpty(line)) continue;

            var recordType = extractRecordTypeByCharacter(line, rule);
            if (recordType == null) {
                unrecognizedLines.add(line);
                recordType = rule.getDefaultType();
            }

            recordsByType.computeIfAbsent(recordType, k -> new ArrayList<>()).add(line);
            recordCounts.merge(recordType, 1, Integer::sum);
        }

        return RecordTypeResult.builder()
                .recordsByType(recordsByType)
                .processingMethod("character-based")
                .recordCounts(recordCounts)
                .unrecognizedLines(unrecognizedLines)
                .build();
    }

    private RecordTypeResult processConditionalBased(List<String> lines, RecordTypeRule rule) {
        var recordsByType = new HashMap<String, List<String>>();
        var recordCounts = new HashMap<String, Integer>();

        for (var line : lines) {
            if (isCommentOrEmpty(line)) continue;

            var recordType = evaluateConditionalLogic(line, rule);
            recordsByType.computeIfAbsent(recordType, k -> new ArrayList<>()).add(line);
            recordCounts.merge(recordType, 1, Integer::sum);
        }

        return RecordTypeResult.builder()
                .recordsByType(recordsByType)
                .processingMethod("conditional-based")
                .recordCounts(recordCounts)
                .build();
    }

    private RecordTypeResult processPatternBased(List<String> lines, RecordTypeRule rule) {
        var pattern = Pattern.compile(rule.getPattern());
        var recordsByType = new HashMap<String, List<String>>();
        var recordCounts = new HashMap<String, Integer>();
        var unrecognizedLines = new ArrayList<String>();

        for (var line : lines) {
            if (isCommentOrEmpty(line)) continue;

            var matcher = pattern.matcher(line);
            String recordType;

            if (matcher.find()) {
                var matchedValue = matcher.group(1);
                recordType = rule.getPatternMappings().getOrDefault(matchedValue, rule.getDefaultType());
            } else {
                unrecognizedLines.add(line);
                recordType = rule.getDefaultType();
            }

            recordsByType.computeIfAbsent(recordType, k -> new ArrayList<>()).add(line);
            recordCounts.merge(recordType, 1, Integer::sum);
        }

        return RecordTypeResult.builder()
                .recordsByType(recordsByType)
                .processingMethod("pattern-based")
                .recordCounts(recordCounts)
                .unrecognizedLines(unrecognizedLines)
                .build();
    }

    private RecordTypeResult processMultiField(List<String> lines, RecordTypeRule rule) {
        var recordsByType = new HashMap<String, List<String>>();
        var recordCounts = new HashMap<String, Integer>();
        var unrecognizedLines = new ArrayList<String>();

        for (var line : lines) {
            if (isCommentOrEmpty(line)) continue;

            var recordType = evaluateMultiFieldRules(line, rule);
            if (recordType == null) {
                unrecognizedLines.add(line);
                recordType = rule.getDefaultType();
            }

            recordsByType.computeIfAbsent(recordType, k -> new ArrayList<>()).add(line);
            recordCounts.merge(recordType, 1, Integer::sum);
        }

        return RecordTypeResult.builder()
                .recordsByType(recordsByType)
                .processingMethod("multi-field")
                .recordCounts(recordCounts)
                .unrecognizedLines(unrecognizedLines)
                .build();
    }

    private RecordTypeResult processNoRecordType(List<String> lines) {
        var recordsByType = new HashMap<String, List<String>>();
        var dataLines = new ArrayList<String>();

        for (var line : lines) {
            if (!isCommentOrEmpty(line)) {
                dataLines.add(line);
            }
        }

        recordsByType.put("DATA", dataLines);
        var recordCounts = Map.of("DATA", dataLines.size());

        return RecordTypeResult.builder()
                .recordsByType(recordsByType)
                .processingMethod("no-record-type")
                .recordCounts(recordCounts)
                .build();
    }

    private String extractRecordTypeByPosition(String line, RecordTypeRule rule) {
        if (line.length() < rule.getStart() + rule.getLength() - 1) {
            return null;
        }

        var value = line.substring(rule.getStart() - 1, rule.getStart() - 1 + rule.getLength());

        if (rule.isTrimValues()) {
            value = value.trim();
        }

        if (rule.isIgnoreCase()) {
            value = value.toUpperCase();
        }

        return rule.getValues().get(value);
    }

    private String extractRecordTypeByCharacter(String line, RecordTypeRule rule) {
        if (line.length() < rule.getStart()) {
            return null;
        }

        var character = String.valueOf(line.charAt(rule.getStart() - 1));

        if (!rule.isCaseSensitive()) {
            character = character.toUpperCase();
        }

        // Check against valid characters if specified
        if (rule.getValidCharacters() != null && !rule.getValidCharacters().contains(character)) {
            return null;
        }

        return rule.getValues().get(character);
    }

    private String evaluateConditionalLogic(String line, RecordTypeRule rule) {
        if (rule.getConditionalExpression() != null) {
            return evaluateExpression(line, rule.getConditionalExpression(), rule);
        }

        if (rule.getConditionalLogic() != null) {
            return evaluateSimpleLogic(line, rule.getConditionalLogic(), rule);
        }

        return rule.getDefaultType();
    }

    private String evaluateExpression(String line, String expression, RecordTypeRule rule) {
        // Simple expression evaluator - can be enhanced for complex expressions
        if (line.length() < rule.getStart()) {
            return rule.getDefaultType();
        }

        var character = String.valueOf(line.charAt(rule.getStart() - 1));

        // Replace placeholders in expression
        var evaluatedExpression = expression
                .replace("{char}", "'" + character + "'")
                .replace("{line}", "'" + line + "'")
                .replace("{length}", String.valueOf(line.length()));

        // Simple evaluation - in production, use a proper expression evaluator
        if (evaluatedExpression.contains("== 'H'")) {
            return "H".equals(character) ? "HEADER" : null;
        } else if (evaluatedExpression.contains("== 'T'")) {
            return "T".equals(character) ? "TRAILER" : null;
        }

        return rule.getDefaultType();
    }

    private String evaluateSimpleLogic(String line, String logic, RecordTypeRule rule) {
        if (line.length() < rule.getStart()) {
            return rule.getDefaultType();
        }

        var character = String.valueOf(line.charAt(rule.getStart() - 1));

        // Simple logic evaluation
        if (logic.contains("== 'H'") && "H".equals(character)) {
            return "HEADER";
        } else if (logic.contains("== 'T'") && "T".equals(character)) {
            return "TRAILER";
        } else if (logic.contains("else")) {
            return "DATA";
        }

        return rule.getDefaultType();
    }

    private String evaluateMultiFieldRules(String line, RecordTypeRule rule) {
        for (var fieldRule : rule.getMultiFieldRules()) {
            if (line.length() >= fieldRule.getStart() + fieldRule.getLength() - 1) {
                var value = line.substring(fieldRule.getStart() - 1,
                        fieldRule.getStart() - 1 + fieldRule.getLength()).trim();

                if (fieldRule.getExpectedValue().equals(value)) {
                    return fieldRule.getRecordType();
                }
            }
        }
        return null;
    }

    private boolean isCommentOrEmpty(String line) {
        return line.trim().isEmpty() ||
                (line.length() > 6 && line.charAt(6) == '*') ||
                line.trim().startsWith("*");
    }
}
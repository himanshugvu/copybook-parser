package com.copybook.parser.processor;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.model.CobolField;
import com.copybook.parser.model.RecordTypeResult;
import com.copybook.parser.util.StorageCalculator;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class FieldProcessor {

    private final StorageCalculator storageCalculator;

    private static final Pattern LEVEL_PATTERN = Pattern.compile("^\\s*(\\d{2})\\s+([A-Za-z0-9-]+)");
    private static final Pattern PICTURE_PATTERN = Pattern.compile("PIC\\s+([X9SVP\\(\\)\\+\\-\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern USAGE_PATTERN = Pattern.compile("USAGE\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OCCURS_PATTERN = Pattern.compile("OCCURS\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REDEFINES_PATTERN = Pattern.compile("REDEFINES\\s+([A-Za-z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE_PATTERN = Pattern.compile("VALUE\\s+(['\"][^'\"]*['\"]|\\S+)", Pattern.CASE_INSENSITIVE);

    public FieldResult process(RecordTypeResult recordTypeResult, ParsingRules rules) {
        var fieldsByRecordType = new HashMap<String, List<CobolField>>();
        var totalFields = 0;

        for (var entry : recordTypeResult.getRecordsByType().entrySet()) {
            var recordType = entry.getKey();
            var lines = entry.getValue();

            log.debug("Processing fields for record type: {}", recordType);
            var fields = processFieldsForRecordType(lines, recordType, rules);
            fieldsByRecordType.put(recordType, fields);
            totalFields += fields.size();
        }

        return FieldResult.builder()
                .fieldsByRecordType(fieldsByRecordType)
                .totalFields(totalFields)
                .build();
    }

    private List<CobolField> processFieldsForRecordType(List<String> lines, String recordType, ParsingRules rules) {
        var fields = new ArrayList<CobolField>();
        var fieldStack = new Stack<CobolField>();
        var currentPosition = rules.getLayoutGeneration().getStartPosition();

        if (rules.getLayoutGeneration().isIncludeRecordType() && rules.getRecordTypeField() != null) {
            currentPosition += rules.getRecordTypeField().getLength() != null ?
                    rules.getRecordTypeField().getLength() : 1;
        }

        for (var line : lines) {
            if (isCommentOrEmpty(line)) continue;

            var field = parseFieldLine(line, recordType, rules);
            if (field != null) {
                // Calculate positions
                field.setStartPosition(currentPosition);
                var length = storageCalculator.calculateStorageLength(field);
                field.setStorageLength(length);
                field.setEndPosition(currentPosition + length - 1);

                // Handle hierarchy
                handleFieldHierarchy(field, fieldStack, rules);

                // Add to results
                fields.add(field);

                // Update position for next field (only for elementary fields)
                if (field.isElementary()) {
                    currentPosition += length;
                }
            }
        }

        return fields;
    }

    private CobolField parseFieldLine(String line, String recordType, ParsingRules rules) {
        var levelMatcher = LEVEL_PATTERN.matcher(line);
        if (!levelMatcher.find()) {
            return null;
        }

        var level = Integer.parseInt(levelMatcher.group(1));
        var name = levelMatcher.group(2);

        // Skip fillers if not configured to include them
        if (!rules.getFieldProcessing().isIncludeFillers() && "FILLER".equalsIgnoreCase(name)) {
            return null;
        }

        var builder = CobolField.builder()
                .level(level)
                .name(name)
                .recordType(recordType);

        // Extract picture clause
        var pictureMatcher = PICTURE_PATTERN.matcher(line);
        if (pictureMatcher.find()) {
            var picture = pictureMatcher.group(1);
            builder.picture(picture);
            builder.isElementary(true);
            builder.dataType(determineDataType(picture));
        } else {
            builder.isGroup(true);
        }

        // Extract usage
        var usageMatcher = USAGE_PATTERN.matcher(line);
        if (usageMatcher.find()) {
            var usage = usageMatcher.group(1);
            builder.usage(usage);
            var storageFormat = rules.getFieldProcessing().getUsageMappings().get(usage.toUpperCase());
            builder.storageFormat(storageFormat != null ? storageFormat : "character");
        }

        // Extract OCCURS
        var occursMatcher = OCCURS_PATTERN.matcher(line);
        if (occursMatcher.find()) {
            builder.occurs(Integer.parseInt(occursMatcher.group(1)));
        }

        // Extract REDEFINES
        var redefinesMatcher = REDEFINES_PATTERN.matcher(line);
        if (redefinesMatcher.find()) {
            builder.redefines(redefinesMatcher.group(1));
        }

        // Extract VALUE
        var valueMatcher = VALUE_PATTERN.matcher(line);
        if (valueMatcher.find()) {
            var value = valueMatcher.group(1);
            if (value.startsWith("'") || value.startsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            builder.value(value);
        }

        // Determine field characteristics
        builder.isCondition(level == 88);

        return builder.build();
    }

    private void handleFieldHierarchy(CobolField field, Stack<CobolField> fieldStack, ParsingRules rules) {
        // Pop fields with higher or equal levels
        while (!fieldStack.isEmpty() && fieldStack.peek().getLevel() >= field.getLevel()) {
            fieldStack.pop();
        }

        // Set parent if exists
        if (!fieldStack.isEmpty()) {
            var parent = fieldStack.peek();
            field.setParentName(parent.getName());

            // Add to parent's children
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(field);
        }

        // Push current field to stack if it's a group
        if (field.isGroup()) {
            fieldStack.push(field);
        }
    }

    private String determineDataType(String picture) {
        if (picture.contains("9")) {
            return picture.contains("V") ? "decimal" : "integer";
        } else if (picture.contains("X")) {
            return "string";
        } else if (picture.contains("A")) {
            return "alphabetic";
        }
        return "unknown";
    }

    private boolean isCommentOrEmpty(String line) {
        return line.trim().isEmpty() ||
                (line.length() > 6 && line.charAt(6) == '*') ||
                line.trim().startsWith("*");
    }

    @Data
    @Builder
    public static class FieldResult {
        private Map<String, List<CobolField>> fieldsByRecordType;
        private int totalFields;
    }
}

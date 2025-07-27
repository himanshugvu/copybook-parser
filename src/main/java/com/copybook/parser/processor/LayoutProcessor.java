package com.copybook.parser.processor;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.model.CobolField;
import com.copybook.parser.model.RecordLayout;
import com.copybook.parser.processor.FieldProcessor.FieldResult;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class LayoutProcessor {

    public LayoutResult process(FieldResult fieldResult, ParsingRules rules) {
        var layouts = new HashMap<String, RecordLayout>();
        var totalFields = 0;

        for (var entry : fieldResult.getFieldsByRecordType().entrySet()) {
            var recordType = entry.getKey();
            var fields = entry.getValue();

            var layout = createRecordLayout(recordType, fields, rules);
            layouts.put(recordType, layout);
            totalFields += fields.size();

            log.debug("Created layout for {}: {} fields, {} bytes",
                    recordType, layout.getFieldCount(), layout.getTotalLength());
        }

        return LayoutResult.builder()
                .layouts(layouts)
                .totalFields(totalFields)
                .build();
    }

    private RecordLayout createRecordLayout(String recordType, List<CobolField> fields, ParsingRules rules) {
        var groupFields = 0;
        var elementaryFields = 0;
        var conditionFields = 0;
        var hasRedefines = false;
        var hasOccurs = false;
        var hasConditions = false;
        var maxLength = 0;

        for (var field : fields) {
            if (field.isGroup()) {
                groupFields++;
            } else if (field.isCondition()) {
                conditionFields++;
                hasConditions = true;
            } else {
                elementaryFields++;
            }

            if (field.getRedefines() != null) {
                hasRedefines = true;
            }

            if (field.getOccurs() != null) {
                hasOccurs = true;
            }

            if (field.getEndPosition() > maxLength) {
                maxLength = field.getEndPosition();
            }
        }

        var layoutName = generateLayoutName(recordType, fields, rules);

        return RecordLayout.builder()
                .recordType(recordType)
                .layoutName(layoutName)
                .fields(fields)
                .fieldCount(fields.size())
                .totalLength(maxLength)
                .minLength(maxLength) // Simplified - could be calculated more accurately
                .maxLength(maxLength)
                .hasRedefines(hasRedefines)
                .hasOccurs(hasOccurs)
                .hasConditions(hasConditions)
                .groupFields(groupFields)
                .elementaryFields(elementaryFields)
                .conditionFields(conditionFields)
                .isValid(true)
                .build();
    }

    private String generateLayoutName(String recordType, List<CobolField> fields, ParsingRules rules) {
        var namingConvention = rules.getLayoutGeneration().getNamingConvention();

        // Find first 01 level field name
        var firstRecordName = fields.stream()
                .filter(f -> f.getLevel() == 1)
                .findFirst()
                .map(CobolField::getName)
                .orElse("RECORD");

        return namingConvention
                .replace("{record_type}", recordType)
                .replace("{01_level_name}", firstRecordName);
    }

    @Data
    @Builder
    public static class LayoutResult {
        private Map<String, RecordLayout> layouts;
        private int totalFields;
    }
}

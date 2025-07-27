package com.copybook.parser.processor;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.model.CobolField;
import com.copybook.parser.model.RecordLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LayoutProcessor {

    @Autowired
    private FieldProcessor fieldProcessor;

    public RecordLayout processLayout(List<String> copybookLines, ParsingRules rules) {
        List<CobolField> fields = fieldProcessor.processFields(copybookLines, rules);
        fieldProcessor.validateFieldStructure(fields, rules);

        int groupFields = 0;
        int elementaryFields = 0;
        int conditionFields = 0;
        int fillerFields = 0;

        for (CobolField field : fields) {
            if (field.isCondition()) {
                conditionFields++;
            } else if (field.isGroup()) {
                groupFields++;
            } else {
                elementaryFields++;
                if (field.isFiller()) {
                    fillerFields++;
                }
            }
        }

        int totalLength = fields.stream()
                .filter(f -> !f.isGroup() && !f.isCondition())
                .mapToInt(CobolField::getEndPosition)
                .max()
                .orElse(0);

        return RecordLayout.builder()
                .recordType("FIXED")
                .layoutName("LAYOUT-NAME")
                .fields(fields)
                .fieldCount(fields.size())
                .totalLength(totalLength)
                .groupFields(groupFields)
                .elementaryFields(elementaryFields)
                .conditionFields(conditionFields)
                .fillerFields(fillerFields)
                .build();
    }
}

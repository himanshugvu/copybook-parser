package com.example.rules;

import com.example.model.CopybookJson;
import com.example.model.Field;
import com.example.model.RecordLayout;

import java.util.List;

public class Json1RuleEngine implements RuleEngine {
    @Override
    public void apply(CopybookJson copybookJson) {
        // Example rule: ensure a 2-char RECORD-TYPE header exists at the start of each record
        for (RecordLayout layout : copybookJson.recordLayouts) {
            boolean hasHeader = layout.fields.stream()
                    .anyMatch(f -> "RECORD-TYPE".equalsIgnoreCase(f.name) && f.length == 2 && f.startPosition == 1);
            if (!hasHeader) {
                Field header = new Field();
                header.level = 3;
                header.name = "RECORD-TYPE";
                header.picture = "X(2)";
                header.startPosition = 1;
                header.endPosition = 2;
                header.length = 2;
                header.dataType = "STRING";
                header.usage = "Text/ASCII format (1 byte per character)";
                header.signed = false;
                header.decimal = false;
                header.decimalPlaces = 0;
                header.occursCount = 0;
                layout.fields.add(0, header);
                // Adjust following fields' positions
                for (int i = 1; i < layout.fields.size(); i++) {
                    Field f = layout.fields.get(i);
                    f.startPosition += 2;
                    f.endPosition += 2;
                }
            }
        }
    }
}

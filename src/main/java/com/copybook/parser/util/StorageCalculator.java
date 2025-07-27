package com.copybook.parser.util;

import com.copybook.parser.model.CobolField;
import java.util.List;

public class StorageCalculator {

    public int calculateTotalStorage(List<CobolField> fields) {
        if (fields == null || fields.isEmpty()) {
            return 0;
        }

        return fields.stream()
                .filter(field -> !field.isGroup() && !field.isCondition())
                .mapToInt(CobolField::getLength)
                .sum();
    }

    public int calculateStorageForGroup(List<CobolField> fields, String groupName) {
        if (fields == null || groupName == null || groupName.isEmpty()) {
            return 0;
        }

        return fields.stream()
                .filter(field -> field.isGroup() && groupName.equalsIgnoreCase(field.getName()))
                .mapToInt(CobolField::getLength)
                .sum();
    }
}

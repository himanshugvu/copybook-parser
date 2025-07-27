package com.example.model;

import java.util.List;
import java.util.Map;

public record CopybookLayout(
        Map<String, List<FieldDefinition>> recordTypeFields,
        Map<String, Integer> recordTypeLengths
) {}

package com.copybook.parser.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LayoutGenerationRule {

    // Layout strategy
    private String strategy = "multi_layout";
    private String separateBy = "record_type";
    private String namingConvention = "{record_type}_{01_level_name}";

    // Position calculation
    private int startPosition = 1;
    private boolean includeRecordType = true;
    private String alignment = "byte";
    private boolean zeroBasedPositions = false;
    private boolean includeEndPositions = true;

    // Output formatting
    private boolean includeMetadata = true;
    private boolean generateDocumentation = false;
    private String outputFormat = "json";
    private List<String> additionalFormats;

    // Layout customization
    private Map<String, String> layoutTemplates;
    private List<String> excludeFromLayout;
    private Map<String, Object> customLayoutProperties;

    // Grouping and organization
    private boolean groupByParent = true;
    private boolean flattenHierarchy = false;
    private String hierarchySeparator = ".";

    // Validation and constraints
    private boolean validatePositions = true;
    private boolean allowOverlaps = false;
    private Integer maxLayoutSize;

    // Output enhancement
    private boolean includeStatistics = true;
    private boolean includeValidationRules = false;
    private Map<String, String> outputMappings;
}
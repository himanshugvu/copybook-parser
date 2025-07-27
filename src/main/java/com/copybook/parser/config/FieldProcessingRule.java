package com.copybook.parser.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldProcessingRule {

    // Level hierarchy configuration
    private String levelHierarchy = "standard";
    private List<Integer> recordLevels = List.of(1);
    private List<Integer> groupLevels = List.of(5, 10, 15, 20, 25);
    private List<Integer> elementaryLevels = List.of(30, 35, 40, 45, 49);
    private List<Integer> conditionLevels = List.of(88);
    private Map<Integer, String> customLevelMappings;

    // Processing options
    private String redefinesHandling = "standard";
    private boolean occursExpansion = true;
    private boolean includeFillers = false;
    private boolean processConditions = true;
    private boolean processRedefines = true;

    // Data type mappings - fully configurable
    private Map<String, String> usageMappings = Map.of(
            "COMP", "binary",
            "COMP-1", "float",
            "COMP-2", "double",
            "COMP-3", "packed_decimal",
            "COMP-4", "binary",
            "COMP-5", "native_binary",
            "DISPLAY", "character",
            "PACKED-DECIMAL", "packed_decimal",
            "BINARY", "binary"
    );

    // Picture clause processing
    private boolean expandPictureRepeats = true;
    private boolean calculateStorageLength = true;
    private Map<String, String> pictureTypeMappings;

    // Field naming and filtering
    private List<String> excludeFieldNames;
    private List<String> includeFieldNames;
    private Map<String, String> fieldNameMappings;
    private String namingConvention = "original"; // original, uppercase, lowercase, camelCase

    // Occurs processing
    private String occursStrategy = "expand"; // expand, single, metadata_only
    private Integer maxOccursExpansion = 100;

    // Custom processing rules
    private List<CustomFieldRule> customRules;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomFieldRule {
        private String fieldPattern;
        private String action; // include, exclude, transform, validate
        private String transformation;
        private Map<String, Object> parameters;
    }
}
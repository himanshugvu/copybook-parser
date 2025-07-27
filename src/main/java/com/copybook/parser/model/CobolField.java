package com.copybook.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CobolField {

    private int level;
    private String name;
    private String originalName;
    private String picture;
    private String usage;
    private String value;

    // Position information
    private int startPosition;
    private int endPosition;
    private int length;
    private int storageLength;

    // OCCURS information
    private Integer occurs;
    private Integer minOccurs;
    private Integer maxOccurs;
    private String dependingOn;
    private List<CobolField> occursFields;

    // REDEFINES information
    private String redefines;
    private boolean isRedefineTarget;
    private List<CobolField> redefineAlternatives;

    // Hierarchy information
    private String parentName;
    private List<CobolField> children;
    private String fullPath;

    // Type information
    private String dataType;
    private String storageFormat;
    private boolean isGroup;
    private boolean isElementary;
    private boolean isCondition;
    private boolean isFiller;

    // Metadata
    private String recordType;
    private String description;
    private List<String> validValues;
    private Map<String, Object> customProperties;

    // Validation information
    private List<String> validationRules;
    private Map<String, String> constraints;
}
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
public class RecordLayout {

    private String recordType;
    private String layoutName;
    private String description;

    // Fields
    private List<CobolField> fields;
    private int fieldCount;

    // Size information
    private int totalLength;
    private int minLength;
    private int maxLength;

    // Characteristics
    private boolean hasRedefines;
    private boolean hasOccurs;
    private boolean hasConditions;
    private boolean isVariableLength;

    // Statistics
    private int groupFields;
    private int elementaryFields;
    private int conditionFields;
    private int fillerFields;

    // Validation
    private List<String> validationRules;
    private boolean isValid;
    private List<String> validationMessages;

    // Additional metadata
    private Map<String, Object> layoutMetadata;
    private List<String> dependencies;
}
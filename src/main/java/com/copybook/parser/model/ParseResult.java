package com.copybook.parser.model;

import com.copybook.parser.config.ParsingRules;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParseResult {

    private boolean success;
    private String copybookName;
    private String version = "2.0.0";

    // Results
    private Map<String, RecordLayout> recordLayouts;
    private int totalFields;
    private int totalRecordTypes;

    // Processing information
    private ParsingRules processingRules;
    private String processingMethod;
    private long processingTimeMs;

    // Error and warning information
    private String errorMessage;
    private List<String> warnings;
    private List<String> validationErrors;
    private Map<String, List<String>> detailedErrors;

    // Statistics
    private Map<String, Integer> fieldTypeStatistics;
    private Map<String, Integer> recordTypeStatistics;
    private Map<String, Object> processingStatistics;

    // Additional metadata
    private Map<String, Object> customMetadata;
    private List<String> appliedTransformations;

    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
}
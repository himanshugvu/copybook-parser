package com.copybook.parser.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class RecordTypeResult {
    private Map<String, List<String>> recordsByType;
    private String processingMethod;
    private Map<String, Integer> recordCounts;
    private List<String> unrecognizedLines;
    private Map<String, Object> processingMetadata;
    private List<String> processingWarnings;
}
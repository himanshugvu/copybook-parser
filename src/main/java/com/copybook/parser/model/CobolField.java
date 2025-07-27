package com.copybook.parser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CobolField {

    private int level;
    private String name;
    private String picture;
    private String value;

    // Position and Length
    private int startPosition;
    private int endPosition;
    private int length;

    // Helper methods to determine field type
    @JsonIgnore
    public boolean isGroup() {
        // A group field has no PICTURE clause and is not a condition level.
        return (picture == null || picture.trim().isEmpty()) && !isCondition();
    }

    @JsonIgnore
    public boolean isCondition() {
        // A condition field is always at level 88.
        return level == 88;
    }

    @JsonIgnore
    public boolean isFiller() {
        return name != null && "FILLER".equalsIgnoreCase(name.trim());
    }
}

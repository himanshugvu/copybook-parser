package com.example.model;

public record FieldDefinition(
        String name,
        String type,
        int start,
        int end,
        int length
) {}

package com.example.validation;

import java.util.Map;
import java.util.Set;

public class LayoutValidator {
    public static boolean validate(Map<String, Integer> recordLengths) {
        Set<Integer> uniqueLengths = Set.copyOf(recordLengths.values());
        if (uniqueLengths.size() > 1) {
            System.err.println("ERROR: Record length mismatch detected:");
            recordLengths.forEach((type, len) ->
                    System.err.println("  " + type + ": " + len + " chars"));
            System.err.println("All record types must have the same length.");
            return false;
        }
        return true;
    }
}

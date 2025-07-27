package com.copybook.parser.util;

import com.copybook.parser.model.CobolField;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Slf4j
public class StorageCalculator {

    private static final Pattern REPEAT_PATTERN = Pattern.compile("(\\w)\\((\\d+)\\)");

    public int calculateStorageLength(CobolField field) {
        if (field.isGroup()) {
            return 0; // Group fields don't consume storage directly
        }

        if (field.getPicture() == null) {
            return 0;
        }

        var picture = expandPictureRepeats(field.getPicture());
        var baseLength = calculateBasePictureLength(picture);

        // Apply OCCURS multiplier
        if (field.getOccurs() != null) {
            baseLength *= field.getOccurs();
        }

        // Apply USAGE modifications
        return applyUsageModifications(baseLength, field.getUsage(), picture);
    }

    private String expandPictureRepeats(String picture) {
        var matcher = REPEAT_PATTERN.matcher(picture);
        var result = new StringBuilder();
        var lastEnd = 0;

        while (matcher.find()) {
            result.append(picture, lastEnd, matcher.start());
            var character = matcher.group(1);
            var count = Integer.parseInt(matcher.group(2));
            result.append(character.repeat(count));
            lastEnd = matcher.end();
        }

        result.append(picture.substring(lastEnd));
        return result.toString();
    }

    private int calculateBasePictureLength(String picture) {
        var length = 0;
        var hasDecimal = false;

        for (var i = 0; i < picture.length(); i++) {
            var ch = picture.charAt(i);
            switch (ch) {
                case '9', 'X', 'A', 'Z', '*', '+', '-' -> length++;
                case 'V' -> hasDecimal = true;
                case 'P' -> {
                    // Assumed decimal positions don't consume storage
                }
                case 'S' -> {
                    // Sign doesn't consume extra storage in DISPLAY
                }
            }
        }

        return length;
    }

    private int applyUsageModifications(int baseLength, String usage, String picture) {
        if (usage == null) {
            return baseLength; // DISPLAY is default
        }

        return switch (usage.toUpperCase()) {
            case "COMP", "COMP-4", "BINARY" -> calculateBinaryLength(baseLength);
            case "COMP-1" -> 4; // Single precision float
            case "COMP-2" -> 8; // Double precision float
            case "COMP-3", "PACKED-DECIMAL" -> calculatePackedLength(baseLength, picture);
            case "COMP-5" -> calculateNativeBinaryLength(baseLength);
            default -> baseLength; // DISPLAY or unknown
        };
    }

    private int calculateBinaryLength(int digits) {
        if (digits <= 4) return 2;
        if (digits <= 9) return 4;
        if (digits <= 18) return 8;
        return 16; // For very large numbers
    }

    private int calculatePackedLength(int digits, String picture) {
        var hasSign = picture.contains("S");
        return (digits + (hasSign ? 1 : 0) + 1) / 2;
    }

    private int calculateNativeBinaryLength(int digits) {
        // Platform-specific binary representation
        return calculateBinaryLength(digits);
    }
}

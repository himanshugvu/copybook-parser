package com.copybook.parser.processor;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.model.CobolField;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FieldProcessor {

    // Pattern to capture level, name, and the rest of the line
    private static final Pattern GENERIC_FIELD_PATTERN = Pattern.compile(
            "^\\s*(\\d{2})\\s+([A-Z0-9\\-]+)(.*)$", Pattern.CASE_INSENSITIVE
    );

    // Pattern to find a PIC clause
    private static final Pattern PIC_PATTERN = Pattern.compile("PIC\\s+([^\\s]+)");
    // Pattern to find a VALUE clause
    private static final Pattern VALUE_PATTERN = Pattern.compile("VALUE\\s+(?:'([^']*)'|\"([^\"]*)\"|([^\\s.]+))");

    public List<CobolField> processFields(List<String> lines, ParsingRules rules) {
        List<CobolField> fields = new ArrayList<>();
        int currentPosition = 1; // COBOL positions start at 1

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("*")) {
                continue; // Skip empty or comment lines
            }

            Matcher matcher = GENERIC_FIELD_PATTERN.matcher(trimmedLine);
            if (matcher.matches()) {
                int level = Integer.parseInt(matcher.group(1));
                String name = matcher.group(2).toUpperCase();
                String remainder = matcher.group(3);

                CobolField.CobolFieldBuilder builder = CobolField.builder()
                        .level(level)
                        .name(name);

                // Check for PIC clause to identify elementary fields
                Matcher picMatcher = PIC_PATTERN.matcher(remainder);
                if (picMatcher.find()) {
                    String picture = picMatcher.group(1);
                    int length = calculateFieldLength(picture);

                    // Calculate start and end positions
                    int startPosForThisField = currentPosition;
                    int endPosition = startPosForThisField + length - 1;

                    builder.picture(picture)
                            .length(length)
                            .startPosition(startPosForThisField)
                            .endPosition(endPosition);

                    // Update the running position for the *next* field
                    currentPosition = endPosition + 1;
                }

                // Check for VALUE clause
                Matcher valueMatcher = VALUE_PATTERN.matcher(remainder);
                if (valueMatcher.find()) {
                    String value = valueMatcher.group(1) != null ? valueMatcher.group(1) :
                            valueMatcher.group(2) != null ? valueMatcher.group(2) :
                                    valueMatcher.group(3);
                    builder.value(value);
                }

                fields.add(builder.build());
            }
        }
        return fields;
    }

    private int calculateFieldLength(String picture) {
        if (picture == null || picture.isEmpty()) return 0;

        picture = picture.replaceAll("\\s+", "").toUpperCase();
        int totalLength = 0;
        int i = 0;

        while (i < picture.length()) {
            char currentChar = picture.charAt(i);
            if (currentChar == 'X' || currentChar == '9' || currentChar == 'S') {
                if (i + 1 < picture.length() && picture.charAt(i + 1) == '(') {
                    int closeParenIndex = picture.indexOf(')', i + 2);
                    if (closeParenIndex != -1) {
                        try {
                            totalLength += Integer.parseInt(picture.substring(i + 2, closeParenIndex));
                            i = closeParenIndex + 1;
                        } catch (NumberFormatException e) { i++; }
                    } else { i++; }
                } else {
                    totalLength++;
                    i++;
                }
            } else if (currentChar == 'V') {
                i++; // Implied decimal, no length
            } else if (currentChar == '.' || currentChar == '+' || currentChar == '-') {
                totalLength++;
                i++;
            } else {
                i++;
            }
        }
        return Math.max(totalLength, 1);
    }

    public void validateFieldStructure(List<CobolField> fields, ParsingRules rules) {
        CobolField lastField = null;
        for (CobolField current : fields) {
            if (!current.isGroup() && !current.isCondition()) {
                if (lastField != null) {
                    if (lastField.getEndPosition() >= current.getStartPosition()) {
                        throw new IllegalStateException(
                                String.format("Field overlap detected: %s (ends at %d) and %s (starts at %d)",
                                        lastField.getName(), lastField.getEndPosition(),
                                        current.getName(), current.getStartPosition())
                        );
                    }
                }
                lastField = current;
            }
        }
    }
}

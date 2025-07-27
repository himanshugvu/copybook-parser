package com.example.parser;

import com.example.model.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

public class CopybookParser {
    private static final Pattern LEVEL_PATTERN = Pattern.compile("^(\\d{2})\\s+");
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "(\\d{2})\\s+([\\w-]+)(?:\\s+REDEFINES\\s+([\\w-]+))?(?:\\s+PIC\\s+([X9A\\(\\)V\\.]+)(?:\\s+(COMP-3|COMP))?)?(?:\\s+OCCURS\\s+(\\d+)\\s+TIMES)?\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern COND_PATTERN = Pattern.compile("^\\s*88\\s+([\\w-]+)\\s+VALUE\\s+'([^']+)'\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern OCCURS_PATTERN = Pattern.compile("OCCURS\\s+(\\d+)\\s+TIMES", Pattern.CASE_INSENSITIVE);

    public static CopybookJson parseToJson(Path copybookPath) throws IOException {
        List<String> lines = Files.readAllLines(copybookPath);
        CopybookJson result = new CopybookJson();
        result.fileName = copybookPath.getFileName().toString();
        List<RecordLayout> layouts = new ArrayList<>();
        List<ReferenceField> refFields = new ArrayList<>();

        int totalLength = 0;
        RecordLayout currentLayout = null;
        Stack<List<Field>> fieldStack = new Stack<>();
        Stack<Integer> offsetStack = new Stack<>();
        int currentOffset = 1;
        Map<String, RecordLayout> layoutByName = new HashMap<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).replaceAll("\\*.*", "").trim();
            if (line.isEmpty()) continue;

            Matcher condMatcher = COND_PATTERN.matcher(line);
            if (condMatcher.find()) {
                // 88-level condition name, attach to last field if possible
                if (!fieldStack.isEmpty() && !fieldStack.peek().isEmpty()) {
                    Field lastField = fieldStack.peek().get(fieldStack.peek().size() - 1);
                    if (lastField.conditionNames == null) lastField.conditionNames = new ArrayList<>();
                    ConditionName cn = new ConditionName();
                    cn.name = condMatcher.group(1);
                    cn.value = condMatcher.group(2);
                    lastField.conditionNames.add(cn);
                }
                continue;
            }

            Matcher m = FIELD_PATTERN.matcher(line);
            if (m.find()) {
                int level = Integer.parseInt(m.group(1));
                String name = m.group(2);
                String redefines = m.group(3);
                String picture = m.group(4);
                String compType = m.group(5);
                int occurs = m.group(6) != null ? Integer.parseInt(m.group(6)) : 0;

                if (level == 1) {
                    if (currentLayout != null) {
                        // finalize previous layout
                        currentLayout.length = offsetStack.isEmpty() ? 0 : offsetStack.peek() - 1;
                        totalLength = Math.max(totalLength, currentLayout.length);
                        layouts.add(currentLayout);
                    }
                    currentLayout = new RecordLayout();
                    currentLayout.name = name;
                    currentLayout.redefines = redefines;
                    currentLayout.startPosition = 1;
                    currentLayout.length = 0;
                    currentLayout.fields = new ArrayList<>();
                    currentLayout.description = redefines != null ? "Memory overlay of " + redefines : "";
                    fieldStack.clear();
                    fieldStack.push(currentLayout.fields);
                    offsetStack.clear();
                    offsetStack.push(1);
                    layoutByName.put(name, currentLayout);

                    // If 01-level has PIC, treat as reference field only, not as layout
                    if (picture != null) {
                        ReferenceField refField = new ReferenceField();
                        refField.level = 1;
                        refField.name = name;
                        refField.picture = picture;
                        refField.startPosition = 1;
                        int length = getFieldLength(picture, 0, compType);
                        refField.endPosition = length;
                        refField.length = length;
                        refField.dataType = picture.contains("X") ? "STRING" : "NUMBER";
                        refField.usage = refField.dataType.equals("STRING") ? "Text/ASCII format (1 byte per character)"
                                : getUsage(compType);
                        refField.signed = picture.contains("S");
                        refField.decimal = picture.contains("V");
                        refField.decimalPlaces = refField.decimal ? getDecimalPlaces(picture) : 0;
                        refField.occursCount = 0;
                        refFields.add(refField);

                        // Don't build a layout for this 01, continue to next line
                        currentLayout = null;
                        fieldStack.clear();
                        offsetStack.clear();
                    }
                    continue;
                }

                if (fieldStack.isEmpty()) continue; // skip if not inside a layout

                Field f = new Field();
                f.level = level;
                f.name = name;
                f.picture = picture;
                f.occursCount = occurs;
                f.signed = picture != null && picture.contains("S");
                f.decimal = picture != null && picture.contains("V");
                f.decimalPlaces = f.decimal ? getDecimalPlaces(picture) : 0;
                f.dataType = (picture != null && picture.contains("X")) ? "STRING" : "NUMBER";
                f.usage = getUsage(compType);
                f.length = getFieldLength(picture, occurs, compType);

                // Group field (no PIC) or OCCURS
                if (picture == null) {
                    f.dataType = "GROUP";
                    f.length = 0;
                    f.usage = "";
                }

                // Calculate positions
                int offset = offsetStack.peek();
                f.startPosition = offset;
                f.endPosition = offset + (f.length > 0 ? f.length - 1 : 0);

                // OCCURS handling
                if (occurs > 0) {
                    // For OCCURS, create arrayElements with placeholders
                    f.arrayElements = new ArrayList<>();
                    for (int j = 0; j < occurs; j++) {
                        ArrayElement elem = new ArrayElement();
                        elem.index = j + 1;
                        elem.startPosition = offset + j * (f.length / occurs);
                        elem.endPosition = elem.startPosition + (f.length / occurs) - 1;
                        elem.length = f.length / occurs;
                        elem.fields = new ArrayList<>();
                        f.arrayElements.add(elem);
                    }
                }

                // Add to current group
                fieldStack.peek().add(f);

                // If group field (no PIC), push new context
                if (picture == null) {
                    f.fields = new ArrayList<>();
                    fieldStack.push(f.fields);
                    offsetStack.push(offset);
                } else {
                    offsetStack.push(offset + f.length);
                }

                // If we reach the end of a group, pop context
                // (simple heuristic: if next line is higher or equal level, pop)
                if (i + 1 < lines.size()) {
                    String nextLine = lines.get(i + 1).trim();
                    Matcher nextLevelMatcher = LEVEL_PATTERN.matcher(nextLine);
                    if (nextLevelMatcher.find()) {
                        int nextLevel = Integer.parseInt(nextLevelMatcher.group(1));
                        while (!offsetStack.isEmpty() && nextLevel <= level) {
                            offsetStack.pop();
                            if (fieldStack.size() > 1) fieldStack.pop();
                        }
                    }
                }
            }
        }
        if (currentLayout != null) {
            currentLayout.length = offsetStack.isEmpty() ? 0 : offsetStack.peek() - 1;
            totalLength = Math.max(totalLength, currentLayout.length);
            layouts.add(currentLayout);
        }

        result.totalLength = totalLength;
        result.referenceFields = refFields;
        result.recordLayouts = layouts;
        return result;
    }

    private static int getFieldLength(String picture, int occurs, String compType) {
        if (picture == null) return 0;
        int len = 0;
        Matcher m = Pattern.compile("([X9A])\\((\\d+)\\)").matcher(picture);
        while (m.find()) len += Integer.parseInt(m.group(2));
        if (len == 0) {
            // fallback: X, 9, etc.
            for (char c : picture.toCharArray()) {
                if ("X9A".indexOf(c) >= 0) len++;
            }
        }
        if (picture.matches(".*V\\d+.*")) {
            // V99 means two decimals (implied)
            String[] parts = picture.split("V");
            if (parts.length > 1) {
                Matcher m2 = Pattern.compile("(\\d+)").matcher(parts[1]);
                if (m2.find()) len += Integer.parseInt(m2.group(1));
            }
        }
        // COMP-3 (packed decimal) and COMP may have different byte sizes; here we just return char count for demo
        return occurs > 0 ? len * occurs : len;
    }

    private static int getDecimalPlaces(String picture) {
        if (picture == null) return 0;
        if (picture.contains("V")) {
            String[] parts = picture.split("V");
            if (parts.length > 1) {
                Matcher m = Pattern.compile("(\\d+)").matcher(parts[1]);
                if (m.find()) return Integer.parseInt(m.group(1));
            }
        }
        return 0;
    }

    private static String getUsage(String compType) {
        if (compType == null) return "Text/ASCII format (1 byte per character)";
        if ("COMP-3".equalsIgnoreCase(compType)) return "Packed decimal format (space efficient)";
        if ("COMP".equalsIgnoreCase(compType)) return "Binary format (2, 4, or 8 bytes)";
        return "Text/ASCII format (1 byte per character)";
    }
}

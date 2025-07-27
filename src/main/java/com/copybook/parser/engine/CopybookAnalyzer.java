package com.copybook.parser.engine;

import com.copybook.parser.config.ParsingRules;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class CopybookAnalyzer {

    private static final Pattern COPYBOOK_NAME_PATTERN = Pattern.compile("^\\s*01\\s+([A-Za-z0-9-]+)");

    public AnalysisResult analyze(List<String> lines, ParsingRules rules) {
        var warnings = new ArrayList<String>();
        var copybookName = "UNKNOWN";
        var totalLines = lines.size();
        var dataLines = 0;
        var commentLines = 0;
        var emptyLines = 0;

        for (var line : lines) {
            if (line.trim().isEmpty()) {
                emptyLines++;
            } else if (isCommentLine(line)) {
                commentLines++;
            } else {
                dataLines++;
                // Try to extract copybook name from first 01 level
                if (copybookName.equals("UNKNOWN")) {
                    var matcher = COPYBOOK_NAME_PATTERN.matcher(line);
                    if (matcher.find()) {
                        copybookName = matcher.group(1);
                    }
                }
            }
        }

        log.info("Copybook analysis: {} total lines, {} data, {} comments, {} empty",
                totalLines, dataLines, commentLines, emptyLines);

        if (dataLines == 0) {
            warnings.add("No data lines found in copybook");
        }

        return AnalysisResult.builder()
                .copybookName(copybookName)
                .totalLines(totalLines)
                .dataLines(dataLines)
                .commentLines(commentLines)
                .emptyLines(emptyLines)
                .warnings(warnings)
                .build();
    }

    private boolean isCommentLine(String line) {
        return (line.length() > 6 && line.charAt(6) == '*') ||
                line.trim().startsWith("*") ||
                line.trim().startsWith("//");
    }

    @Data
    @lombok.Builder
    public static class AnalysisResult {
        private String copybookName;
        private int totalLines;
        private int dataLines;
        private int commentLines;
        private int emptyLines;
        private List<String> warnings;
    }
}
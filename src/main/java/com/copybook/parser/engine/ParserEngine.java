package com.copybook.parser.engine;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.model.ParseResult;
import com.copybook.parser.processor.RecordTypeProcessor;
import com.copybook.parser.processor.FieldProcessor;
import com.copybook.parser.processor.LayoutProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ParserEngine {

    private final RuleInterpreter ruleInterpreter;
    private final CopybookAnalyzer analyzer;
    private final RecordTypeProcessor recordTypeProcessor;
    private final FieldProcessor fieldProcessor;
    private final LayoutProcessor layoutProcessor;

    public ParseResult parse(List<String> copybookLines, ParsingRules rules) {
        var startTime = System.currentTimeMillis();
        var warnings = new ArrayList<String>();

        log.info("Starting copybook parsing with rule type: {}", rules.getRuleType());

        try {
            // Step 1: Validate and interpret rules
            ruleInterpreter.validateRules(rules);

            // Step 2: Analyze copybook structure
            var analysisResult = analyzer.analyze(copybookLines, rules);
            if (!analysisResult.getWarnings().isEmpty()) {
                warnings.addAll(analysisResult.getWarnings());
            }

            // Step 3: Process record types
            var recordTypeResult = recordTypeProcessor.process(copybookLines, rules);
            log.info("Found {} record types: {}",
                    recordTypeResult.getRecordsByType().size(),
                    recordTypeResult.getRecordsByType().keySet());

            // Step 4: Process fields for each record type
            var fieldResult = fieldProcessor.process(recordTypeResult, rules);

            // Step 5: Generate layouts
            var layoutResult = layoutProcessor.process(fieldResult, rules);

            var processingTime = System.currentTimeMillis() - startTime;

            // Step 6: Build final result
            return ParseResult.builder()
                    .success(true)
                    .copybookName(analysisResult.getCopybookName())
                    .recordLayouts(layoutResult.getLayouts())
                    .totalFields(layoutResult.getTotalFields())
                    .totalRecordTypes(recordTypeResult.getRecordsByType().size())
                    .processingRules(rules)
                    .processingMethod(recordTypeResult.getProcessingMethod())
                    .processingTimeMs(processingTime)
                    .warnings(warnings.isEmpty() ? null : warnings)
                    .fieldTypeStatistics(calculateFieldTypeStats(layoutResult))
                    .recordTypeStatistics(recordTypeResult.getRecordCounts())
                    .build();

        } catch (Exception e) {
            log.error("Error parsing copybook", e);
            return ParseResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private Map<String, Integer> calculateFieldTypeStats(LayoutProcessor.LayoutResult layoutResult) {
        var stats = new HashMap<String, Integer>();

        layoutResult.getLayouts().values().forEach(layout -> {
            layout.getFields().forEach(field -> {
                var type = field.isGroup() ? "GROUP" :
                        field.isCondition() ? "CONDITION" : "ELEMENTARY";
                stats.merge(type, 1, Integer::sum);
            });
        });

        return stats;
    }
}
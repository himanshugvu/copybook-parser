package com.copybook.parser;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.engine.ParserEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class CopybookParserApplication implements CommandLineRunner {

    private final ParserEngine parserEngine;

    @Bean
    public ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    public static void main(String[] args) {
        SpringApplication.run(CopybookParserApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            printUsage();
            return;
        }

        var copybookPath = args[0];
        var rulesPath = args.length > 1 ? args[1] : null;
        var outputPath = args.length > 2 ? args[2] : "output.json";

        log.info("Processing copybook: {}", copybookPath);

        var copybookLines = Files.readAllLines(Paths.get(copybookPath));
        log.info("Loaded {} lines from copybook", copybookLines.size());

        var rules = loadRules(rulesPath);
        log.info("Using rule type: {}", rules.getRuleType());

        var result = parserEngine.parse(copybookLines, rules);

        var objectMapper = objectMapper();
        objectMapper.writeValue(new File(outputPath), result);

        if (result.isSuccess()) {
            log.info("✅ Parsing completed successfully in {}ms", result.getProcessingTimeMs());
            log.info("📊 Results: {} record layouts, {} total fields",
                    result.getRecordLayouts().size(), result.getTotalFields());
            log.info("📁 Output written to: {}", outputPath);

            if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
                log.warn("⚠️  Warnings: {}", result.getWarnings());
            }
        } else {
            log.error("❌ Parsing failed: {}", result.getErrorMessage());
            System.exit(1);
        }
    }

    private ParsingRules loadRules(String rulesPath) throws Exception {
        if (rulesPath == null) {
            log.info("No rules file provided, using default configurable rules");
            return ParsingRules.createDefault();
        }

        var objectMapper = objectMapper();
        var rules = objectMapper.readValue(new File(rulesPath), ParsingRules.class);
        log.info("Loaded rules from: {}", rulesPath);
        return rules;
    }

    private void printUsage() {
        System.out.println("""
                🔧 Copybook Parser v2.0.0 - Java 21 (Fully Configurable)
                
                Usage: java -jar copybook-parser.jar <copybook-file> [rules-file] [output-file]
                
                Arguments:
                  copybook-file  : Path to the COBOL copybook file (required)
                  rules-file     : Path to parsing rules JSON file (optional)
                  output-file    : Output file path (optional, default: output.json)
                
                Examples:
                  java -jar copybook-parser.jar employee.cpy
                  java -jar copybook-parser.jar employee.cpy rules/position-based.json
                  java -jar copybook-parser.jar employee.cpy rules/banking.json result.json
                """);
    }
}
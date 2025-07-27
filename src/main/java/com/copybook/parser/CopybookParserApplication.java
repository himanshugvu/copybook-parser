package com.copybook.parser;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.engine.CopybookAnalyzer;
import com.copybook.parser.engine.ParserEngine;
import com.copybook.parser.model.RecordLayout;
import com.copybook.parser.processor.LayoutProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
public class CopybookParserApplication implements CommandLineRunner {

    @Autowired
    private CopybookAnalyzer copybookAnalyzer;

    @Autowired
    private ParserEngine parserEngine;

    @Autowired
    private ParsingRules parsingRules;

    @Autowired
    private LayoutProcessor layoutProcessor;

    public static void main(String[] args) {
        SpringApplication.run(CopybookParserApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java -jar copybook-parser.jar <path-to-copybook>");
            return;
        }

        String copybookPath = args[0];
        Path path = Path.of(copybookPath);

        if (!Files.exists(path)) {
            System.err.println("Error: File not found at " + copybookPath);
            return;
        }

        // Read the copybook file into a list of lines
        List<String> copybookLines = Files.readAllLines(path);

        try {
            // Analyze the copybook
            RecordLayout layout = copybookAnalyzer.analyze(copybookLines, parsingRules);

            // Print the results
            System.out.println("Copybook Parsing Successful!");
            System.out.println("Record Layout Name: " + layout.getLayoutName());
            System.out.println("Total Length: " + layout.getTotalLength());
            System.out.println("Field Count: " + layout.getFieldCount());
            System.out.println("Group Fields: " + layout.getGroupFields());
            System.out.println("Elementary Fields: " + layout.getElementaryFields());
            System.out.println("Condition Fields: " + layout.getConditionFields());
            System.out.println("Filler Fields: " + layout.getFillerFields());

            // Optionally, print each field
            layout.getFields().forEach(field -> {
                System.out.printf("Field: %s, Level: %d, Picture: %s, Length: %d, Start: %d, End: %d%n",
                        field.getName(), field.getLevel(), field.getPicture(),
                        field.getLength(), field.getStartPosition(), field.getEndPosition());
            });
        } catch (Exception e) {
            System.err.println("Error during copybook parsing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

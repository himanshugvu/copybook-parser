package com.example;

import com.example.model.CopybookJson;
import com.example.parser.CopybookParser;
import com.example.rules.RuleEngine;
import com.example.rules.Json1RuleEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !"--copybook".equals(args[0])) {
            System.out.println("Usage: java -jar copybook-parser.jar --copybook <copybook.cpy>");
            return;
        }
        String copybookFile = args[1];

        CopybookJson cbJson = CopybookParser.parseToJson(Path.of(copybookFile));

        // Apply rule engine (choose based on your logic)
        RuleEngine ruleEngine = new Json1RuleEngine();
        ruleEngine.apply(cbJson);

        String outputFile = copybookFile.replaceAll("\\.[^.]+$", "") + ".json";
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(Path.of(outputFile).toFile(), cbJson);

        System.out.println("JSON written to: " + outputFile);
    }
}

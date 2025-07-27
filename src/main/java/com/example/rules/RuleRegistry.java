package com.example.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;

public class RuleRegistry {
    private final Map<String, RecordTypeRule> rules = new HashMap<>();

    public RuleRegistry(String configFilePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(configFilePath));
            JsonNode rulesNode = root.get("rules");
            Iterator<String> it = rulesNode.fieldNames();
            while (it.hasNext()) {
                String ruleId = it.next();
                JsonNode ruleDef = rulesNode.get(ruleId);
                String type = ruleDef.get("type").asText();
                switch (type) {
                    case "position" -> {
                        int start = ruleDef.get("start").asInt();
                        int length = ruleDef.get("length").asInt();
                        Map<String, String> mapping = new HashMap<>();
                        ruleDef.get("mapping").fields().forEachRemaining(entry ->
                                mapping.put(entry.getKey(), entry.getValue().asText()));
                        rules.put(ruleId, new PositionBasedRecordTypeRule(start, length, mapping));
                    }
                    case "character" -> {
                        int start = ruleDef.get("start").asInt();
                        Map<String, String> mapping = new HashMap<>();
                        ruleDef.get("mapping").fields().forEachRemaining(entry ->
                                mapping.put(entry.getKey(), entry.getValue().asText()));
                        rules.put(ruleId, new CharacterBasedRecordTypeRule(start, mapping));
                    }
                    case "conditional" -> {
                        List<ConditionalBasedRecordTypeRule.Condition> conds = new ArrayList<>();
                        for (JsonNode condNode : ruleDef.get("conditions")) {
                            conds.add(new ConditionalBasedRecordTypeRule.Condition(
                                    condNode.get("equals").asText(),
                                    condNode.get("at").asInt(),
                                    condNode.get("result").asText()
                            ));
                        }
                        String defaultType = ruleDef.get("default").asText();
                        rules.put(ruleId, new ConditionalBasedRecordTypeRule(conds, defaultType));
                    }
                    case "none" -> rules.put(ruleId, new NoRecordTypeRule());
                    default -> throw new IllegalArgumentException("Unknown rule type: " + type);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load rule config: " + e.getMessage(), e);
        }
    }

    public RecordTypeRule getRule(String ruleId) {
        return rules.get(ruleId);
    }
}

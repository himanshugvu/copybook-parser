package com.example.rules;

import com.example.model.CopybookJson;

public interface RuleEngine {
    void apply(CopybookJson copybookJson);
}

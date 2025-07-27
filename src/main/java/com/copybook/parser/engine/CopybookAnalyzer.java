package com.copybook.parser.engine;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.model.RecordLayout;
import com.copybook.parser.processor.LayoutProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CopybookAnalyzer {

    @Autowired
    private LayoutProcessor layoutProcessor;

    public RecordLayout analyze(List<String> copybookLines, ParsingRules rules) {
        // Process the copybook lines into a RecordLayout
        return layoutProcessor.processLayout(copybookLines, rules);
    }
}

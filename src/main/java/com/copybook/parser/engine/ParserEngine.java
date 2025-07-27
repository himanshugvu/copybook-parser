package com.copybook.parser.engine;

import com.copybook.parser.config.ParsingRules;
import com.copybook.parser.model.RecordLayout;
import com.copybook.parser.processor.LayoutProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ParserEngine {

    @Autowired
    private LayoutProcessor layoutProcessor;

    public ParseResult parseCopybook(List<String> copybookLines, ParsingRules rules) {
        try {
            RecordLayout layout = layoutProcessor.processLayout(copybookLines, rules);

            Map<String, RecordLayout> layouts = new HashMap<>();
            layouts.put(layout.getLayoutName(), layout);

            return new ParseResult(layouts, true, null);
        } catch (Exception e) {
            return new ParseResult(new HashMap<>(), false, e.getMessage());
        }
    }

    public static class ParseResult {
        private final Map<String, RecordLayout> layouts;
        private final boolean success;
        private final String errorMessage;

        public ParseResult(Map<String, RecordLayout> layouts, boolean success, String errorMessage) {
            this.layouts = layouts;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public Map<String, RecordLayout> getLayouts() {
            return layouts;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

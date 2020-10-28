package org.globalbioticinteractions.pensoft;

import java.util.Arrays;
import java.util.List;

public class TableRectifier implements TableProcessor {

    private final List<TableProcessor> processors;

    public TableRectifier() {
        this(new TablePreprocessor(),
                new ExpandColumnSpans(),
                new ExpandRowSpans(),
                new AddColumnsForOpenBiodivTerms());
    }

    public TableRectifier(TableProcessor... processors) {
        this.processors = Arrays.asList(processors);
    }

    @Override
    public String process(String input) {
        return processors
                .stream()
                .reduce(input,
                        (s, processor) -> processor.process(s),
                        (s, s2) -> s);
    }
}

package org.globalbioticinteractions.pensoft;

import java.util.stream.Stream;

public class TableRectifier implements TableProcessor {

    @Override
    public String process(String input) {
        return Stream.of(
                new TablePreprocessor(),
                new ExpandColumnSpans(),
                new ExpandRowSpans(),
                new ExpandRowValues()
        ).reduce(input,
                (s, processor) -> processor.process(s),
                (s, s2) -> s);
    }
}

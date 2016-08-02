package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;
import org.globalnames.parser.ScientificNameParser;
import scala.Option;

public class GlobalNamesCanon implements NameSuggester {
    private final ScientificNameParser parser = ScientificNameParser.instance();

    @Override
    public String suggest(String name) {
        final Option<String> canonized = parser.fromString(name).canonized(true);
        return canonized.isDefined() ? canonized.get() : name;
    }
}

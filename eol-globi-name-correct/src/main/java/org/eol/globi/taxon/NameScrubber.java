package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;

class NameScrubber implements NameSuggester {

    @Override
    public String suggest(final String name) {
        return clean(name);
    }

    private static String clean(String name) {
        name = name.replaceAll("[^\\p{L}\\p{N}-\\.\\(\\)]", " ");
        return name.trim();
    }

}

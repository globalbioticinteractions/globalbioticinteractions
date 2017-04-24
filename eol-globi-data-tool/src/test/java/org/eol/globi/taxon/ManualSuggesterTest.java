package org.eol.globi.taxon;

import org.junit.Test;

public class ManualSuggesterTest {

    @Test
    public void throwOnDuplicates() {
        new ManualSuggester().suggest("bla");
    }
}

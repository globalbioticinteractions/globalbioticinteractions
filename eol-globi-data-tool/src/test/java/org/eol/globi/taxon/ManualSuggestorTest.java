package org.eol.globi.taxon;

import org.junit.Test;

public class ManualSuggestorTest {

    @Test
    public void throwOnDuplicates() {
        new ManualSuggestor().suggest("bla");
    }
}

package org.eol.globi.data.taxon;

import org.junit.Test;

public class ManualSuggestorTest {

    @Test
    public void throwOnDuplicates() {
        new ManualSuggestor().suggest("bla");
    }
}

package org.eol.globi.taxon;

import org.junit.Test;

public class TaxonIdLookupTest {

    @Test
    public void humans() {
        TaxonIdLookup.main(new String[]{"NCBI:9606"});
        TaxonIdLookup.main(new String[]{"EOL:327955"});
    }

}
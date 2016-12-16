package org.eol.globi.taxon;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class TaxonIdLookupTest {
    private static final Log LOG = LogFactory.getLog(TaxonIdLookupTest.class);

    @Test
    public void lookupHumanEOL() {
        doLookup("EOL:327955");
    }

    @Test
    public void lookupHumanNCBI() {
        doLookup("NCBI:9606");
    }

    private void doLookup(String id) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        TaxonIdLookup.main(new String[]{id});
        stopWatch.stop();
        LOG.info("lookup of [" + id + "] took: [" + stopWatch.getTime() + "] ms.");
    }


}
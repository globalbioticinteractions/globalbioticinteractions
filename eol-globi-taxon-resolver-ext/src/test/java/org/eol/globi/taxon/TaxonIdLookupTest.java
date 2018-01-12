package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class TaxonIdLookupTest {
    private static final Log LOG = LogFactory.getLog(TaxonIdLookupTest.class);

    @Test
    public void lookupHumanEOL() {
        doLookup(new String[]{"EOL:327955"});
    }

    @Test
    public void lookupNonExisting() {
        doLookup(new String[]{"EXAMPLE:123"});
    }

    @Test
    public void lookupHumanNCBI() {
        doLookup(new String[]{"NCBI:9606"});
    }

    private void doLookup(String[] args) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        TaxonIdLookup.main(args);
        stopWatch.stop();
        LOG.info("lookup of [" + StringUtils.join(args, "|") + "] took: [" + stopWatch.getTime() + "] ms.");
    }


}
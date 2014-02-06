package org.eol.globi.service;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TaxonRichnessLookupTest {

    private static Log LOG = LogFactory.getLog(TaxonRichnessLookup.class);

    @Test
    public void lookupDiversityByMarineLocation() throws IOException {
        Double richness = new TaxonRichnessLookup().lookupRichness(12.811801, 131.118162);
        assertThat(richness, is(0.608192685));
    }

    @Test
    public void lookupDiversityByLocationTerrestrial() throws IOException {
        Double richness = new TaxonRichnessLookup().lookupRichness(41.771312,75.043944);
        assertThat(richness, is(0.0));
    }

    @Test
    public void performance() throws IOException {
        double lng = Math.random() * 360.0 - 180.0;
        double lat = Math.random() * 180.0 - 90.0;
        StopWatch watch = new StopWatch();
        TaxonRichnessLookup taxonRichnessLookup = new TaxonRichnessLookup();
        watch.start();
        for (int i=0; i< 1000; i++) {
            taxonRichnessLookup.lookupRichness(lat, lng);
        }
        watch.stop();
        taxonRichnessLookup.dispose();

        LOG.info("took [" + watch.getTime() + "] ms to lookup 1000 random locations");
    }


}

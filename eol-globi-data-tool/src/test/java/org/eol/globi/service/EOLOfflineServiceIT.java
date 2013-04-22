package org.eol.globi.service;

import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.domain.TaxonomyProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.eol.globi.domain.Taxon;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class EOLOfflineServiceIT {

    private static EOLOfflineService eolOfflineService;

    public static final String[] TAXON_NAMES = new String[]{
            "Zalieutes mcgintyi",
            "Serranus atrobranchus",
            "Peprilus burti",
            "Prionotus longispinosus",
            "Neopanope sayi"
    };

    @BeforeClass
    public static void init() {
        eolOfflineService = new EOLOfflineService();
    }

    @AfterClass
    public static void destroy() {
        eolOfflineService.shutdown();
    }

    @Test
    public void matchPredatorTaxon() throws TaxonPropertyLookupServiceException {
        matchTaxon("Homo sapiens");
    }

    @Test
    public void matchPreyTaxon() throws TaxonPropertyLookupServiceException {
        enrichPreyTaxon("Rattus rattus");
    }

    @Test
    public void matchNameTooShort() throws TaxonPropertyLookupServiceException {
        String externalId = eolOfflineService.lookupPropertyValueByTaxonName("G", Taxon.EXTERNAL_ID);
        assertThat(externalId, is(nullValue()));
    }

    private void enrichPreyTaxon(String preyName) throws TaxonPropertyLookupServiceException {
        matchTaxon(preyName);
    }

    private void matchTaxon(String speciesName) throws TaxonPropertyLookupServiceException {
        String externalId = eolOfflineService.lookupPropertyValueByTaxonName(speciesName, Taxon.EXTERNAL_ID);
        assertThat("failed to match [" + speciesName + "]", externalId, containsString(TaxonomyProvider.ID_PREFIX_EOL));
    }


    @Ignore
    @Test
    public void matchManyPredatorTaxons() throws TaxonPropertyLookupServiceException {

        //warm-up
        matchTaxon("Syacium gunteri");

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        for (String taxonName : TAXON_NAMES) {
            matchTaxon(taxonName);
        }
        stopwatch.stop();

        float rate = 1000.0f * TAXON_NAMES.length / stopwatch.getTime();
        assertThat("rate of term matching [" + rate + "] is less than 1 term/s", rate > 1.0, is(true));
    }

    @Ignore
    @Test
    public void matchManyPreyTaxons() throws TaxonPropertyLookupServiceException {

        //warm-up
        enrichPreyTaxon("Syacium gunteri");

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        for (String taxonName : TAXON_NAMES) {
            enrichPreyTaxon(taxonName);
        }
        stopwatch.stop();

        float rate = 1000.0f * TAXON_NAMES.length / stopwatch.getTime();
        assertThat("rate of term matching [" + rate + "] is less than 1 term/s", rate > 1.0, is(true));
    }


}

package org.eol.globi.data;

import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.ResourceService;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.data.DatasetImporterForMetaTable.EVENT_DATE;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_RANK;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_RANK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class INaturalistOccurrenceIdIdEnricherTest {

    @Test
    public void lookupSourceOccurrenceId() throws StudyImporterException {

        Map<String, String> properties
                = new INaturalistOccurrenceIdIdEnricher(null, null, getResourceService())
                .enrich(new TreeMap<String, String>() {{
                    put("sourceOccurrenceId", "https://www.inaturalist.org/observations/2900976");
                }});

        assertThat(properties.get(SOURCE_TAXON_NAME), is("Enhydra lutris"));
        assertThat(properties.get(SOURCE_TAXON_ID), is(TaxonomyProvider.INATURALIST_TAXON.getIdPrefix() + "41860"));
    }

    public ResourceService getResourceService() {
        return new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return getClass().getResourceAsStream("inat-2900976.json");
            }
        };
    }

    @Test
    public void lookupTargetOccurrenceId() throws StudyImporterException {
        Map<String, String> properties
                = new INaturalistOccurrenceIdIdEnricher(null, null, getResourceService())
                .enrich(new TreeMap<String, String>() {{
                    put("targetOccurrenceId", "https://www.inaturalist.org/observations/2900976");
                }});

        assertThat(properties.get(TARGET_TAXON_NAME), is("Enhydra lutris"));
        assertThat(properties.get(TARGET_TAXON_RANK), is("species"));
        assertThat(properties.get(TARGET_TAXON_ID), is("INAT_TAXON:41860"));
        assertThat(properties.get("decimalLatitude"), is("35.207705692"));
        assertThat(properties.get("decimalLongitude"), is("-120.9944534689"));
        assertThat(properties.get(EVENT_DATE), is("2016-03-25"));
        assertThat(properties.get("localityName"), is("San Luis Obispo County, US-CA, US"));
    }


    @Test
    public void parse() {

        InputStream is = getClass().getResourceAsStream("inaturalist.json");

        String taxonNameField = SOURCE_TAXON_NAME;
        String taxonIdField = SOURCE_TAXON_ID;
        String taxonRankField = SOURCE_TAXON_RANK;

        Map<String, String> properties = new TreeMap<>();

        try {
            INaturalistOccurrenceIdIdEnricher.enrichWithINaturalistObservation(is,
                    taxonNameField,
                    taxonIdField,
                    taxonRankField,
                    properties);

        } catch (IOException e) {


        }
        assertThat(properties.get(taxonNameField), is("Enhydra lutris"));
        assertThat(properties.get(taxonRankField), is("species"));
        assertThat(properties.get(taxonIdField), is("INAT_TAXON:41860"));
        assertThat(properties.get("decimalLatitude"), is("35.207705692"));
        assertThat(properties.get("decimalLongitude"), is("-120.9944534689"));
        assertThat(properties.get(EVENT_DATE), is("2016-03-25"));
        assertThat(properties.get("localityName"), is("San Luis Obispo County, US-CA, US"));


    }


    private class ResourceStaticService implements ResourceService {
        @Override
        public InputStream retrieve(URI resourceName) throws IOException {
            return getClass().getResourceAsStream("inat-2900976.json");
        }
    }
}
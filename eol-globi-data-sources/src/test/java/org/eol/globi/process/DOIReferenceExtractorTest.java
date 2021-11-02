package org.eol.globi.process;

import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class DOIReferenceExtractorTest {

    @Test
    public void extractDOI() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Gonzalez et al. 2015. Floral integration and pollinator diversity in the generalized plant-pollinator system of Alstroemeria ligtu (Alstroemeriaceae). Evolutionary Ecology, 21(9), 63-75. doi: 10.1007/s10682-014-9746-3");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is("10.1007/s10682-014-9746-3"));

    }

    @Test
    public void existingDOI() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Gonzalez et al. 2015. Floral integration and pollinator diversity in the generalized plant-pollinator system of Alstroemeria ligtu (Alstroemeriaceae). Evolutionary Ecology, 21(9), 63-75. doi: 10.1007/s10682-014-9746-3");
            put(DatasetImporterForTSV.REFERENCE_DOI, "10.1007/444");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is("10.1007/444"));

    }

    @Test
    public void existingReferenceURL() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Gonzalez et al. 2015. Floral integration and pollinator diversity in the generalized plant-pollinator system of Alstroemeria ligtu (Alstroemeriaceae). Evolutionary Ecology, 21(9), 63-75. doi: 10.1007/s10682-014-9746-3");
            put(DatasetImporterForTSV.REFERENCE_URL, "https://example.org");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is(nullValue()));

    }

    @Test
    public void extractMalformedDOI() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Gonzalez et al. 2015. Floral integration and pollinator diversity in the generalized plant-pollinator system of Alstroemeria ligtu (Alstroemeriaceae). Evolutionary Ecology, 21(9), 63-75. doi: 40.1007/s10682-014-9746-3");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is(nullValue()));

    }

    @Test
    public void extractNoDOI() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Gonzalez et al. 2015. Floral integration and pollinator diversity in the generalized plant-pollinator system of Alstroemeria ligtu (Alstroemeriaceae). Evolutionary Ecology, 21(9), 63-75.");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is(nullValue()));

    }

}
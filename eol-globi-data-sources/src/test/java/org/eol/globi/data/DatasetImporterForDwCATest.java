package org.eol.globi.data;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.tool.NullImportLogger;
import org.gbif.dwc.Archive;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DwCAUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertNull;
import static org.eol.globi.data.DatasetImporterForDwCA.EXTENSION_ASSOCIATED_TAXA;
import static org.eol.globi.data.DatasetImporterForDwCA.EXTENSION_RESOURCE_RELATIONSHIP;
import static org.eol.globi.data.DatasetImporterForDwCA.importAssociatedTaxaExtension;
import static org.eol.globi.data.DatasetImporterForDwCA.importResourceRelationExtension;
import static org.eol.globi.data.DatasetImporterForDwCA.mapReferenceInfo;
import static org.eol.globi.data.DatasetImporterForDwCA.parseAssociatedOccurrences;
import static org.eol.globi.data.DatasetImporterForDwCA.parseDynamicPropertiesForInteractionsOnly;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.eol.globi.data.DatasetImporterForTSV.DATASET_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_BODY_PART_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_CATALOG_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_COLLECTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_FIELD_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_INSTITUTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_FAMILY;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.gbif.dwc.terms.DwcTerm.relatedResourceID;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertTrue;

public class DatasetImporterForDwCATest {

    @Test
    public void importRecordsFromDir() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/vampire-moth-dwca-main/meta.xml");
        URI archiveRoot = new File(resource.toURI()).getParentFile().toURI();
        assertImportsSomething(archiveRoot, new AtomicInteger(0));
    }

    @Test
    public void importAssociatedTaxaFromDir() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/associated-taxa-test/meta.xml");
        URI archiveRoot = new File(resource.toURI()).getParentFile().toURI();
        assertImportsSomething(archiveRoot, new AtomicInteger(0));
    }

    @Test
    public void importTaxonDescriptionsFromDir() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/coetzer/meta.xml");
        URI archiveRoot = new File(resource.toURI()).getParentFile().toURI();
        List<Map<String, String>> links = new ArrayList<>();
        DatasetImporterForDwCA studyImporterForDwCA = new DatasetImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", archiveRoot, inStream -> inStream));
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                links.add(interaction);
            }
        });
        studyImporterForDwCA.importStudy();

        assertThat(links.size() > 0, is(true));
        assertThat(links.get(0).get(DATASET_CITATION), containsString("org/globalbioticinteractions/dataset/coetzer/"));
        assertThat(links.get(0).get(REFERENCE_CITATION), is("Cockerell, T.D.A. 1937. African bees of the genera Ceratina, Halictus and Megachile. 254 pp. William Clowes and Sons, London"));
        assertThat(links.get(0).get(TARGET_TAXON_NAME), is("Chaetodactylus leleupi"));
        assertThat(links.get(0).get(SOURCE_TAXON_NAME), is("Ceratina ruwenzorica Cockerell, 1937"));
        assertThat(links.get(0).get(INTERACTION_TYPE_NAME), is("Parasite"));
    }

    @Test
    public void importTaxonDescriptionsFromDirNoInteractionType() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/coetzer-no-interaction-type/meta.xml");
        URI archiveRoot = new File(resource.toURI()).getParentFile().toURI();
        List<Map<String, String>> links = new ArrayList<>();
        DatasetImporterForDwCA studyImporterForDwCA = new DatasetImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", archiveRoot, inStream -> inStream));
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                links.add(interaction);
            }
        });
        studyImporterForDwCA.importStudy();

        assertThat(links.size(), is(0));
    }

    @Test
    public void importTaxonDescriptionsFromDirUnsupportedDescriptionType() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/coetzer-unsupported-description-type/meta.xml");
        URI archiveRoot = new File(resource.toURI()).getParentFile().toURI();
        List<Map<String, String>> links = new ArrayList<>();
        DatasetImporterForDwCA studyImporterForDwCA = new DatasetImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", archiveRoot, inStream -> inStream));
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                links.add(interaction);
            }
        });
        studyImporterForDwCA.importStudy();

        assertThat(links.size(), is(0));
    }

    @Test
    public void importAssociatedTaxaFromDirIgnoredInteractionType() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/associated-taxa-test/meta.xml");
        URI archiveRoot = new File(resource.toURI()).getParentFile().toURI();
        assertImportsSomething(archiveRoot, new AtomicInteger(0));
    }

    @Test
    public void importRecordsFromMCZ() throws StudyImporterException, URISyntaxException {
        StringBuilder actualMessage = new StringBuilder();
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/mcz/meta.xml");
        URI archiveRoot = new File(resource.toURI()).getParentFile().toURI();
        AtomicInteger recordCounter = new AtomicInteger(0);
        DatasetImporterForDwCA studyImporterForDwCA = new DatasetImporterForDwCA(null, null);
        studyImporterForDwCA.setLogger(new NullImportLogger() {
            @Override
            public void severe(LogContext ctx, String message) {
                actualMessage.append(message);
            }
        });
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", archiveRoot, inStream -> inStream));
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                for (String expectedProperty : new String[]{}) {
                    assertThat("no [" + expectedProperty + "] found in " + interaction, interaction.containsKey(expectedProperty), is(true));
                    assertThat("no value of [" + expectedProperty + "] found in " + interaction, interaction.get(expectedProperty), is(notNullValue()));
                }

                recordCounter.incrementAndGet();
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(recordCounter.get(), is(0));
        assertThat(actualMessage.toString(), startsWith("[failed to handle dwc record]"));
    }

    @Test
    public void nonInteractionRecordMessage() throws StudyImporterException, URISyntaxException {
        List<String> msgs = new ArrayList<>();
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/mcz-adjusted/meta.xml");
        URI archiveRoot = new File(resource.toURI()).getParentFile().toURI();
        AtomicInteger recordCounter = new AtomicInteger(0);
        DatasetImporterForDwCA studyImporterForDwCA = new DatasetImporterForDwCA(null, null);
        studyImporterForDwCA.setLogger(new NullImportLogger() {
            @Override
            public void info(LogContext ctx, String message) {
                msgs.add(message);
            }
        });
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", archiveRoot, inStream -> inStream));
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                recordCounter.incrementAndGet();

            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(recordCounter.get(), is(0));
        String joinedMsgs = StringUtils.join(msgs, "\n");
        assertThat(joinedMsgs, containsString("]: indexing interaction records"));
        assertThat(joinedMsgs, containsString("]: scanned [1] record(s)"));
    }

    @Test
    public void importRecordsFromArchive() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip");
        assertImportsSomething(resource.toURI(), new AtomicInteger(0));
    }

    @Test
    public void importRecordsFromArchiveWithResourceRelations() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/dwca-with-resource-relation.zip");
        AtomicInteger recordCounter = new AtomicInteger(0);
        assertImportsSomething(resource.toURI(), recordCounter,
                TaxonUtil.SOURCE_TAXON_ID,
                SOURCE_TAXON_NAME,
                INTERACTION_TYPE_NAME,
                TaxonUtil.TARGET_TAXON_ID,
                TaxonUtil.TARGET_TAXON_NAME);
        assertThat(recordCounter.get(), is(677));
    }

    @Test
    public void importRecordsFromUArchive() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip");
        assertImportsSomething(resource.toURI(), new AtomicInteger(0));
    }

    @Test
    public void importRecordsFromUnresolvedResourceRelationshipArchive() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("fmnh-rr-unresolved-targetid-test.zip");

        AtomicInteger recordCounter = new AtomicInteger(0);
        DatasetImporterForDwCA studyImporterForDwCA = new DatasetImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", resource.toURI(), inStream -> inStream));
        studyImporterForDwCA.setInteractionListener(interaction -> {
            if (interaction.get(TARGET_OCCURRENCE_ID) != null) {
                assertNull(interaction.get(TARGET_TAXON_NAME));
                assertThat(interaction.get(TARGET_OCCURRENCE_ID), is("http://n2t.net/ark:/65665/37d63a454-d948-4b1d-89db-89809887ef41"));
                recordCounter.incrementAndGet();
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(recordCounter.get(), greaterThan(0));
    }

    @Test
    public void importRecordsFromArchiveWithAssociatedTaxa() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/eol/globi/data/AEC-DBCNet_DwC-A20160308-sample.zip");
        assertImportsSomething(resource.toURI(), new AtomicInteger(0));
    }

    @Test
    public void importRecordsFromArctosArchive() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/arctos_mvz_bird_small.zip");
        DatasetImporterForDwCA studyImporterForDwCA = new DatasetImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", resource.toURI(), inStream -> inStream));

        List<String> families = new ArrayList<>();
        AtomicBoolean someRecords = new AtomicBoolean(false);
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                assertThat(interaction.get(REFERENCE_URL), startsWith("http://arctos.database.museum/guid/"));
                assertThat(interaction.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), anyOf(
                        is("http://arctos.database.museum/guid/MVZ:Bird:180448?seid=587053"),
                        is("http://arctos.database.museum/guid/MVZ:Bird:183644?seid=158590"),
                        is("http://arctos.database.museum/guid/MVZ:Bird:58090?seid=657121")
                ));
                if (interaction.containsKey(DatasetImporterForTSV.TARGET_OCCURRENCE_ID)) {
                    assertThat(interaction.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), anyOf(
                            is("http://arctos.database.museum/guid/MVZ:Herp:241200"),
                            is("http://arctos.database.museum/guid/MVZ:Bird:183643"),
                            is("http://arctos.database.museum/guid/MVZ:Bird:58093")
                    ));
                }
                assertThat(interaction.get(SOURCE_TAXON_FAMILY), anyOf(
                        is("Accipitridae"),
                        is("Strigidae")
                ));

                someRecords.set(true);
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(someRecords.get(), is(true));
    }

    @Test
    public void importRecords() throws StudyImporterException, URISyntaxException, IOException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip");
        DatasetImporterForDwCA studyImporterForDwCA = new DatasetImporterForDwCA(null, null);
        DatasetImpl dataset = new DatasetImpl("some/namespace", resource.toURI(), inStream -> inStream);
        dataset.setConfig(new ObjectMapper().readTree("{ \"citation\": \"some citation\" }"));
        studyImporterForDwCA.setDataset(dataset);

        AtomicBoolean someRecords = new AtomicBoolean(false);
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                String associatedTaxa = interaction.get("http://rs.tdwg.org/dwc/terms/associatedTaxa");
                String dynamicProperties = interaction.get("http://rs.tdwg.org/dwc/terms/dynamicProperties");
                assertThat(StringUtils.isNotBlank(associatedTaxa) || StringUtils.isNotBlank(dynamicProperties), is(true));
                assertThat(interaction.get(SOURCE_TAXON_NAME), is(not(nullValue())));
                assertThat(interaction.get(TaxonUtil.TARGET_TAXON_NAME), is(not(nullValue())));
                assertThat(interaction.get(INTERACTION_TYPE_NAME), is(not(nullValue())));
                assertThat(interaction.get(DatasetImporterForTSV.DATASET_CITATION), containsString("some citation"));
                assertThat(interaction.get(DatasetImporterForTSV.DATASET_CITATION), containsString("Accessed at"));
                assertThat(interaction.get(DatasetImporterForTSV.DATASET_CITATION), containsString("dataset/dwca.zip"));
                assertThat(interaction.get(REFERENCE_ID), is(not(nullValue())));
                assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), is(not(nullValue())));
                assertThat(interaction.get(REFERENCE_URL), is(not(nullValue())));
                someRecords.set(true);
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(someRecords.get(), is(true));
    }

    @Test
    public void parseOwlPellets() {
        Map<String, String> properties = DatasetImporterForDwCA.parseRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks("Found in Burrowing Owl pellet");

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Burrowing Owl"));
        assertThat(properties.get(TARGET_BODY_PART_NAME), is("pellet"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("found in"));
    }

    @Test
    public void parseOwlPelletsRandom() {
        Map<String, String> properties = DatasetImporterForDwCA
                .parseRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks("hey, this is some random comment");

        assertThat(MapUtils.isEmpty(properties), is(true));
    }

    @Test
    public void parseOwlPelletsBlank() {
        Map<String, String> properties = DatasetImporterForDwCA
                .parseRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks("");

        assertThat(MapUtils.isEmpty(properties), is(true));
    }

    @Test
    public void parseOwlPelletsSpeciesUnknown() {
        Map<String, String> properties = DatasetImporterForDwCA
                .parseRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks("Found in owl pellet - species unknown");

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("owl"));
        assertThat(properties.get(TARGET_BODY_PART_NAME), is("pellet"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("found in"));
    }

    @Test
    public void parseOwlPelletsSpecies2() throws IOException {
        Map<String, String> properties = DatasetImporterForDwCA
                .parseRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks("Found in Northern Saw-Whet Owl pellet");

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Northern Saw-Whet Owl"));
        assertThat(properties.get(TARGET_BODY_PART_NAME), is("pellet"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("found in"));
    }

    @Test
    public void importRecordsFromZip() throws StudyImporterException, IOException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip");
        DatasetImporterForDwCA studyImporterForDwCA = new DatasetImporterForDwCA(null, null);

        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("file:///some/path/data.zip"), inStream -> inStream);
        JsonNode jsonNode = new ObjectMapper().readTree("{ " +
                "\"interactionTypeId\": \"http://purl.obolibrary.org/obo/RO_0002437\"," +
                "\"url\": \"" + resource.toExternalForm() + "\"" +
                "}");
        dataset.setConfig(jsonNode);
        studyImporterForDwCA.setDataset(dataset);
        String expectedCitation = dataset.getCitation();
        AtomicBoolean someRecords = new AtomicBoolean(false);
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                String associatedTaxa = interaction.get("http://rs.tdwg.org/dwc/terms/associatedTaxa");
                String dynamicProperties = interaction.get("http://rs.tdwg.org/dwc/terms/dynamicProperties");
                assertThat(StringUtils.isNotBlank(associatedTaxa) || StringUtils.isNotBlank(dynamicProperties), is(true));
                assertThat(interaction.get(SOURCE_TAXON_NAME), is(not(nullValue())));
                assertThat(interaction.get(TaxonUtil.TARGET_TAXON_NAME), is(not(nullValue())));
                assertThat(interaction.get(INTERACTION_TYPE_NAME), is(not(nullValue())));
                assertThat(interaction.get(DatasetImporterForTSV.DATASET_CITATION), containsString(expectedCitation));
                assertThat(interaction.get(REFERENCE_ID), startsWith("https://symbiota.ccber.ucsb.edu:443/collections/individual/index.php?occid"));
                assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), startsWith("https://symbiota.ccber.ucsb.edu:443/collections/individual/index.php?occid"));
                assertThat(interaction.get(REFERENCE_URL), startsWith("https://symbiota.ccber.ucsb.edu:443/collections/individual/index.php?occid"));
                someRecords.set(true);
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(someRecords.get(), is(true));
    }

    private void assertImportsSomething(URI archiveRoot, AtomicInteger recordCounter, String... expectedProperties) throws StudyImporterException {
        DatasetImporterForDwCA studyImporterForDwCA = new DatasetImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", archiveRoot, inStream -> inStream));
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                for (String expectedProperty : expectedProperties) {
                    assertThat("no [" + expectedProperty + "] found in " + interaction, interaction.containsKey(expectedProperty), is(true));
                    assertThat("no value of [" + expectedProperty + "] found in " + interaction, interaction.get(expectedProperty), is(notNullValue()));
                }

                recordCounter.incrementAndGet();
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(recordCounter.get(), greaterThan(0));
    }


    @Test
    // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/504
    public void occurrenceRemarks() throws IOException {
        String occurrenceRemarks = "2.5 gluteraldehyde Neutral red Permount {\"hostGen\":\"Biomphalaria\",\"hostSpec\":\"havanensis\"}";

        Map<String, String> properties = DatasetImporterForDwCA.parseUSNMStyleHostOccurrenceRemarks(occurrenceRemarks);


        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Biomphalaria havanensis"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_GENUS), is("Biomphalaria"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET), is("havanensis"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is(InteractType.HAS_HOST.getLabel()));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(InteractType.HAS_HOST.getIRI()));
    }

    @Test
    // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/504
    public void occurrenceRemarksComplex() throws IOException {
        String occurrenceRemarks = "{\"hostGen\":\"Hybopsis\",  \"hostSpec\":\"dorsalis\",  \"hostHiTax\":\"Pisces: Cypriniformes: Cyprinidae\",  \"hostSyn\":\"Notropis dorsalis\",   \"hostBodyLoc\":\"gills\", \"hostFldNo\":\"DCloutman-6028-4\",  \"hostMusNo\":\"KU-34829 (University of Kansas\"}";

        Map<String, String> properties = DatasetImporterForDwCA.parseUSNMStyleHostOccurrenceRemarks(occurrenceRemarks);


        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Hybopsis dorsalis"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_GENUS), is("Hybopsis"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET), is("dorsalis"));
        assertThat(properties.get(TARGET_FIELD_NUMBER), is("DCloutman-6028-4"));
        assertThat(properties.get(TARGET_CATALOG_NUMBER), is("KU-34829 (University of Kansas"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is(InteractType.HAS_HOST.getLabel()));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(InteractType.HAS_HOST.getIRI()));
    }

    @Test
    // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/504
    public void occurrenceRemarks2() throws IOException {
        String occurrenceRemarks = "4% Formaldehyde Original USNPC preservative was a solution of 70% ethanol, 3% formalin, and 2% glycerine " +
                "{\"hostGen\":\"Lutjanus\",\"hostSpec\":\"campechanus\",\"hostHiTax\":\"Actinopterygii: Pereciformes: Lutjanidae\",\"hostBodyLoc\":\"ovary\"}";

        Map<String, String> properties = DatasetImporterForDwCA.parseUSNMStyleHostOccurrenceRemarks(occurrenceRemarks);


        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Lutjanus campechanus"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_GENUS), is("Lutjanus"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET), is("campechanus"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_PATH), is("Actinopterygii | Pereciformes | Lutjanidae | Lutjanus | Lutjanus campechanus"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_PATH_NAMES), is("| | | genus | species"));
        assertThat(properties.get(TARGET_BODY_PART_NAME), is("ovary"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is(InteractType.HAS_HOST.getLabel()));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(InteractType.HAS_HOST.getIRI()));
    }

    @Test
    // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/504
    public void occurrenceRemarks3() throws IOException {
        String occurrenceRemarks = "AFA Acetocarmine Canada balsam " +
                "{\"hostGen\":\"Bryconamericus\"," +
                "\"hostSpec\":\"scleroparius\"," +
                "\"hostBodyLoc\":\"intestine\"," +
                "\"hostFldNo\":\"AChoudhury-BA-CR98-3\"}";

        Map<String, String> properties = DatasetImporterForDwCA.parseUSNMStyleHostOccurrenceRemarks(occurrenceRemarks);


        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Bryconamericus scleroparius"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_GENUS), is("Bryconamericus"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET), is("scleroparius"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_PATH), is("Bryconamericus | Bryconamericus scleroparius"));
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_PATH_NAMES), is("genus | species"));
        assertThat(properties.get(TARGET_BODY_PART_NAME), is("intestine"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is(InteractType.HAS_HOST.getLabel()));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(InteractType.HAS_HOST.getIRI()));
    }

    @Test(expected = IOException.class)
    // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/504
    public void occurrenceRemarksMalformed() throws IOException {
        String occurrenceRemarks = "{\"hostGen\":\"Potamotrygon\",\"hostSpec\":\"sp.\",\"hostHiTax\":\"Pisces: Rajiformes: Potamotrygonidae\",\"hostBodyLoc\":\"gill\",\"hostFldNo\":\"Code: AC06-069\",\"hostRemarks\":\"sp. \"jam1\"\"}";

        try {
            DatasetImporterForDwCA.parseUSNMStyleHostOccurrenceRemarks(occurrenceRemarks);
        } catch (IOException ex) {
            assertThat(ex.getMessage(), is("found likely malformed host description [{\"hostGen\":\"Potamotrygon\",\"hostSpec\":\"sp.\",\"hostHiTax\":\"Pisces: Rajiformes: Potamotrygonidae\",\"hostBodyLoc\":\"gill\",\"hostFldNo\":\"Code: AC06-069\",\"hostRemarks\":\"sp. \"jam1\"\"}], see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/505"));
            throw ex;
        }
    }


    @Test
    public void dynamicProperties() {
        String s = "targetTaxonName: Homo sapiens; targetTaxonId: https://www.gbif.org/species/2436436; interactionTypeName: eats; interactionTypeId: http://purl.obolibrary.org/obo/RO_0002470; targetBodyPartName: blood; targetBodyPartId: http://purl.obolibrary.org/obo/NCIT_C12434\",\"eats: Homo sapiens";
        Map<String, String> properties = parseDynamicPropertiesForInteractionsOnly(s);

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
    }

    @Test
    public void dynamicPropertiesEmptyOnNoInteractions() {
        String s = "targetTaxonName: Homo sapiens; targetTaxonId: https://www.gbif.org/species/2436436; targetBodyPartName: blood; targetBodyPartId: http://purl.obolibrary.org/obo/NCIT_C12434\",\"eats: Homo sapiens";
        Map<String, String> properties = parseDynamicPropertiesForInteractionsOnly(s);
        assertThat(properties.isEmpty(), is(true));
    }

    @Test
    public void dynamicProperties2() {
        String s = "sourceLifeStageName=pupae ; sourceLifeStageID= ; experimentalConditionName=in nature ; experimentalConditionID=http://purl.obolibrary.org/obo/ENVO_01001226 ; interactionTypeName=inside ; interactionTypeId=http://purl.obolibrary.org/obo/RO_0001025 ; targetTaxonName=Mus ; targetTaxonId=https://www.gbif.org/species/2311167";
        Map<String, String> properties = parseDynamicPropertiesForInteractionsOnly(s);

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Mus"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("inside"));
        assertThat(properties.get(DatasetImporterForTSV.SOURCE_LIFE_STAGE_NAME), is("pupae"));
        assertThat(properties.get(DatasetImporterForTSV.SOURCE_LIFE_STAGE_ID), is(nullValue()));
        assertThat(properties.get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0001025"));
    }


    @Test
    public void associatedOccurrences() {
        String associateOccurrences = "(eaten by) MVZ:Bird http://arctos.database.museum/guid/MVZ:Bird:183644";
        List<Map<String, String>> propertyList = parseAssociatedOccurrences(associateOccurrences);

        assertThat(propertyList.size(), is(1));

        Map<String, String> properties = propertyList.get(0);
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), is("http://arctos.database.museum/guid/MVZ:Bird:183644"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("(eaten by)"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));
    }


    @Test
    public void associatedOccurrencesMCZ() {
        String associateOccurrences = "parasitically found on/in          <a href=\"http://mczbase.mcz.harvard.edu/SpecimenDetail.cfm?collection_object_id=5197872\"> MCZ IZ ECH-8358</a>";
        List<Map<String, String>> propertyList = parseAssociatedOccurrences(associateOccurrences);

        assertThat(propertyList.size(), is(1));

        Map<String, String> properties = propertyList.get(0);
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(TARGET_OCCURRENCE_ID), is("MCZ:IZ:ECH-8358"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("parasitically found on/in"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedOccurrencesMCZLists() {
        String associateOccurrences = "from same lot as          <a href=\"http://mczbase.mcz.harvard.edu/SpecimenDetail.cfm?collection_object_id=666604\"> MCZ Mamm 3186</a>; from same lot as          <a href=\"http://mczbase.mcz.harvard.edu/SpecimenDetail.cfm?collection_object_id=666606\"> MCZ Mamm 3187</a>; from same lot as          <a href=\"http://mczbase.mcz.harvard.edu/SpecimenDetail.cfm?collection_object_id=666608\"> MCZ Mamm 3188</a>; from same lot as          <a href=\"http://mczbase.mcz.harvard.edu/SpecimenDetail.cfm?collection_object_id=666610\"> MCZ Mamm 3190</a>; from same lot as          <a href=\"http://mczbase.mcz.harvard.edu/SpecimenDetail.cfm?collection_object_id=666612\"> MCZ Mamm 3192</a>; from same lot as          <a href=\"http://mczbase.mcz.harvard.edu/SpecimenDetail.cfm?collection_object_id=678406\"> MCZ Mamm 3191</a>; from same lot as          <a href=\"http://mczbase.mcz.harvard.edu/SpecimenDetail.cfm?collection_object_id=730482\"> MCZ Mamm 3189</a>";
        List<Map<String, String>> propertyList = parseAssociatedOccurrences(associateOccurrences);

        assertThat(propertyList.size(), is(7));

        Map<String, String> properties = propertyList.get(0);
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(TARGET_OCCURRENCE_ID), is("MCZ:Mamm:3186"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("from same lot as"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));

        properties = propertyList.get(6);
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(TARGET_OCCURRENCE_ID), is("http://mczbase.mcz.harvard.edu/guid/MCZ:Mamm:3189"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("from same lot as"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedOccurrencesMalformed() {
        assertThat(parseAssociatedOccurrences("(eaten by)").size(), is(0));
    }

    @Test
    public void associatedOccurrencesMalformed2() {

        assertThat(parseAssociatedOccurrences("donald").size(), is(0));

    }

    @Test
    public void associatedOccurrences2() {
        String associateOccurrences = "(ate) DZTM: Denver Zoology Tissue Mammal 2822; (ate) DZTM: Denver Zoology Tissue Mammal 2823";
        List<Map<String, String>> propertyList = parseAssociatedOccurrences(associateOccurrences);

        assertThat(propertyList.size(), is(2));

        Map<String, String> properties = propertyList.get(0);
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), is("Denver Zoology Tissue Mammal 2822"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("(ate)"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));

        properties = propertyList.get(1);
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), is("Denver Zoology Tissue Mammal 2823"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("(ate)"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void hasAssociatedTaxaExtension() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("AEC-DBCNet_DwC-A20160308-sample.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        assertThat(DatasetImporterForDwCA.findResourceExtension(archive, EXTENSION_ASSOCIATED_TAXA),
                Is.is(notNullValue()));
    }

    @Test
    public void hasResourceRelationshipsExtension() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("fmnh-rr-test.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        assertThat(DatasetImporterForDwCA.findResourceExtension(archive, EXTENSION_RESOURCE_RELATIONSHIP),
                Is.is(notNullValue()));
    }

    @Test
    public void hasAssociatedTaxa() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("AEC-DBCNet_DwC-A20160308-sample.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        AtomicBoolean foundLink = new AtomicBoolean(false);
        importAssociatedTaxaExtension(archive, new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                assertThat(interaction.get(SOURCE_TAXON_NAME), is("Andrena wilkella"));
                assertThat(interaction.get(DatasetImporterForTSV.SOURCE_SEX_NAME), is("Female"));
                assertThat(interaction.get(DatasetImporterForTSV.SOURCE_LIFE_STAGE_NAME), is("Adult"));
                assertThat(interaction.get(TaxonUtil.TARGET_TAXON_NAME), is("Melilotus officinalis"));
                assertThat(interaction.get(INTERACTION_TYPE_NAME), is("associated with"));
                assertThat(interaction.get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002437"));
                assertThat(interaction.get(DatasetImporterForTSV.BASIS_OF_RECORD_NAME), is("LabelObservation"));

                assertThat(interaction.get(DatasetImporterForTSV.DECIMAL_LATITUDE), is("42.40000"));
                assertThat(interaction.get(DatasetImporterForTSV.DECIMAL_LONGITUDE), is("-76.50000"));
                assertThat(interaction.get(DatasetImporterForTSV.LOCALITY_NAME), is("Tompkins County"));
                assertThat(interaction.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), is("urn:uuid:859e1708-d8e1-11e2-99a2-0026552be7ea"));
                assertThat(interaction.get(DatasetImporterForTSV.SOURCE_COLLECTION_CODE), is(nullValue()));
                assertThat(interaction.get(DatasetImporterForTSV.SOURCE_COLLECTION_ID), is(nullValue()));
                assertThat(interaction.get(DatasetImporterForTSV.SOURCE_INSTITUTION_CODE), is("CUIC"));
                assertThat(interaction.get(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER), is("CUIC_ENT 00014070"));
                assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), is("Digital Bee Collections Network, 2014 (and updates). Version: 2016-03-08. National Science Foundation grant DBI 0956388"));
                foundLink.set(true);

            }
        });

        assertTrue(foundLink.get());
    }

    @Test
    public void hasResourceRelationshipsOccurrenceToOccurrence() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("fmnh-rr-test.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        AtomicInteger numberOfFoundLinks = new AtomicInteger(0);
        importResourceRelationExtension(archive, new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                numberOfFoundLinks.incrementAndGet();
                if (1 == numberOfFoundLinks.get()) {
                    assertThat(interaction.get(relatedResourceID.qualifiedName()), is("http://n2t.net/ark:/65665/37d63a454-d948-4b1d-89db-89809887ef41"));
                    assertThat(interaction.get(SOURCE_TAXON_NAME), is("Trichobius parasparsus Wenzel, 1976"));
                    assertThat(interaction.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), is("8afec7db-7b19-44f7-8ac8-8d98614e71d2"));
                    assertThat(interaction.get(INTERACTION_TYPE_NAME), is("Ectoparasite of"));
                    assertThat(interaction.get(INTERACTION_TYPE_ID), is(nullValue()));
                    assertThat(interaction.get(DatasetImporterForTSV.BASIS_OF_RECORD_NAME), is("PreservedSpecimen"));
                    assertThat(interaction.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
                    assertThat(interaction.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), is("http://n2t.net/ark:/65665/37d63a454-d948-4b1d-89db-89809887ef41"));
                    assertThat(interaction.get(DatasetImporterForTSV.TARGET_CATALOG_NUMBER), is(nullValue()));
                    assertThat(interaction.get(DatasetImporterForTSV.TARGET_COLLECTION_CODE), is(nullValue()));
                    assertThat(interaction.get(DatasetImporterForTSV.TARGET_INSTITUTION_CODE), is(nullValue()));
                    assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), is("A. L. Tuttle | M. D. Tuttle"));
                } else if (2 == numberOfFoundLinks.get()) {
                    assertThat(interaction.get(SOURCE_TAXON_NAME), is("Rhinolophus fumigatus aethiops"));
                    assertThat(interaction.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), is("7048675a-b110-4baf-91a3-2db138316709"));
                    assertThat(interaction.get(INTERACTION_TYPE_NAME), is("Host to"));
                    assertThat(interaction.get(INTERACTION_TYPE_ID), is(nullValue()));
                    assertThat(interaction.get(DatasetImporterForTSV.BASIS_OF_RECORD_NAME), is("PreservedSpecimen"));
                    assertThat(interaction.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
                    assertThat(interaction.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), is("10d8d814-2afc-4cf2-9843-a2b719346179"));
                    assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), is("G. Heinrich"));
                } else if (8 == numberOfFoundLinks.get()) {
                    assertThat(interaction.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), is("3efb94e7-5182-4dd3-bec5-aa838ba22b4f"));
                    assertThat(interaction.get(SOURCE_TAXON_NAME), is("Thamnophis fulvus"));

                    assertThat(interaction.get(INTERACTION_TYPE_NAME), is("Stomach Contents of"));
                    assertThat(interaction.get(INTERACTION_TYPE_ID), is(nullValue()));

                    assertThat(interaction.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), is("5c419063-682a-4b3f-8a27-9ed286717922"));
                    assertThat(interaction.get(TaxonUtil.TARGET_TAXON_NAME), is("Thamnophis fulvus"));

                    assertThat(interaction.get(DatasetImporterForTSV.BASIS_OF_RECORD_NAME), is("PreservedSpecimen"));
                    assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), is("C. M. Barber"));
                }
                assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), is(notNullValue()));
            }
        });

        assertThat(numberOfFoundLinks.get(), is(8));
    }

    @Test
    public void hasResourceRelationshipsOccurrenceToOccurrenceMissingTargetReference() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("fmnh-rr-unresolved-targetid-test.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        AtomicInteger numberOfFoundLinks = new AtomicInteger(0);
        importResourceRelationExtension(archive, new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                numberOfFoundLinks.incrementAndGet();
                assertThat(interaction.get(relatedResourceID.qualifiedName()), is("http://n2t.net/ark:/65665/37d63a454-d948-4b1d-89db-89809887ef41"));
                assertThat(interaction.get(SOURCE_TAXON_NAME), is("Trichobius parasparsus Wenzel, 1976"));
                assertThat(interaction.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), is("8afec7db-7b19-44f7-8ac8-8d98614e71d2"));
                assertThat(interaction.get(INTERACTION_TYPE_NAME), is("Ectoparasite of"));
                assertThat(interaction.get(INTERACTION_TYPE_ID), is(nullValue()));
                assertThat(interaction.get(DatasetImporterForTSV.BASIS_OF_RECORD_NAME), is("PreservedSpecimen"));
                assertThat(interaction.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
                assertThat(interaction.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), is("http://n2t.net/ark:/65665/37d63a454-d948-4b1d-89db-89809887ef41"));
                assertThat(interaction.get(DatasetImporterForTSV.TARGET_CATALOG_NUMBER), is(nullValue()));
                assertThat(interaction.get(DatasetImporterForTSV.TARGET_COLLECTION_CODE), is(nullValue()));
                assertThat(interaction.get(DatasetImporterForTSV.TARGET_INSTITUTION_CODE), is(nullValue()));
                assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), is("A. L. Tuttle | M. D. Tuttle"));
            }
        });

        assertThat(numberOfFoundLinks.get(), is(1));
    }

    @Test
    public void hasResourceRelationshipsOccurrenceToOccurrenceRemarks() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("fmnh-rr-remarks-test.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        AtomicInteger numberOfFoundLinks = new AtomicInteger(0);
        importResourceRelationExtension(archive, new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                numberOfFoundLinks.incrementAndGet();
                if (1 == numberOfFoundLinks.get()) {
                    assertThat(interaction.get(SOURCE_TAXON_NAME), is("Trichobius parasparsus Wenzel, 1976"));
                    assertThat(interaction.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), is("8afec7db-7b19-44f7-8ac8-8d98614e71d2"));
                    assertThat(interaction.get(INTERACTION_TYPE_NAME), is("Ectoparasite of"));
                    assertThat(interaction.get(INTERACTION_TYPE_ID), is(nullValue()));
                    assertThat(interaction.get(DatasetImporterForTSV.BASIS_OF_RECORD_NAME), is("PreservedSpecimen"));
                    assertThat(interaction.get(TaxonUtil.TARGET_TAXON_NAME), is("Donald duckus"));
                    assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), is("A. L. Tuttle | M. D. Tuttle"));
                }
                assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), is(notNullValue()));
            }
        });

        assertThat(numberOfFoundLinks.get(), is(1));
    }

    @Test
    public void hasResourceRelationshipsOccurrenceToTaxa() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("inaturalist-dwca-rr.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        AtomicInteger numberOfFoundLinks = new AtomicInteger(0);
        importResourceRelationExtension(archive, new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                numberOfFoundLinks.incrementAndGet();
                if (1 == numberOfFoundLinks.get()) {
                    assertThat(interaction.get(TaxonUtil.SOURCE_TAXON_ID), is("http://www.inaturalist.org/taxa/465153"));
                    assertThat(interaction.get(SOURCE_TAXON_NAME), is("Gorgonocephalus eucnemis"));
                    assertThat(interaction.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), is("http://www.inaturalist.org/observations/2309983"));
                    assertThat(interaction.get(INTERACTION_TYPE_NAME), is("Eaten by"));
                    assertThat(interaction.get(INTERACTION_TYPE_ID), is("http://www.inaturalist.org/observation_fields/879"));
                    assertThat(interaction.get(DatasetImporterForTSV.BASIS_OF_RECORD_NAME), is("HumanObservation"));
                    assertThat(interaction.get(TaxonUtil.TARGET_TAXON_ID), is("http://www.inaturalist.org/taxa/133061"));
                    assertThat(interaction.get(TaxonUtil.TARGET_TAXON_NAME), is("Enhydra lutris kenyoni"));
                    assertThat(interaction.get(DatasetImporterForTSV.REFERENCE_CITATION), is("https://www.inaturalist.org/users/dpom"));
                }

            }
        });

        assertThat(numberOfFoundLinks.get(), is(1));
    }


    @Test
    public void mapReferencesInfo() {
        DummyRecord dummyRecord = new DummyRecord(new HashMap<Term, String>() {{
            put(DcTerm.references,
                    "some reference");
        }});

        TreeMap<String, String> properties = new TreeMap<>();
        mapReferenceInfo(dummyRecord, properties);
        assertThat(properties.get(REFERENCE_CITATION), is("some reference"));
        assertThat(properties.get(REFERENCE_ID), is("some reference"));
        assertThat(properties.get(REFERENCE_URL), is(nullValue()));

    }

    @Test
    public void mapReferencesInfoValidURIButInvalidURL() {
        DummyRecord dummyRecord = new DummyRecord(new HashMap<Term, String>() {{
            put(DwcTerm.occurrenceID,
                    "something");
        }});

        TreeMap<String, String> properties = new TreeMap<>();
        mapReferenceInfo(dummyRecord, properties);
        assertThat(properties.get(REFERENCE_CITATION), is("something"));
        assertThat(properties.get(REFERENCE_ID), is("something"));
        assertThat(properties.get(REFERENCE_URL), is(nullValue()));

    }

    @Test
    public void mapReferencesInfoWithURL() {
        DummyRecord dummyRecord = new DummyRecord(new HashMap<Term, String>() {{
            put(DcTerm.references,
                    "https://example.org");
        }});
        TreeMap<String, String> properties = new TreeMap<>();
        mapReferenceInfo(dummyRecord, properties);
        assertThat(properties.get(REFERENCE_CITATION), is("https://example.org"));
        assertThat(properties.get(REFERENCE_ID), is("https://example.org"));
        assertThat(properties.get(REFERENCE_URL), is("https://example.org"));
    }

    @Test
    public void mapReferencesInfoWithOccurrenceId() {
        DummyRecord dummyRecord = new DummyRecord(new HashMap<Term, String>() {{
            put(DwcTerm.occurrenceID,
                    "https://example.org");
        }});
        TreeMap<String, String> properties = new TreeMap<>();
        mapReferenceInfo(dummyRecord, properties);
        assertThat(properties.get(REFERENCE_CITATION), is("https://example.org"));
        assertThat(properties.get(REFERENCE_ID), is("https://example.org"));
        assertThat(properties.get(REFERENCE_URL), is("https://example.org"));
    }

    private class DummyRecord implements Record {

        private final Map<Term, String> valueMap;

        DummyRecord(Map<Term, String> valueMap) {
            this.valueMap = valueMap;
        }

        @Override
        public String id() {
            return null;
        }

        @Override
        public Term rowType() {
            return null;
        }

        @Override
        public String value(Term term) {
            return valueMap.get(term);
        }

        @Override
        public String column(int i) {
            return null;
        }

        @Override
        public Set<Term> terms() {
            return null;
        }
    }
}
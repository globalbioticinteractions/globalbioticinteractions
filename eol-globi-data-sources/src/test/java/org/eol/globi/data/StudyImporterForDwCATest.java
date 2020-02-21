package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.tool.NullImportLogger;
import org.gbif.dwc.Archive;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DwCAUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eol.globi.data.StudyImporterForDwCA.hasResourceRelationships;
import static org.eol.globi.data.StudyImporterForDwCA.importAssociatedTaxaExtension;
import static org.eol.globi.data.StudyImporterForDwCA.importResourceRelationExtension;
import static org.eol.globi.data.StudyImporterForDwCA.parseAssociatedOccurrences;
import static org.eol.globi.data.StudyImporterForDwCA.parseAssociatedTaxa;
import static org.eol.globi.data.StudyImporterForDwCA.parseDynamicProperties;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_FAMILY;
import static org.gbif.dwc.terms.DwcTerm.relatedResourceID;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StudyImporterForDwCATest {

    @Test
    public void importRecordsFromDir() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/vampire-moth-dwca-master/meta.xml");
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
        StudyImporterForDwCA studyImporterForDwCA = new StudyImporterForDwCA(null, null);
        studyImporterForDwCA.setLogger(new NullImportLogger() {
            @Override
            public void severe(LogContext ctx, String message) {
                actualMessage.append(message);
            }
        });
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", archiveRoot, inStream -> inStream));
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                for (String expectedProperty : new String[]{}) {
                    assertThat("no [" + expectedProperty + "] found in " + properties, properties.containsKey(expectedProperty), is(true));
                    assertThat("no value of [" + expectedProperty + "] found in " + properties, properties.get(expectedProperty), is(notNullValue()));
                }

                recordCounter.incrementAndGet();
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(recordCounter.get(), is(0));
        assertThat(actualMessage.toString(), startsWith("[failed to handle dwc record]"));
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
                TaxonUtil.SOURCE_TAXON_NAME,
                StudyImporterForTSV.INTERACTION_TYPE_NAME,
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
    public void importRecordsFromArchiveWithAssociatedTaxa() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/eol/globi/data/AEC-DBCNet_DwC-A20160308-sample.zip");
        assertImportsSomething(resource.toURI(), new AtomicInteger(0));
    }

    @Test
    public void importRecordsFromArctosArchive() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/arctos_mvz_bird_small.zip");
        StudyImporterForDwCA studyImporterForDwCA = new StudyImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", resource.toURI(), inStream -> inStream));

        List<String> families = new ArrayList<>();
        AtomicBoolean someRecords = new AtomicBoolean(false);
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_URL), startsWith("http://arctos.database.museum/guid/"));
                assertThat(properties.get(StudyImporterForTSV.SOURCE_OCCURRENCE_ID), anyOf(
                        is("http://arctos.database.museum/guid/MVZ:Bird:180448?seid=587053"),
                        is("http://arctos.database.museum/guid/MVZ:Bird:183644?seid=158590"),
                        is("http://arctos.database.museum/guid/MVZ:Bird:58090?seid=657121")
                ));
                if (properties.containsKey(StudyImporterForTSV.TARGET_OCCURRENCE_ID)) {
                    assertThat(properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID), anyOf(
                            is("http://arctos.database.museum/guid/MVZ:Herp:241200"),
                            is("http://arctos.database.museum/guid/MVZ:Bird:183643"),
                            is("http://arctos.database.museum/guid/MVZ:Bird:58093")
                    ));
                }
                assertThat(properties.get(SOURCE_TAXON_FAMILY), anyOf(
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
        StudyImporterForDwCA studyImporterForDwCA = new StudyImporterForDwCA(null, null);
        DatasetImpl dataset = new DatasetImpl("some/namespace", resource.toURI(), inStream -> inStream);
        dataset.setConfig(new ObjectMapper().readTree("{ \"citation\": \"some citation\" }"));
        studyImporterForDwCA.setDataset(dataset);

        AtomicBoolean someRecords = new AtomicBoolean(false);
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                String associatedTaxa = properties.get("http://rs.tdwg.org/dwc/terms/associatedTaxa");
                String dynamicProperties = properties.get("http://rs.tdwg.org/dwc/terms/dynamicProperties");
                assertThat(StringUtils.isNotBlank(associatedTaxa) || StringUtils.isNotBlank(dynamicProperties), is(true));
                assertThat(properties.get(TaxonUtil.SOURCE_TAXON_NAME), is(not(nullValue())));
                assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.STUDY_SOURCE_CITATION), containsString("some citation"));
                assertThat(properties.get(StudyImporterForTSV.STUDY_SOURCE_CITATION), containsString("Accessed at"));
                assertThat(properties.get(StudyImporterForTSV.STUDY_SOURCE_CITATION), containsString("dataset/dwca.zip"));
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_ID), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_CITATION), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_URL), is(not(nullValue())));
                someRecords.set(true);
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(someRecords.get(), is(true));
    }

    @Test
    public void importRecordsFromZip() throws StudyImporterException, URISyntaxException, IOException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip");
        StudyImporterForDwCA studyImporterForDwCA = new StudyImporterForDwCA(null, null);

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
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                String associatedTaxa = properties.get("http://rs.tdwg.org/dwc/terms/associatedTaxa");
                String dynamicProperties = properties.get("http://rs.tdwg.org/dwc/terms/dynamicProperties");
                assertThat(StringUtils.isNotBlank(associatedTaxa) || StringUtils.isNotBlank(dynamicProperties), is(true));
                assertThat(properties.get(TaxonUtil.SOURCE_TAXON_NAME), is(not(nullValue())));
                assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is(not(nullValue())));
                assertThat(properties.get(INTERACTION_TYPE_ID), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.STUDY_SOURCE_CITATION), containsString(expectedCitation));
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_ID), startsWith("https://symbiota.ccber.ucsb.edu:443/collections/individual/index.php?occid"));
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_CITATION), startsWith("https://symbiota.ccber.ucsb.edu:443/collections/individual/index.php?occid"));
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_URL), startsWith("https://symbiota.ccber.ucsb.edu:443/collections/individual/index.php?occid"));
                someRecords.set(true);
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(someRecords.get(), is(true));
    }

    private void assertImportsSomething(URI archiveRoot, AtomicInteger recordCounter, String... expectedProperties) throws StudyImporterException {
        StudyImporterForDwCA studyImporterForDwCA = new StudyImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", archiveRoot, inStream -> inStream));
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                for (String expectedProperty : expectedProperties) {
                    assertThat("no [" + expectedProperty + "] found in " + properties, properties.containsKey(expectedProperty), is(true));
                    assertThat("no value of [" + expectedProperty + "] found in " + properties, properties.get(expectedProperty), is(notNullValue()));
                }

                recordCounter.incrementAndGet();
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(recordCounter.get(), greaterThan(0));
    }

    @Test
    public void associatedTaxa() {
        String associatedTaxa = "eats: Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxa2() {
        String associatedTaxa = " visitsFlowersOf:Eschscholzia californica";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Eschscholzia californica"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("visitsFlowersOf"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxa3() {
        String associatedTaxa = " visitsFlowersOf : Lupinus succulentus";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Lupinus succulentus"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("visitsFlowersOf"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxa4() {
        String associatedTaxa = "visitsFlowersOf: Phallia";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Phallia"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("visitsFlowersOf"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaMultiple() {
        String associatedTaxa = "eats: Homo sapiens | eats: Canis lupus";
        assertTwoInteractions(associatedTaxa);
    }

    @Test
    public void associatedTaxaEx() {
        String associatedTaxa = "ex Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("ex Homo sapiens"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("ex"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaRearedEx() {
        String associatedTaxa = "ReAred ex Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("ReAred ex Homo sapiens"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("reared ex"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxonomicHierachy() {
        String associatedTaxa = "Caesalpinaceae: Cercidium: praecox";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Cercidium praecox"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(InteractType.INTERACTS_WITH.getIRI()));
    }

    @Test
    public void associatedTaxonomicHierachy2() {
        String associatedTaxa = "Bucephala albeola: Anatidae";

        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Bucephala albeola"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(InteractType.INTERACTS_WITH.getIRI()));
    }

    @Test
    public void associatedTaxaExPeriod() {
        String associatedTaxa = "ex. Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("ex. Homo sapiens"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("ex"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaMultipleBlanks() {
        String associatedTaxa = "Homo sapiens, ,Felis catus";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(2));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("interactsWith"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002437"));
        assertThat(properties.get(1).get(TaxonUtil.TARGET_TAXON_NAME), is("Felis catus"));
        assertThat(properties.get(1).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("interactsWith"));
        assertThat(properties.get(1).get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002437"));
    }

    @Test
    public void associatedTaxaMultipleBlanks2() {
        String associatedTaxa = "V. priceana, , V. papilionacea";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(2));
    }

    @Test
    public void associatedTaxaMultipleCommas() {
        String associatedTaxa = "Ceramium, Chaetomorpha linum, Enteromorpha intestinalis, Ulva angusta, Porphyra perforata, Sargassum muticum, Gigartina spp., Rhodoglossum affine, and Grateloupia sp.";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(9));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Ceramium"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("interactsWith"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002437"));
        assertThat(properties.get(8).get(TaxonUtil.TARGET_TAXON_NAME), is("and Grateloupia sp."));
        assertThat(properties.get(8).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("interactsWith"));
        assertThat(properties.get(8).get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002437"));
    }

    @Test
    public void associatedTaxaMixed() {
        String associatedTaxa = "Ceramium, Chaetomorpha linum| Enteromorpha intestinalis; Ulva angusta, Porphyra perforata, Sargassum muticum, Gigartina spp., Rhodoglossum affine, and Grateloupia sp.";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(9));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Ceramium"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("interactsWith"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002437"));
        assertThat(properties.get(8).get(TaxonUtil.TARGET_TAXON_NAME), is("and Grateloupia sp."));
        assertThat(properties.get(8).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("interactsWith"));
        assertThat(properties.get(8).get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002437"));
    }

    @Test
    public void associatedTaxaMultipleSemicolon() {
        String associatedTaxa = "eats: Homo sapiens ; eats: Canis lupus";
        assertTwoInteractions(associatedTaxa);
    }

    public void assertTwoInteractions(String associatedTaxa) {
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(2));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(1).get(TaxonUtil.TARGET_TAXON_NAME), is("Canis lupus"));
        assertThat(properties.get(1).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(1).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaUnsupported() {
        String associatedTaxa = "eatz: Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eatz"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }


    @Test
    public void dynamicProperties() {
        String s = "targetTaxonName: Homo sapiens; targetTaxonId: https://www.gbif.org/species/2436436; interactionTypeName: eats; interactionTypeId: http://purl.obolibrary.org/obo/RO_0002470; targetBodyPartName: blood; targetBodyPartId: http://purl.obolibrary.org/obo/NCIT_C12434\",\"eats: Homo sapiens";
        Map<String, String> properties = parseDynamicProperties(s);

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
    }

    @Test
    public void dynamicProperties2() {
        String s = "sourceLifeStageName=pupae ; sourceLifeStageID= ; experimentalConditionName=in nature ; experimentalConditionID=http://purl.obolibrary.org/obo/ENVO_01001226 ; interactionTypeName=inside ; interactionTypeId=http://purl.obolibrary.org/obo/RO_0001025 ; targetTaxonName=Mus ; targetTaxonId=https://www.gbif.org/species/2311167";
        Map<String, String> properties = parseDynamicProperties(s);

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Mus"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("inside"));
        assertThat(properties.get(StudyImporterForTSV.SOURCE_LIFE_STAGE_NAME), is("pupae"));
        assertThat(properties.get(StudyImporterForTSV.SOURCE_LIFE_STAGE_ID), is(nullValue()));
        assertThat(properties.get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0001025"));
    }


    @Test
    public void associatedOccurrences() {
        String associateOccurrences = "(eaten by) MVZ:Bird http://arctos.database.museum/guid/MVZ:Bird:183644";
        List<Map<String, String>> propertyList = parseAssociatedOccurrences(associateOccurrences);

        assertThat(propertyList.size(), is(1));

        Map<String, String> properties = propertyList.get(0);
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID), is("http://arctos.database.museum/guid/MVZ:Bird:183644"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("(eaten by)"));
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
        assertThat(properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID), is("Denver Zoology Tissue Mammal 2822"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("(ate)"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));

        properties = propertyList.get(1);
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID), is("Denver Zoology Tissue Mammal 2823"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("(ate)"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void hasAssociatedTaxaExtension() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("AEC-DBCNet_DwC-A20160308-sample.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        assertTrue(StudyImporterForDwCA.hasAssociatedTaxaExtension(archive));
    }

    @Test
    public void hasResourceRelationshipsExtension() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("fmnh-rr-test.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        assertTrue(hasResourceRelationships(archive));
    }

    @Test
    public void hasAssociatedTaxa() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("AEC-DBCNet_DwC-A20160308-sample.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        AtomicBoolean foundLink = new AtomicBoolean(false);
        importAssociatedTaxaExtension(archive, new InteractionListener() {

            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                assertThat(properties.get(TaxonUtil.SOURCE_TAXON_NAME), is("Andrena wilkella"));
                assertThat(properties.get(StudyImporterForTSV.SOURCE_SEX_NAME), is("Female"));
                assertThat(properties.get(StudyImporterForTSV.SOURCE_LIFE_STAGE_NAME), is("Adult"));
                assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Melilotus officinalis"));
                assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("associated with"));
                assertThat(properties.get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002437"));
                assertThat(properties.get(StudyImporterForTSV.BASIS_OF_RECORD_NAME), is("LabelObservation"));

                assertThat(properties.get(StudyImporterForTSV.DECIMAL_LATITUDE), is("42.40000"));
                assertThat(properties.get(StudyImporterForTSV.DECIMAL_LONGITUDE), is("-76.50000"));
                assertThat(properties.get(StudyImporterForTSV.LOCALITY_NAME), is("Tompkins County"));
                assertThat(properties.get(StudyImporterForTSV.SOURCE_OCCURRENCE_ID), is("urn:uuid:859e1708-d8e1-11e2-99a2-0026552be7ea"));
                assertThat(properties.get(StudyImporterForTSV.SOURCE_COLLECTION_CODE), is(nullValue()));
                assertThat(properties.get(StudyImporterForTSV.SOURCE_COLLECTION_ID), is(nullValue()));
                assertThat(properties.get(StudyImporterForTSV.SOURCE_INSTITUTION_CODE), is("CUIC"));
                assertThat(properties.get(StudyImporterForTSV.SOURCE_CATALOG_NUMBER), is("CUIC_ENT 00014070"));
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_CITATION), is("Digital Bee Collections Network, 2014 (and updates). Version: 2016-03-08. National Science Foundation grant DBI 0956388"));
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
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                numberOfFoundLinks.incrementAndGet();
                if (1 == numberOfFoundLinks.get()) {
                    assertThat(properties.get(relatedResourceID.qualifiedName()), is("http://n2t.net/ark:/65665/37d63a454-d948-4b1d-89db-89809887ef41"));
                    assertThat(properties.get(TaxonUtil.SOURCE_TAXON_NAME), is("Trichobius parasparsus Wenzel, 1976"));
                    assertThat(properties.get(StudyImporterForTSV.SOURCE_OCCURRENCE_ID), is("8afec7db-7b19-44f7-8ac8-8d98614e71d2"));
                    assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("Ectoparasite of"));
                    assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));
                    assertThat(properties.get(StudyImporterForTSV.BASIS_OF_RECORD_NAME), is("PreservedSpecimen"));
                    assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
                    assertThat(properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID), is("http://n2t.net/ark:/65665/37d63a454-d948-4b1d-89db-89809887ef41"));
                    assertThat(properties.get(StudyImporterForTSV.TARGET_CATALOG_NUMBER), is(nullValue()));
                    assertThat(properties.get(StudyImporterForTSV.TARGET_COLLECTION_CODE), is(nullValue()));
                    assertThat(properties.get(StudyImporterForTSV.TARGET_INSTITUTION_CODE), is(nullValue()));
                    assertThat(properties.get(StudyImporterForTSV.REFERENCE_CITATION), is("A. L. Tuttle | M. D. Tuttle"));
                } else if (2 == numberOfFoundLinks.get()) {
                    assertThat(properties.get(TaxonUtil.SOURCE_TAXON_NAME), is("Rhinolophus fumigatus aethiops"));
                    assertThat(properties.get(StudyImporterForTSV.SOURCE_OCCURRENCE_ID), is("7048675a-b110-4baf-91a3-2db138316709"));
                    assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("Host to"));
                    assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));
                    assertThat(properties.get(StudyImporterForTSV.BASIS_OF_RECORD_NAME), is("PreservedSpecimen"));
                    assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is(nullValue()));
                    assertThat(properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID), is("10d8d814-2afc-4cf2-9843-a2b719346179"));
                    assertThat(properties.get(StudyImporterForTSV.REFERENCE_CITATION), is("G. Heinrich"));
                } else if (9 == numberOfFoundLinks.get()) {
                    assertThat(properties.get(TaxonUtil.SOURCE_TAXON_NAME), is("Thamnophis fulvus"));
                    assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("Stomach Contents"));
                    assertThat(properties.get(INTERACTION_TYPE_ID), is(nullValue()));
                    assertThat(properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID), is("5c419063-682a-4b3f-8a27-9ed286717922"));
                    assertThat(properties.get(StudyImporterForTSV.SOURCE_OCCURRENCE_ID), is("3efb94e7-5182-4dd3-bec5-aa838ba22b4f"));
                    assertThat(properties.get(StudyImporterForTSV.BASIS_OF_RECORD_NAME), is("PreservedSpecimen"));
                    assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Thamnophis fulvus"));
                    assertThat(properties.get(StudyImporterForTSV.REFERENCE_CITATION), is("C. M. Barber"));
                }
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_CITATION), is(notNullValue()));
            }
        });

        assertThat(numberOfFoundLinks.get(), is(8));
    }

    @Test
    public void hasResourceRelationshipsOccurrenceToTaxa() throws IOException, URISyntaxException {
        URI sampleArchive = getClass().getResource("inaturalist-dwca-rr.zip").toURI();

        Archive archive = DwCAUtil.archiveFor(sampleArchive, "target/tmp");

        AtomicInteger numberOfFoundLinks = new AtomicInteger(0);
        importResourceRelationExtension(archive, new InteractionListener() {

            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                numberOfFoundLinks.incrementAndGet();
                if (1 == numberOfFoundLinks.get()) {
                    assertThat(properties.get(TaxonUtil.SOURCE_TAXON_ID), is("http://www.inaturalist.org/taxa/465153"));
                    assertThat(properties.get(TaxonUtil.SOURCE_TAXON_NAME), is("Gorgonocephalus eucnemis"));
                    assertThat(properties.get(StudyImporterForTSV.SOURCE_OCCURRENCE_ID), is("http://www.inaturalist.org/observations/2309983"));
                    assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("Eaten by"));
                    assertThat(properties.get(INTERACTION_TYPE_ID), is("http://www.inaturalist.org/observation_fields/879"));
                    assertThat(properties.get(StudyImporterForTSV.BASIS_OF_RECORD_NAME), is("HumanObservation"));
                    assertThat(properties.get(TaxonUtil.TARGET_TAXON_ID), is("http://www.inaturalist.org/taxa/133061"));
                    assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Enhydra lutris kenyoni"));
                    assertThat(properties.get(StudyImporterForTSV.REFERENCE_CITATION), is("https://www.inaturalist.org/users/dpom"));
                }

            }
        });

        assertThat(numberOfFoundLinks.get(), is(1));
    }


}
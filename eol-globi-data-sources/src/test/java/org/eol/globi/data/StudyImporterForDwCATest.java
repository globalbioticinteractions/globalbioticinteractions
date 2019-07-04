package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.LogContext;
import org.eol.globi.service.DatasetImpl;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class StudyImporterForDwCATest {

    @Test
    public void importRecordsFromDir() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/vampire-moth-dwca-master/meta.xml");
        URI archiveRoot = new File(resource.toURI()).getParentFile().toURI();
        assertImportsSomething(archiveRoot);
    }

    @Test
    public void importRecordsFromArchive() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip");
        assertImportsSomething(resource.toURI());
    }

    @Test
    public void importRecordsFromArctosArchive() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/arctos_mvz_bird_small.zip");
        StudyImporterForDwCA studyImporterForDwCA = new StudyImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", resource.toURI()));
        AtomicBoolean someRecords = new AtomicBoolean(false);
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_URL), startsWith("http://arctos.database.museum/guid/"));
                someRecords.set(true);
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(someRecords.get(), is(true));
    }

    @Test
    public void importRecords() throws StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip");
        StudyImporterForDwCA studyImporterForDwCA = new StudyImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", resource.toURI()));
        AtomicBoolean someRecords = new AtomicBoolean(false);
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                String associatedTaxa = properties.get("http://rs.tdwg.org/dwc/terms/associatedTaxa");
                String dynamicProperties = properties.get("http://rs.tdwg.org/dwc/terms/dynamicProperties");
                assertThat(StringUtils.isNotBlank(associatedTaxa) || StringUtils.isNotBlank(dynamicProperties), is(true));
                assertThat(properties.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.TARGET_TAXON_NAME), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.STUDY_SOURCE_CITATION), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_ID), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_CITATION), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.REFERENCE_URL), is(not(nullValue())));
                someRecords.set(true);
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(someRecords.get(), is(true));
    }

    private void assertImportsSomething(URI archiveRoot) throws StudyImporterException {
        StudyImporterForDwCA studyImporterForDwCA = new StudyImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", archiveRoot));
        AtomicBoolean someRecords = new AtomicBoolean(false);
        studyImporterForDwCA.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                someRecords.set(true);
            }
        });
        studyImporterForDwCA.importStudy();
        assertThat(someRecords.get(), is(true));
    }

    @Test
    public void associatedTaxa() {
        String associatedTaxa = "eats: Homo sapiens";
        List<Map<String, String>> properties = StudyImporterForDwCA.parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
    }

    @Test
    public void associatedTaxaUnsupported() {
        String associatedTaxa = "eatz: Homo sapiens";
        List<Map<String, String>> properties = StudyImporterForDwCA.parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eatz"));
        assertThat(properties.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void logUnsupported() {
        String associatedTaxa = "eatz: Homo sapiens";
        List<Map<String, String>> properties = StudyImporterForDwCA.parseAssociatedTaxa(associatedTaxa);

        final AtomicBoolean loggedSomething = new AtomicBoolean(false);
        StudyImporterForDwCA.logUnsupportedInteractionTypes(properties, new ImportLogger() {
            @Override
            public void warn(LogContext study, String message) {
                assertThat(message, is("found unsupported interaction type [eatz]"));
                loggedSomething.set(true);
            }

            @Override
            public void info(LogContext study, String message) {

            }

            @Override
            public void severe(LogContext study, String message) {

            }
        });
        assertThat(loggedSomething.get(), is(true));
    }

    @Test
    public void notLogSupported() {
        String associatedTaxa = "eats: Homo sapiens";
        List<Map<String, String>> properties = StudyImporterForDwCA.parseAssociatedTaxa(associatedTaxa);

        StudyImporterForDwCA.logUnsupportedInteractionTypes(properties, new ImportLogger() {
            @Override
            public void warn(LogContext study, String message) {
                fail("boom!");
            }

            @Override
            public void info(LogContext study, String message) {

            }

            @Override
            public void severe(LogContext study, String message) {

            }
        });
    }

    @Test
    public void dynamicProperties() {
        String s = "targetTaxonName: Homo sapiens; targetTaxonId: https://www.gbif.org/species/2436436; interactionTypeName: eats; interactionTypeId: http://purl.obolibrary.org/obo/RO_0002470; targetBodyPartName: blood; targetBodyPartId: http://purl.obolibrary.org/obo/NCIT_C12434\",\"eats: Homo sapiens";
        Map<String, String> properties = StudyImporterForDwCA.parseDynamicProperties(s);

        assertThat(properties.get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
    }


    @Test
    public void associatedOccurrences() {
        String associateOccurrences = "(eaten by) MVZ:Bird http://arctos.database.museum/guid/MVZ:Bird:183644";
        List<Map<String, String>> propertyList = StudyImporterForDwCA.parseAssociatedOccurrences(associateOccurrences);

        assertThat(propertyList.size(), is(1));

        Map<String, String> properties = propertyList.get(0);
        assertThat(properties.get(StudyImporterForTSV.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID), is("http://arctos.database.museum/guid/MVZ:Bird:183644"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eatenBy"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002471"));
    }

    @Test
    public void associatedOccurrences2() {
        String associateOccurrences = "(ate) DZTM: Denver Zoology Tissue Mammal 2822; (ate) DZTM: Denver Zoology Tissue Mammal 2823";
        List<Map<String, String>> propertyList = StudyImporterForDwCA.parseAssociatedOccurrences(associateOccurrences);

        assertThat(propertyList.size(), is(2));

        Map<String, String> properties = propertyList.get(0);
        assertThat(properties.get(StudyImporterForTSV.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID), is("Denver Zoology Tissue Mammal 2822"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));

        properties = propertyList.get(1);
        assertThat(properties.get(StudyImporterForTSV.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID), is("Denver Zoology Tissue Mammal 2823"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
    }

}
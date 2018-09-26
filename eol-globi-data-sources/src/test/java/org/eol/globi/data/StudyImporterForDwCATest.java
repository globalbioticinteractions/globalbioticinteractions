package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.DatasetImpl;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class StudyImporterForDwCATest {

    @Test
    public void importRecordsFromDir() throws IOException, StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/vampire-moth-dwca-master/meta.xml");
        URI archiveRoot = new File(resource.toURI()).getParentFile().toURI();
        assertImportsSomething(archiveRoot);
    }

    @Test
    public void importRecordsFromArchive() throws IOException, StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip");
        assertImportsSomething(resource.toURI());
    }

    @Test
    public void importRecords() throws IOException, StudyImporterException, URISyntaxException {
        URL resource = getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip");
        StudyImporterForDwCA studyImporterForDwCA = new StudyImporterForDwCA(null, null);
        studyImporterForDwCA.setDataset(new DatasetImpl("some/namespace", resource.toURI()));
        AtomicBoolean someRecords = new AtomicBoolean(false);
        studyImporterForDwCA.setListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                String associatedTaxa = properties.get("http://rs.tdwg.org/dwc/terms/associatedTaxa");
                String dynamicProperties = properties.get("http://rs.tdwg.org/dwc/terms/dynamicProperties");
                assertThat(StringUtils.isNotBlank(associatedTaxa) || StringUtils.isNotBlank(dynamicProperties), is(true));
                assertThat(properties.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.TARGET_TAXON_NAME), is(not(nullValue())));
                assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is(not(nullValue())));
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
        studyImporterForDwCA.setListener(new InteractionListener() {
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
    }

    @Test
    public void dynamicProperties() {
        String s = "targetTaxonName: Homo sapiens; targetTaxonId: https://www.gbif.org/species/2436436; interactionTypeName: eats; interactionTypeId: http://purl.obolibrary.org/obo/RO_0002470; targetBodyPartName: blood; targetBodyPartId: http://purl.obolibrary.org/obo/NCIT_C12434\",\"eats: Homo sapiens";
        Map<String, String> properties = StudyImporterForDwCA.parseDynamicProperties(s);

        assertThat(properties.get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
    }

}
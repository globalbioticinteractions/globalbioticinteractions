package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.InteractTypeMapper;
import org.eol.globi.util.InteractTypeMapperFactoryImpl;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.globalbioticinteractions.util.OpenBiodivClientImpl;
import org.globalbioticinteractions.util.SparqlClient;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.TestCase.assertNotNull;
import static org.eol.globi.data.DatasetImporterForPensoftTest.getTableObj;
import static org.eol.globi.data.DatasetImporterForPensoftTest.parseRowsAndEnrich;
import static org.eol.globi.data.TestUtil.getResourceServiceTest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

public class DatasetImporterForPensoftIT {

    @Test
    public void importStudy() throws StudyImporterException, URISyntaxException {
        final DatasetImporterForPensoft importer = new DatasetImporterForPensoft(new ParserFactoryLocal(getClass()), null);
        final Dataset dataset = new DatasetWithResourceMapping("some/name", URI.create("some:uri"), new ResourceServiceLocalAndRemote(in -> in));
        final ObjectNode objectNode = new ObjectMapper().createObjectNode();
        final URL resource = getClass().getResource("pensoft/annotated-tables-first-two.json");
        objectNode.put("url", resource.toURI().toString());
        objectNode.put("citation", "some dataset citation");
        dataset.setConfig(objectNode);
        List<Map<String, String>> links = new ArrayList<>();

        importer.setDataset(dataset);
        importer.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                links.add(interaction);
            }
        });
        importer.importStudy();

        assertThat(links.size(), is(121));

        assertThat(links.get(0), hasEntry("Family Name", "Acanthaceae"));
        assertThat(links.get(0), hasEntry("Family Name_expanded_taxon_name", "Acanthaceae"));
        assertThat(links.get(0), hasEntry("Family Name_expanded_taxon_id", "http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(links.get(0), hasEntry("referenceUrl", "https://doi.org/10.3897/zookeys.306.5455"));
        assertThat(links.get(0), hasEntry("referenceDoi", "10.3897/zookeys.306.5455"));
        assertThat(links.get(0), hasEntry("referenceCitation", "Dewi Sartiami, Laurence A. Mound. 2013. Identification of the terebrantian thrips (Insecta, Thysanoptera) associated with cultivated plants in Java, Indonesia. ZooKeys. https://doi.org/10.3897/zookeys.306.5455"));
    }

    @Test
    public void retrieveCitation() throws IOException {
        final SparqlClient sparqlClientImpl = new OpenBiodivClientImpl(getResourceServiceTest());
        String citation = DatasetImporterForPensoft.findCitationByDoi("10.3897/zookeys.306.5455", sparqlClientImpl);
        assertThat(citation, is("Dewi Sartiami, Laurence A. Mound. 2013. Identification of the terebrantian thrips (Insecta, Thysanoptera) associated with cultivated plants in Java, Indonesia. ZooKeys. https://doi.org/10.3897/zookeys.306.5455"));
    }

    @Test
    public void retrieveCitationById() throws IOException {
        final SparqlClient sparqlClientImpl = new OpenBiodivClientImpl(getResourceServiceTest());
        String citation = DatasetImporterForPensoft.findCitationById("<http://openbiodiv.net/D37E8D1A-221B-FFA6-FFE7-4458FFA0FFC2>", sparqlClientImpl);
        assertThat(citation, is("Dewi Sartiami, Laurence A. Mound. 2013. Identification of the terebrantian thrips (Insecta, Thysanoptera) associated with cultivated plants in Java, Indonesia. ZooKeys. https://doi.org/10.3897/zookeys.306.5455"));
    }

    @Test
    public void retrieveCitationById2() throws IOException {
        final SparqlClient sparqlClientImpl = new OpenBiodivClientImpl(getResourceServiceTest());
        String citation = DatasetImporterForPensoft.findCitationById("http://openbiodiv.net/D37E8D1A-221B-FFA6-FFE7-4458FFA0FFC2", sparqlClientImpl);
        assertThat(citation, is("Dewi Sartiami, Laurence A. Mound. 2013. Identification of the terebrantian thrips (Insecta, Thysanoptera) associated with cultivated plants in Java, Indonesia. ZooKeys. https://doi.org/10.3897/zookeys.306.5455"));
    }

    @Test
    public void retrieveCitationById3() throws IOException {
        final SparqlClient sparqlClientImpl = new OpenBiodivClientImpl(getResourceServiceTest());
        String citation = DatasetImporterForPensoft.findCitationById("http://openbiodiv.net/222C9E1B135454BEB7144BD7794FA01C", sparqlClientImpl);
        assertThat(citation, is("Marko Prous, Andrew Liston, Katja Kramp, Henri Savina, Hege Vårdal, Andreas Taeger. 2019. The West Palaearctic genera of Nematinae (Hymenoptera, Tenthredinidae). ZooKeys. https://doi.org/10.3897/zookeys.875.35748"));
    }

    @Test
    public void retrieveCitationById3ByDOI() throws IOException {
        final SparqlClient sparqlClientImpl = new OpenBiodivClientImpl(getResourceServiceTest());
        String citation = DatasetImporterForPensoft.findCitationByDoi("10.3897/zookeys.875.35748", sparqlClientImpl);
        assertThat(citation, is("Marko Prous, Andrew Liston, Katja Kramp, Henri Savina, Hege Vårdal, Andreas Taeger. 2019. The West Palaearctic genera of Nematinae (Hymenoptera, Tenthredinidae). ZooKeys. https://doi.org/10.3897/zookeys.875.35748"));
    }

    @Test
    public void singleTableHostInColumnHeader() throws IOException, TermLookupServiceException, StudyImporterException {

        final JsonNode tableObj = getTableObj();

        assertNotNull(tableObj);

        tableObj.get("article_doi");
        final JsonNode annotations = tableObj.get("annotations");
        for (JsonNode annotation : annotations) {
            final InteractTypeMapper interactTypeMapper =
                    new InteractTypeMapperFactoryImpl().create();
            if (annotation.has("id")) {
                final InteractType interactType = interactTypeMapper.getInteractType(annotation.get("id").asText());
            }
            if (annotation.has("context")) {
                String verbatimInteraction = annotation.get("context").asText();
            }
            annotation.get("row");
            annotation.get("column");
            annotation.get("possition");


        }

        tableObj.get("caption");


        List<Map<String, String>> rowValues = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                rowValues.add(interaction);
            }
        };

        parseRowsAndEnrich(tableObj, listener, getResourceServiceTest());

    }

    @Test
    public void parseTableContent() throws IOException, StudyImporterException {

        final JsonNode tableObj = getTableObj();

        List<Map<String, String>> rowValues = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                rowValues.add(new TreeMap<>(interaction));
            }
        };

        parseRowsAndEnrich(tableObj, listener, getResourceServiceTest());

        assertThat(rowValues.size(), is(121));
        assertThat(rowValues.get(0), hasEntry("Family Name", "Acanthaceae"));
        assertThat(rowValues.get(0), hasEntry("Family Name_expanded_taxon_id", "http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(rowValues.get(0), hasEntry("Family Name_expanded_taxon_name", "Acanthaceae"));
        assertThat(rowValues.get(0), hasEntry("Host Plant", "Ruellia sp."));
        assertThat(rowValues.get(0), hasEntry("Host Plant_expanded_taxon_id", "http://openbiodiv.net/56F59D49-725E-4BF7-8A6D-1B1A7A721231"));
        assertThat(rowValues.get(0), hasEntry("Host Plant_expanded_taxon_name", "Ruellia"));
        assertThat(rowValues.get(0), hasEntry("Thrips species", "Copidothrips octarticulatusThrips parvispinus"));
        assertThat(rowValues.get(0), hasEntry("Thrips species_expanded_taxon_id", "http://openbiodiv.net/6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC"));
        assertThat(rowValues.get(0), hasEntry("Thrips species_expanded_taxon_name", "Copidothrips octarticulatus"));

        assertThat(rowValues.get(1), hasEntry("Family Name", "Acanthaceae"));
        assertThat(rowValues.get(1), hasEntry("Family Name_expanded_taxon_id", "http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(rowValues.get(1), hasEntry("Family Name_expanded_taxon_name", "Acanthaceae"));

    }
    @Test
    public void parseTableContentZookeys_318_5693() throws IOException, StudyImporterException {

        final JsonNode tableObj = getTableObj("pensoft/annotated-table_zookeys_318_5693.json");

        List<Map<String, String>> rowValues = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                rowValues.add(new TreeMap<>(interaction));
            }
        };

        parseRowsAndEnrich(tableObj, listener, getResourceServiceTest());

        assertThat(rowValues.size(), is(3));
        assertThat(rowValues.get(0), hasEntry("Aphidura species Host plant", "Silene fruticosa"));
        assertThat(rowValues.get(0), hasEntry("Aphidura species Host plant_expanded_taxon_id", "http://openbiodiv.net/9A18EF6D-F508-4649-B290-89A0C5216050"));
        assertThat(rowValues.get(0), hasEntry("Aphidura species Host plant_expanded_taxon_name", "Silene fruticosa"));
        assertThat(rowValues.get(0), hasEntry("_expanded_taxon_id", "http://openbiodiv.net/F260B0E2-2B48-435E-A34F-26090046EA31"));
        assertThat(rowValues.get(0), hasEntry("_expanded_taxon_name", "Aphidura picta"));

        assertThat(rowValues.get(1), hasEntry("Aphidura species Host plant", "Silene italica"));
        assertThat(rowValues.get(1), hasEntry("Aphidura species Host plant_expanded_taxon_id", "http://openbiodiv.net/5BE202E0-3DA3-422B-B7DD-00EECE8F99CC"));
        assertThat(rowValues.get(1), hasEntry("Aphidura species Host plant_expanded_taxon_name", "Silene italica"));
        assertThat(rowValues.get(1), hasEntry("_expanded_taxon_id", "http://openbiodiv.net/F260B0E2-2B48-435E-A34F-26090046EA31"));
        assertThat(rowValues.get(1), hasEntry("_expanded_taxon_name", "Aphidura picta"));

    }

    @Test
    public void parseTableContentWithRowSpan() throws IOException, StudyImporterException {

        final String tableContent = IOUtils.toString(getClass().getResourceAsStream("pensoft/table-with-rowspan.html"), StandardCharsets.UTF_8);

        final ObjectNode tableObj = new ObjectMapper().createObjectNode();
        tableObj.put("table_id", "<http://openbiodiv.net/FB706B4E-BAC2-4432-AD28-48063E7753E4>");
        tableObj.put("caption", "a caption");
        tableObj.put("article_doi", "10.12/34");
        tableObj.put("article_id", "<http://openbiodiv.net/D37E8D1A-221B-FFA6-FFE7-4458FFA0FFC2>");
        tableObj.put("table_content", tableContent);

        List<Map<String, String>> rowValues = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                rowValues.add(new TreeMap<>(interaction));
            }
        };

        parseRowsAndEnrich(tableObj, listener, getResourceServiceTest());

        assertThat(rowValues.size(), is(8));
        assertThat(rowValues.get(0), hasEntry("Family Name", "Apiaceae"));
        assertThat(rowValues.get(0), hasEntry("Thrips species_expanded_taxon_name", "Thrips parvispinus"));
        assertThat(rowValues.get(1), hasEntry("Family Name", "Apiaceae"));
        assertThat(rowValues.get(1), hasEntry("Thrips species_expanded_taxon_name", "Thrips nigropilosus"));
        assertThat(rowValues.get(2), hasEntry("Family Name", "Apiaceae"));
        assertThat(rowValues.get(2), hasEntry("Thrips species_expanded_taxon_name", "Thrips parvispinus"));
        assertThat(rowValues.get(7), hasEntry("Family Name", "Apocynaceae"));
        assertThat(rowValues.get(7), hasEntry("Thrips species_expanded_taxon_name", "Thrips malloti"));

    }


}
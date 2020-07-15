package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.InteractTypeMapper;
import org.eol.globi.util.InteractTypeMapperFactoryImpl;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
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
import static org.eol.globi.data.TestUtil.getResourceServiceTest;
import static org.eol.globi.data.StudyImporterForPensoftTest.getTableObj;
import static org.eol.globi.data.StudyImporterForPensoftTest.parseRowsAndEnrich;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class StudyImporterForPensoftIT {

    @Test
    public void importStudy() throws StudyImporterException, URISyntaxException {
        final StudyImporterForPensoft importer = new StudyImporterForPensoft(new ParserFactoryLocal(), null);
        final Dataset dataset = new DatasetImpl("some/name", URI.create("some:uri"), in -> in);
        final ObjectNode objectNode = new ObjectMapper().createObjectNode();
        final URL resource = getClass().getResource("pensoft/annotated-tables-first-two.json");
        objectNode.put("url", resource.toURI().toString());
        objectNode.put("citation", "some dataset citation");
        dataset.setConfig(objectNode);
        List<Map<String, String>> links = new ArrayList<>();

        importer.setDataset(dataset);
        importer.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                links.add(link);
            }
        });
        importer.importStudy();

        assertThat(links.size(), is(146));

        assertThat(links.get(0), hasEntry("Family Name", "Acanthaceae"));
        assertThat(links.get(0), hasEntry("Family Name_taxon_name", "Acanthaceae"));
        assertThat(links.get(0), hasEntry("Family Name_taxon_path", "Plantae | Tracheophyta | Magnoliopsida | Lamiales | Acanthaceae"));
        assertThat(links.get(0), hasEntry("Family Name_taxon_pathNames", "kingdom | phylum | class | order | family"));
        assertThat(links.get(0), hasEntry("referenceUrl", "http://openbiodiv.net/FB706B4E-BAC2-4432-AD28-48063E7753E4"));
        assertThat(links.get(0), hasEntry("referenceDoi", "10.3897/zookeys.306.5455"));
        assertThat(links.get(0), hasEntry("referenceCitation", "Dewi Sartiami, Laurence A. Mound. 2013. Identification of the terebrantian thrips (Insecta, Thysanoptera) associated with cultivated plants in Java, Indonesia. ZooKeys. https://doi.org/10.3897/zookeys.306.5455"));
    }

    @Test
    public void retrieveCitation() throws IOException {
        final OpenBiodivClient openBiodivClient = new OpenBiodivClient(getResourceServiceTest());
        String citation = StudyImporterForPensoft.findCitationByDoi("10.3897/zookeys.306.5455", openBiodivClient);
        assertThat(citation, is("Dewi Sartiami, Laurence A. Mound. 2013. Identification of the terebrantian thrips (Insecta, Thysanoptera) associated with cultivated plants in Java, Indonesia. ZooKeys. https://doi.org/10.3897/zookeys.306.5455"));
    }

    @Test
    public void retrieveTaxonFamily() throws IOException {
        Taxon taxon = StudyImporterForPensoft.retrieveTaxonHierarchyById("4B689A17-2541-4F5F-A896-6F0C2EEA3FB4", new OpenBiodivClient(getResourceServiceTest()));
        assertThat(taxon.getName(), is("Acanthaceae"));
        assertThat(taxon.getRank(), is("family"));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(taxon.getPath(), is("Plantae | Tracheophyta | Magnoliopsida | Lamiales | Acanthaceae"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family"));
    }

    @Test
    public void retrieveTaxonSpecies() throws IOException {
        Taxon taxon = StudyImporterForPensoft.retrieveTaxonHierarchyById("6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC", new OpenBiodivClient(getResourceServiceTest()));
        assertThat(taxon.getName(), is("Copidothrips octarticulatus"));
        assertThat(taxon.getRank(), is(nullValue()));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC"));
        assertThat(taxon.getPath(), is("Copidothrips octarticulatus"));
        assertThat(taxon.getPathNames(), is(""));
    }

    @Test
    public void retrieveTaxonSpecies2() throws IOException {
        Taxon taxon = StudyImporterForPensoft.retrieveTaxonHierarchyById("22A7F215-829B-458A-AEBB-39FFEA6D4A91", new OpenBiodivClient(getResourceServiceTest()));
        assertThat(taxon.getName(), is("Bolacothrips striatopennatus"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/22A7F215-829B-458A-AEBB-39FFEA6D4A91"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Thysanoptera | Thripidae | Bolacothrips | Bolacothrips striatopennatus"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | genus | species"));
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
            public void newLink(Map<String, String> link) throws StudyImporterException {
                rowValues.add(link);
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
            public void newLink(Map<String, String> link) throws StudyImporterException {
                rowValues.add(new TreeMap<>(link));
            }
        };

        parseRowsAndEnrich(tableObj, listener, getResourceServiceTest());

        assertThat(rowValues.size(), is(121));
        assertThat(rowValues.get(0), hasEntry("Family Name", "Acanthaceae"));
        assertThat(rowValues.get(0), hasEntry("Family Name_taxon_id", "http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(rowValues.get(0), hasEntry("Family Name_taxon_name", "Acanthaceae"));
        assertThat(rowValues.get(0), hasEntry("Host Plant", "Ruellia sp."));
        assertThat(rowValues.get(0), hasEntry("Host Plant_taxon_id", "http://openbiodiv.net/56F59D49-725E-4BF7-8A6D-1B1A7A721231"));
        assertThat(rowValues.get(0), hasEntry("Host Plant_taxon_name", "Ruellia"));
        assertThat(rowValues.get(0), hasEntry("Thrips species", "Copidothrips octarticulatus<br/> Thrips parvispinus"));
        assertThat(rowValues.get(0), hasEntry("Thrips species_taxon_id", "http://openbiodiv.net/6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC"));
        assertThat(rowValues.get(0), hasEntry("Thrips species_taxon_name", "Copidothrips octarticulatus"));

        assertThat(rowValues.get(1), hasEntry("Family Name", "Acanthaceae"));
        assertThat(rowValues.get(1), hasEntry("Family Name_taxon_id", "http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(rowValues.get(1), hasEntry("Family Name_taxon_name", "Acanthaceae"));

    }

    @Test
    public void parseTableContentWithRowSpan() throws IOException, TermLookupServiceException, StudyImporterException {

        final String tableContent = IOUtils.toString(getClass().getResourceAsStream("pensoft/table-with-rowspan.html"), StandardCharsets.UTF_8);

        final ObjectNode tableObj = new ObjectMapper().createObjectNode();
        tableObj.put("table_id", "<http://openbiodiv.net/FB706B4E-BAC2-4432-AD28-48063E7753E4>");
        tableObj.put("caption", "a caption");
        tableObj.put("article_doi", "10.12/34");
        tableObj.put("table_content", tableContent);

        List<Map<String, String>> rowValues = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                rowValues.add(new TreeMap<>(link));
            }
        };

        parseRowsAndEnrich(tableObj, listener, getResourceServiceTest());

        assertThat(rowValues.size(), is(8));
        assertThat(rowValues.get(0), hasEntry("Family Name", "Apiaceae"));
        assertThat(rowValues.get(0), hasEntry("Thrips species_taxon_name", "Thrips parvispinus"));
        assertThat(rowValues.get(1), hasEntry("Family Name", "Apiaceae"));
        assertThat(rowValues.get(1), hasEntry("Thrips species_taxon_name", "Thrips nigropilosus"));
        assertThat(rowValues.get(2), hasEntry("Family Name", "Apiaceae"));
        assertThat(rowValues.get(2), hasEntry("Thrips species_taxon_name", "Thrips parvispinus"));
        assertThat(rowValues.get(7), hasEntry("Family Name", "Apocynaceae"));
        assertThat(rowValues.get(7), hasEntry("Thrips species_taxon_name", "Thrips malloti"));

    }


}
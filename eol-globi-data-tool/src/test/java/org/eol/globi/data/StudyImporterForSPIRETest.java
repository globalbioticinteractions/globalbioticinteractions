package org.eol.globi.data;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.JetFormat;
import com.healthmarketscience.jackcess.Table;
import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import org.apache.commons.collections.CollectionUtils;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.EnvoService;
import org.eol.globi.service.EnvoServiceException;
import org.eol.globi.service.EnvoTerm;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.internal.matchers.StringContains.containsString;

public class StudyImporterForSPIRETest extends GraphDBTestCase {

    @Test
    public void parseIllegalTitle() throws StudyImporterException {
        HashMap<String, String> properties = new HashMap<String, String>();
        StudyImporterForSPIRE.parseTitlesAndAuthors("this is really not supported, and is unformatted", properties);
        assertThat(properties.get(Study.PUBLICATION_YEAR), is(""));
        assertThat(properties.get(Study.CONTRIBUTOR), is(""));
        assertThat(properties.get(Study.TITLE), is("this is really not su...e9154c16f07ad2470849d90a8a0b9dab"));
        assertThat(properties.get(Study.DESCRIPTION), is("this is really not supported, and is unformatted"));

    }

    @Test
    public void parseAnotherYetYetOtherTitlesAndAuthorsFormat() throws StudyImporterException {
        Map<String, String> properties = new HashMap<String, String>();
        String titlesAndAuthors = "G. A. Knox, Antarctic marine ecosystems. In: Antarctic Ecology, M. W. Holdgate, Ed. (Academic Press, New York, 1970) 1:69-96, from p. 87.";
        StudyImporterForSPIRE.parseTitlesAndAuthors(titlesAndAuthors, properties);
        assertThat(properties.get(Study.DESCRIPTION), is("G. A. Knox, Antarctic marine ecosystems. In: Antarctic Ecology, M. W. Holdgate, Ed. (Academic Press, New York, 1970) 1:69-96, from p. 87."));
        assertThat(properties.get(Study.PUBLICATION_YEAR), is(""));
        assertThat(properties.get(Study.TITLE), is("Knox, Antarctic marin...984ae066666743823ac7b57da0e01f2d"));
        assertThat(properties.get(Study.CONTRIBUTOR), is(""));
    }

    @Test
    public void parseAnotherYetOtherTitlesAndAuthorsFormat() throws StudyImporterException {
        Map<String, String> properties = new HashMap<String, String>();
        StudyImporterForSPIRE.parseTitlesAndAuthors("B. A. Hawkins and R. D. Goeden, 1984.  Organization of a parasitoid community associated with a complex of galls on Atriplex spp. in southern California.  Ecol. Entomol. 9:271-292, from p. 274.", properties);
        assertThat(properties.get(Study.PUBLICATION_YEAR), is(""));
        assertThat(properties.get(Study.TITLE), is("Hawkins and Goeden, 1...fcebc21f82937fa4ab9f77a0ecbd62e3"));
        assertThat(properties.get(Study.DESCRIPTION), is("B. A. Hawkins and R. D. Goeden, 1984.  Organization of a parasitoid community associated with a complex of galls on Atriplex spp. in southern California.  Ecol. Entomol. 9:271-292, from p. 274."));
        assertThat(properties.get(Study.CONTRIBUTOR), is(""));
    }

    @Test
    public void parseYetOtherTitlesAndAuthorsFormat() throws StudyImporterException {
        Map<String, String> properties = new HashMap<String, String>();
        StudyImporterForSPIRE.parseTitlesAndAuthors("Townsend, CR, Thompson, RM, McIntosh, AR, Kilroy, C, Edwards, ED, Scarsbrook, MR. 1998.  Disturbance, resource supply and food-web architecture in streams.  Ecology Letters 1:200-209.", properties);
        assertThat(properties.get(Study.TITLE), is("Townsend, CR, Thompso...db61dcc043a135ac2fa8b440e11165e3"));
        assertThat(properties.get(Study.PUBLICATION_YEAR), is(""));
        assertThat(properties.get(Study.DESCRIPTION), is("Townsend, CR, Thompson, RM, McIntosh, AR, Kilroy, C, Edwards, ED, Scarsbrook, MR. 1998.  Disturbance, resource supply and food-web architecture in streams.  Ecology Letters 1:200-209."));
        assertThat(properties.get(Study.CONTRIBUTOR), is(""));
    }

    @Test
    public void readMDB() throws URISyntaxException, IOException {
        URI uri = getClass().getResource("spire/econetvis.mdb").toURI();
        assertThat(uri, is(notNullValue()));
        Database db = Database.open(new File(uri), true);
        assertThat(db.getFileFormat().getJetFormat(), is(JetFormat.VERSION_4));

        String[] tableNames = new String[]{
                "attribute_types",
                "common_names",
                "entities",
                "habitats",
                "links",
                "localities",
                "metastudies",
                "part_mapping_new",
                "part_qualifiers",
                "studies",
                "study_habitat",
                "study_local",
                "taxon",
                "taxon_attributes"

        };
        Set<String> expectedSet = new HashSet<String>();
        Collections.addAll(expectedSet, tableNames);

        Set<String> actualTableNames = db.getTableNames();
        assertThat(actualTableNames.size(), is(not(0)));
        assertThat("expected tables names [" + Arrays.toString(tableNames) + "] to be present",
                CollectionUtils.subtract(expectedSet, actualTableNames).size(), is(0));

        Table studies = db.getTable("studies");
        for (Map<String, Object> study : studies) {
            assertNotNull(study.get("reference"));
        }

        List<String> expectedColumnNames = Arrays.asList("study_id", "entity1", "entity2", "link_strength", "link_type", "table_ref", "link_number");
        assertColumnNames(expectedColumnNames, db.getTable("links"));

        expectedColumnNames = Arrays.asList("id", "latinname", "commonname", "parent", "webinfo", "moreinfo", "numchildren", "pos", "classification", "pictures", "sounds", "specimens", "idx", "extinct", "rank");
        assertColumnNames(expectedColumnNames, db.getTable("taxon"));

        Table taxonTable = db.getTable("taxon");
        int numberOfTaxa = 0;
        while (taxonTable.getNextRow() != null) {
            numberOfTaxa++;
        }

        assertThat(numberOfTaxa, is(198301));

        Table links = db.getTable("links");
        int numberOfLinks = 0;
        while (links.getNextRow() != null) {
            numberOfLinks++;
        }

        assertThat(numberOfLinks, is(18189));
    }

    private void assertColumnNames(List<String> expectedColumnNames, Table table) throws IOException {
        Table links = table;
        List<String> actualColumnNames = new ArrayList<String>();
        List<Column> columns = links.getColumns();
        for (Column column : columns) {
            actualColumnNames.add(column.getName());
        }

        assertThat(actualColumnNames, is(expectedColumnNames));
    }

    @Test
    public void importSingleLink() throws NodeFactoryException {
        assertSingleImport("some spire habitat", "envo externalid", "envo name");

    }

    @Test
    public void importSingleLinkNoHabitatMatch() throws NodeFactoryException {
        String unmappedHabitat = "some unmapped spire habitat";
        assertSingleImport("some unmapped spire habitat", "SPIRE:" + unmappedHabitat, unmappedHabitat);
    }

    private void assertSingleImport(String spireHabitat, String envoId, String envoLabel) throws NodeFactoryException {
        StudyImporterForSPIRE studyImporterForSPIRE = createImporter();
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(Study.TITLE, "the study of men eating dogs");
        properties.put(StudyImporterForSPIRE.PREY_NAME, "dog");
        properties.put(StudyImporterForSPIRE.PREDATOR_NAME, "man");
        properties.put(StudyImporterForSPIRE.COUNTRY, "USA");
        properties.put(StudyImporterForSPIRE.OF_HABITAT, spireHabitat);
        studyImporterForSPIRE.importTrophicLink(properties);

        Taxon dog = nodeFactory.findTaxon("dog");
        assertThat(dog, is(notNullValue()));
        Taxon man = nodeFactory.findTaxon("man");
        assertThat(man, is(notNullValue()));
        Iterable<Relationship> specimenRels = man.getUnderlyingNode().getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);

        int count = 0;
        for (Relationship specimenRel : specimenRels) {
            count++;
            Specimen specimen = new Specimen(specimenRel.getStartNode());
            assertThat(specimen.getSampleLocation().getLatitude(), is(39.76));
            assertThat(specimen.getSampleLocation().getLongitude(), is(-98.5));

            List<Environment> environments = specimen.getSampleLocation().getEnvironments();
            assertThat(environments.size(), is(1));
            Environment environment = environments.get(0);
            assertThat(environment.getExternalId(), is(envoId));
            assertThat(environment.getName(), is(envoLabel));
        }
        assertThat(count, is(1));
    }

    private StudyImporterForSPIRE createImporter() {
        StudyImporterForSPIRE studyImporterForSPIRE = new StudyImporterForSPIRE(null, nodeFactory);
        studyImporterForSPIRE.setEnvoService(new EnvoService() {

            @Override
            public List<EnvoTerm> lookupBySPIREHabitat(String name) throws EnvoServiceException {
                ArrayList<EnvoTerm> envoTerms = new ArrayList<EnvoTerm>();
                if ("some spire habitat".equals(name)) {
                    envoTerms.add(new EnvoTerm("envo externalid", "envo name"));
                }
                return envoTerms;
            }
        });
        return studyImporterForSPIRE;
    }

    @Test
    public void importStudy() throws IOException, StudyImporterException {
        RDFDefaultErrorHandler.silent = true;
        StudyImporterForSPIRE importer = createImporter();
        TestTrophicLinkListener listener = new TestTrophicLinkListener();
        importer.setTrophicLinkListener(listener);
        importer.importStudy();

        assertThat(listener.countries.size(), is(50));

        assertThat(listener.getCount(), is(30196));

        for (String description : listener.descriptions) {
            assertThat(description, not(containsString("http://spire.umbc.edu/ontologies/SpireEcoConcepts.owl#")));
        }

        for (String title : listener.titles) {
            assertThat(title, not(containsString("http://spire.umbc.edu/")));
        }

        for (String envo : listener.environments) {
            assertThat(envo, not(containsString("http://spire.umbc.edu/ontologies/SpireEcoConcepts.owl#")));
        }

        assertThat(listener.environments, hasItem("Galls"));
    }


    private static class TestTrophicLinkListener implements TrophicLinkListener {
        public int getCount() {
            return count;
        }

        private int count = 0;
        Set<String> countries = new HashSet<String>();
        Set<String> descriptions = new HashSet<String>();
        Set<String> titles = new HashSet<String>();
        List<String> environments = new ArrayList<String>();

        @Override
        public void newLink(Map<String, String> properties) {
            if (properties.containsKey(StudyImporterForSPIRE.COUNTRY)) {
                countries.add(properties.get(StudyImporterForSPIRE.COUNTRY));
            }
            if (properties.containsKey(Study.DESCRIPTION)) {
                descriptions.add(properties.get(Study.DESCRIPTION));
            }
            if (properties.containsKey(Study.TITLE)) {
                titles.add(properties.get(Study.TITLE));
            }

            if (properties.containsKey(Study.TITLE)) {
                titles.add(properties.get(Study.TITLE));
            }
            if (properties.containsKey(StudyImporterForSPIRE.OF_HABITAT)) {
                environments.add(properties.get(StudyImporterForSPIRE.OF_HABITAT));
            }
            count++;
        }
    }


}

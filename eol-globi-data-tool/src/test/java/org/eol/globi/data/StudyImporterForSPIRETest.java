package org.eol.globi.data;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.JetFormat;
import com.healthmarketscience.jackcess.Table;
import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import org.apache.commons.collections.CollectionUtils;
import org.eol.globi.domain.Study;
import org.junit.Test;

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
    public void importStudy() throws IOException, StudyImporterException {
        RDFDefaultErrorHandler.silent = true;
        StudyImporterForSPIRE importer = new StudyImporterForSPIRE(null, nodeFactory);
        TestTrophicLinkListener listener = new TestTrophicLinkListener();
        importer.setTrophicLinkListener(listener);
        importer.importStudy();

        assertThat(listener.getCount() > 30000, is(true));
    }


    private static class TestTrophicLinkListener implements TrophicLinkListener {
        public int getCount() {
            return count;
        }

        private int count = 0;
        Set<String> countries = new HashSet<String>();

        @Override
        public void newLink(Map<String, String> properties) {
            if (properties.containsKey(StudyImporterForSPIRE.COUNTRY)) {
                countries.add(properties.get(StudyImporterForSPIRE.COUNTRY));
            }
            count++;
        }
    }


}

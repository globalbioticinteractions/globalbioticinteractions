package org.eol.globi.data;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.JetFormat;
import com.healthmarketscience.jackcess.Table;
import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.eol.globi.domain.Study;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class StudyImporterForSPIRETest extends GraphDBTestCase {

    @Test
    public void readMDB() throws URISyntaxException, IOException {
        URI uri = getClass().getResource("spire/econetvis.mdb").toURI();
        assertThat(uri, is(notNullValue()));
        Database db = Database.open(new File(uri), true);
        assertThat(db.getFileFormat().getJetFormat(), is(JetFormat.VERSION_4));

        String[] tableNames = new String[] {
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
        for (String tableName : tableNames) {
            expectedSet.add(tableName);
        }

        Set<String> actualTableNames = db.getTableNames();
        assertThat(actualTableNames.size(), is(not(0)));
        assertThat("expected tables names [" + tableNames + "] to be present",
                CollectionUtils.subtract(expectedSet, actualTableNames).size(), is(0));

        Table studies = db.getTable("studies");
        for (Map<String, Object> study : studies) {
            assertNotNull(study.get("reference"));
        }
    }

    @Test
    public void importStudy() throws IOException, StudyImporterException {
        RDFDefaultErrorHandler.silent = true;
        StudyImporterForSPIRE importer = new StudyImporterForSPIRE(null, nodeFactory);
        TestTrophicLinkListener listener = new TestTrophicLinkListener();
        importer.setTrophicLinkListener(listener);
        importer.importStudy();

        assertThat(listener.getCount(), is(30196));
        assertThat("number of unique countries changed since this test was written", listener.countries.size(), is(50));
    }


    private static class TestTrophicLinkListener implements TrophicLinkListener {
        public int getCount() {
            return count;
        }

        private int count = 0;
        Set<String> countries = new HashSet<String>();

        @Override
        public void newLink(Study study, String predatorName, String preyName, String country, String state, String locality) {
            if (country != null) {
                countries.add(country);
            }
            count++;
        }
    }


}

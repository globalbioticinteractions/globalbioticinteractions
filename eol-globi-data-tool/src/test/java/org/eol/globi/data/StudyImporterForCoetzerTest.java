package org.eol.globi.data;

import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class StudyImporterForCoetzerTest extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException, IOException {
        StudyImporterForCoetzer importer = new StudyImporterForCoetzer(null, nodeFactory);
        importer.setSourceCitation("source citation");
        importer.setArchiveURL("coetzer/CatalogOfAfrotropicalBees.zip");
        importStudy(importer);

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (Study allStudy : allStudies) {
            assertThat(allStudy.getSource(), startsWith("source citation"));
            assertThat(allStudy.getSource(), containsString("Accessed at"));
        }

        assertThat(taxonIndex.findTaxonByName("Agrostis tremula"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Coelioxys erythrura"), is(notNullValue()));

    }

}

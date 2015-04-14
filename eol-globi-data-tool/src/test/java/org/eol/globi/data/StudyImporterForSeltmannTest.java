package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class StudyImporterForSeltmannTest extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException, NodeFactoryException, IOException {
        StudyImporterForSeltmann importer = new StudyImporterForSeltmann(null, nodeFactory);
        importer.setArchiveURL("seltmann/testArchive.zip");
        importer.importStudy();

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (Study allStudy : allStudies) {
            assertThat(allStudy.getSource(), startsWith("Digital Bee Collections Network, 2014 (and updates). Version: 2015-03-18. National Science Foundation grant DBI 0956388"));
            assertThat(allStudy.getCitation(), is("citation:doi:Digital Bee Collections Network, 2014 (and updates). Version: 2015-03-18. National Science Foundation grant DBI 0956388"));
        }

        assertThat(nodeFactory.findTaxonByName("Megandrena mentzeliae"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Mentzelia tricuspis"), is(notNullValue()));

    }
}

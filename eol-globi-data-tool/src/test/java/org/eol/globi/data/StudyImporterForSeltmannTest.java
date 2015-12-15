package org.eol.globi.data;

import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class StudyImporterForSeltmannTest extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException, NodeFactoryException, IOException {
        StudyImporterForSeltmann importer = new StudyImporterForSeltmann(null, nodeFactory);
        importer.setArchiveURL("seltmann/testArchive.zip");
        importStudy(importer);

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (Study allStudy : allStudies) {
            assertThat(allStudy.getSource(), startsWith("Digital Bee Collections Network, 2014 (and updates). Version: 2015-03-18. National Science Foundation grant DBI 0956388"));
            assertThat(allStudy.getCitation(), is("citation:doi:Digital Bee Collections Network, 2014 (and updates). Version: 2015-03-18. National Science Foundation grant DBI 0956388"));
            Iterable<Relationship> specimens = allStudy.getSpecimens();
            for (Relationship specimen : specimens) {
                Specimen spec = new Specimen(specimen.getEndNode());
                Term basisOfRecord = spec.getBasisOfRecord();
                assertThat(basisOfRecord.getId(), either(is("TEST:PreservedSpecimen")).or(is("TEST:LabelObservation")));
                assertThat(basisOfRecord.getName(), either(is("PreservedSpecimen")).or(is("LabelObservation")));
            }
        }

        assertThat(taxonIndex.findTaxonByName("Megandrena mentzeliae"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Mentzelia tricuspis"), is(notNullValue()));

    }
}

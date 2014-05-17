package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForSIADTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {
        StudyImporterForSIAD importerForSAID = new StudyImporterForSIAD(new ParserFactoryImpl(), nodeFactory);
        importerForSAID.importStudy();
        List<Study> allStudies = NodeFactory.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));

        Study foundStudy = allStudies.get(0);
        Iterable<Relationship> relationships = foundStudy.getSpecimens();
        int count = 0;
        for (Relationship relationship : relationships) {
            count++;
        }

        assertThat(count > 14000, is(true));

    }

}

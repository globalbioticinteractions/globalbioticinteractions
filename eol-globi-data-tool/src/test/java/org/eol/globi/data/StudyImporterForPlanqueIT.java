package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.Ostermiller.util.StringHelper.containsAny;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForPlanqueIT extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        final List<String> errorMessages = new ArrayList<String>();


        StudyImporter importer = new StudyImporterForPlanque(new ParserFactoryImpl(), nodeFactory);
        importer.setLogger(new ImportLogger() {
            @Override
            public void warn(Study study, String message) {
                errorMessages.add(message);
            }

            @Override
            public void info(Study study, String message) {

            }

            @Override
            public void severe(Study study, String message) {

            }
        });
        importStudy(importer);

        int interactionCount = 0;
        List<Study> studies = NodeUtil.findAllStudies(getGraphDb());
        for (Study study : studies) {
            Iterable<Relationship> specimenRels = study.getSpecimens();
            for (Relationship specimenRel : specimenRels) {
                interactionCount++;
            }
        }
        assertThat(interactionCount, is(4900));

        int uniqueReference = 236;

        // note that the +1 is for all links that had no reference associated to it
        assertThat(studies.size(), is(uniqueReference + 1));
        assertThat(taxonIndex.findTaxonByName("Trisopterus esmarkii"), is(notNullValue()));
        assertThat(errorMessages.size(), is(0));
    }

}

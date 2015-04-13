package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

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
        importer.importStudy();

        List<Study> studies = NodeUtil.findAllStudies(getGraphDb());

        assertThat(studies.size(), is(212));
        assertThat(nodeFactory.findTaxonByName("Sagitta elegans"), is(notNullValue()));

        assertThat(errorMessages, hasItems("no full ref for [Ponomarenko 2009] on line [287], using short instead",
                "no full ref for [Ponomarenko 2009] on line [288], using short instead",
                "no full ref for [Ponomarenko 2009] on line [289], using short instead"));


        assertThat(errorMessages.size(), is(3));
    }

}

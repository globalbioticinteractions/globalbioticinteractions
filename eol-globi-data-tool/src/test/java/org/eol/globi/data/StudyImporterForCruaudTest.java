package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForCruaudTest extends GraphDBTestCase {

    @Test
        public void importAll() throws NodeFactoryException, StudyImporterException {
            StudyImporter importer = new StudyImporterForCruaud(new ParserFactoryImpl(), nodeFactory);
            importer.importStudy();

            List<Study> allStudies = NodeFactoryImpl.findAllStudies(getGraphDb());
            assertThat(allStudies.size(), is(1));


            assertThat(nodeFactory.findTaxonByName("Agaon sp."), is(notNullValue()));
            assertThat(nodeFactory.findTaxonByName("Ficus chapaensis"), is(notNullValue()));

            assertThat(allStudies.get(0).getSource(), is(notNullValue()));
        }
}

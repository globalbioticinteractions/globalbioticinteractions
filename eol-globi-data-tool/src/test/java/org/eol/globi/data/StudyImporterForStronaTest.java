package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForStronaTest extends GraphDBTestCase {


    @Test
    public void importFirst200() throws NodeFactoryException, StudyImporterException {
        StudyImporter importer = new StudyImporterForStrona(new ParserFactoryImpl(), nodeFactory);
        importer.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber < 200;
            }
        });
        importer.importStudy();

        List<Study> allStudies = NodeFactoryImpl.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));


        assertThat(nodeFactory.findTaxonByName("Aidablennius sphynx"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Acanthocephaloides incrassatus"), is(notNullValue()));

        assertThat(allStudies.get(0).getSource(), containsString(" Accessed at"));
    }


}

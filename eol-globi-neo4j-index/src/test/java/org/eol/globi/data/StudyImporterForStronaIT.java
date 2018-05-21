package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForStronaIT extends GraphDBTestCase {


    @Test
    public void importFirst200() throws NodeFactoryException, StudyImporterException {
        StudyImporter importer = new StudyImporterForStrona(new ParserFactoryLocal(), nodeFactory);
        importer.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber < 200;
            }
        });
        importStudy(importer);

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));


        assertThat(taxonIndex.findTaxonByName("Aidablennius sphynx"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Acanthocephaloides incrassatus"), is(notNullValue()));

        Study study = allStudies.get(0);
        assertThat(study.getSource(), containsString(" Accessed at"));
        assertThat(study.getCitation(), containsString("https://doi.org/10.1890/12-1419.1"));
        assertThat(study.getCitation(), containsString("Strona"));
    }


}

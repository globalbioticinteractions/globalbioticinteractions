package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForFishbase2IT extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        new StudyImporterForFishbase2(null, nodeFactory).importStudy();
        Study study = nodeFactory.findStudy("FB:REF:6160");
        assertThat(study, is(notNullValue()));
        assertThat(study.getCitation(), is("citation bla"));
        assertThat(study.getSource(), is("source bla"));
        assertThat(taxonIndex.findTaxonByName("Eledone cirrhosa"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Gadus morhua"), is(notNullValue()));
    }

}

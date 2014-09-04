package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForSIADTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporterForSIAD importerForSAID = new StudyImporterForSIAD(new ParserFactoryImpl(), nodeFactory);
        importerForSAID.importStudy();
        List<Study> allStudies = NodeFactory.findAllStudies(getGraphDb());
        assertThat(allStudies.size() > 1, is(true));

        TaxonNode taxon = nodeFactory.findTaxonByName("Anisakis");
        assertThat(taxon, is(notNullValue())) ;

        taxon = nodeFactory.findTaxonByName("Abbreviata");
        assertThat(taxon, is(notNullValue())) ;
    }

}

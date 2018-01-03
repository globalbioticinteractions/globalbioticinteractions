package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForSIADIT extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {
        StudyImporterForSIAD importer = new StudyImporterForSIAD(new ParserFactoryLocal(), nodeFactory);
        importStudy(importer);
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size() > 1, is(true));

        Taxon taxon = taxonIndex.findTaxonByName("Anisakis");
        assertThat(taxon, is(notNullValue())) ;

        taxon = taxonIndex.findTaxonByName("Abbreviata");
        assertThat(taxon, is(notNullValue())) ;
    }

}

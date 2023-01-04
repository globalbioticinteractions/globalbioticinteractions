package org.eol.globi.data;

import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForSIADIT extends GraphDBNeo4j2TestCase {

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporterForSIAD importer = new DatasetImporterForSIAD(
                new ParserFactoryLocal(getClass()), nodeFactory
        );
        importStudy(importer);
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size() > 1, is(true));

        Taxon taxon = taxonIndex.findTaxonByName("Anisakis");
        assertThat(taxon, is(notNullValue())) ;

        taxon = taxonIndex.findTaxonByName("Abbreviata");
        assertThat(taxon, is(notNullValue())) ;
    }

}

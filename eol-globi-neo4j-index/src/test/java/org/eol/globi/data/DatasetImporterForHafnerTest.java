package org.eol.globi.data;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForHafnerTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {

        DatasetImporter importer = new DatasetImporterForHafner(new ParserFactoryLocal(), nodeFactory);
        importStudy(importer);


        assertThat(taxonIndex.findTaxonByName("Orthogeomys_cherriei"), is(notNullValue()));
    }
}

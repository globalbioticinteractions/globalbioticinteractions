package org.eol.globi.data;

import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForHafnerTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {

        DatasetImporter importer = new DatasetImporterForHafner(new ParserFactoryLocal(getClass()), nodeFactory);
        importStudy(importer);


        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);
            assertThat(taxonIndex.findTaxonByName("Orthogeomys_cherriei"), is(notNullValue()));
        }
    }
}

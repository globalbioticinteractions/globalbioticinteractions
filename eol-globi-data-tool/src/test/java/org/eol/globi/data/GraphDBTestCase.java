package org.eol.globi.data;

import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public abstract class GraphDBTestCase {

    private GraphDatabaseService graphDb;

    protected NodeFactory nodeFactory;

    @Before
    public void startGraphDb() throws IOException {
        graphDb = new org.neo4j.test.ImpermanentGraphDatabase();
        nodeFactory = new NodeFactory(graphDb, new TaxonPropertyEnricher() {

            @Override
            public boolean enrich(Taxon taxon) throws IOException {
                return false;
            }
        });
    }

    @After
    public void shutdownGraphDb() {
        graphDb.shutdown();
    }

    protected GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    protected long getUTCTestTime() {
        Calendar calendar = DatatypeConverter.parseDateTime("1992-03-30T08:00:00Z");
        return calendar.getTime().getTime();
    }
}

package org.eol.globi.service;

import org.neo4j.graphdb.GraphDatabaseService;

import java.util.ArrayList;
import java.util.List;

public class TaxonPropertyEnricherFactory {
    public static TaxonPropertyEnricher createTaxonEnricher(GraphDatabaseService graphService) {
        TaxonPropertyEnricherImpl taxonEnricher = new TaxonPropertyEnricherImpl(graphService);
        List<TaxonPropertyLookupService> services = new ArrayList<TaxonPropertyLookupService>() {
            {
                //add(new EOLOfflineService());
                add(new EOLService());
                add(new WoRMSService());
                add(new ITISService());
                add(new GulfBaseService());
                add(new NoMatchService());

            }
        };
        taxonEnricher.setServices(services);
        return taxonEnricher;
    }
}

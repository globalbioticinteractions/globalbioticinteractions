package org.eol.globi.service;

import java.util.ArrayList;
import java.util.List;

public class TaxonPropertyEnricherFactory {
    public static TaxonPropertyEnricher createTaxonEnricher() {
        TaxonPropertyEnricherImpl taxonEnricher = new TaxonPropertyEnricherImpl();
        List<TaxonPropertyLookupService> services = new ArrayList<TaxonPropertyLookupService>() {
            {
                //add(new GloBICachedTaxaService());
                //add(new EOLOfflineService());
                add(new EnvoService());
                add(new WikipediaService());
                add(new EOLService());
                add(new WoRMSService());
                //add(new ITISService());
                add(new GulfBaseService());
            }
        };
        taxonEnricher.setServices(services);
        return taxonEnricher;
    }
}

package org.eol.globi.service;

import java.util.ArrayList;
import java.util.List;

public class TaxonPropertyEnricherFactory {
    public static TaxonPropertyEnricher createTaxonEnricher() {
        TaxonPropertyEnricherImpl taxonEnricher = new TaxonPropertyEnricherImpl();
        List<TaxonPropertyLookupService> services = new ArrayList<TaxonPropertyLookupService>() {
            {
                //add(new EOLOfflineService());
                add(new EnvoService());
                add(new FunctionalGroupService());
                add(new EOLService());
                add(new WoRMSService());
                //add(new ITISService());
                add(new GulfBaseService());
                add(new AtlasOfLivingAustraliaService());
            }
        };
        taxonEnricher.setServices(services);
        return taxonEnricher;
    }
}

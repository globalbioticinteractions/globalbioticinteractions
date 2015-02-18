package org.eol.globi.service;

import java.util.ArrayList;
import java.util.List;

public class PropertyEnricherFactory {
    public static PropertyEnricher createTaxonEnricher() {
        TaxonEnricherImpl taxonEnricher = new TaxonEnricherImpl();
        List<PropertyEnricher> services = new ArrayList<PropertyEnricher>() {
            {
                add(new EnvoService());
                add(new FunctionalGroupService());
                add(new NBNService());
                add(new EOLService());
                add(new ITISService());
                add(new WoRMSService());
                add(new GulfBaseService());
                add(new AtlasOfLivingAustraliaService());
            }
        };
        taxonEnricher.setServices(services);
        return taxonEnricher;
    }
}

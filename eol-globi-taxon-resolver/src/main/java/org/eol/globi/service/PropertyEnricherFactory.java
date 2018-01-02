package org.eol.globi.service;

import org.eol.globi.taxon.*;

import java.util.ArrayList;
import java.util.List;

public class PropertyEnricherFactory {
    public static PropertyEnricher createTaxonEnricher() {
        return getTaxonEnricher();
    }

    private static TaxonEnricherImpl getTaxonEnricher() {
        return new TaxonEnricherImpl() {{
            setServices(new ArrayList<PropertyEnricher>() {
                {
                    add(new EnvoService());
                    add(new FunctionalGroupService());
                    add(new NBNService());
                    add(new NODCTaxonService());
                    add(new ITISService());
                    add(new NCBIService());
                    add(new GBIFService());
                    add(new INaturalistTaxonService());
                    add(new EOLService());
                    add(new WoRMSService());
                    add(new GulfBaseService());
                    add(new AtlasOfLivingAustraliaService());
                }
            });
        }};
    }

    public static TermMatcher createTaxonMatcher() {
        return getTaxonEnricher();
    }
}

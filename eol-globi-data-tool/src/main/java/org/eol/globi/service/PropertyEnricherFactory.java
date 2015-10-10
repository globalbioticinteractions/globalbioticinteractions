package org.eol.globi.service;

import org.eol.globi.taxon.AtlasOfLivingAustraliaService;
import org.eol.globi.taxon.EOLService;
import org.eol.globi.taxon.EnvoService;
import org.eol.globi.taxon.FunctionalGroupService;
import org.eol.globi.taxon.GulfBaseService;
import org.eol.globi.taxon.ITISService;
import org.eol.globi.taxon.NBNService;
import org.eol.globi.taxon.NCBIService;
import org.eol.globi.taxon.TaxonEnricherImpl;
import org.eol.globi.taxon.WoRMSService;

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
                add(new ITISService());
                add(new NCBIService());
                add(new EOLService());
                add(new WoRMSService());
                add(new GulfBaseService());
                add(new AtlasOfLivingAustraliaService());
            }
        };
        taxonEnricher.setServices(services);
        return taxonEnricher;
    }
}

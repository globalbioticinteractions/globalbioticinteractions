package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.EOLTaxonImageService;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.service.TaxonUtil;

import java.io.IOException;
import java.util.Map;

public class TaxonIdLookup {

    public static void main(String args[]) {
        String externalId = args[0];
        try {
            System.err.println("externalId [" + externalId + "] resolving...");
            Map<String, String> enrich = PropertyEnricherFactory.createTaxonEnricher().enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));
            TaxonImage taxonImage = new EOLTaxonImageService().lookupImageForExternalId(externalId);
            Taxon taxon = TaxonUtil.mapToTaxon(enrich);
            if (taxonImage != null) {
                taxon.setThumbnailUrl(taxonImage.getThumbnailURL());
            }

            String[] row = new String[]{
                    taxon.getExternalId(),
                    taxon.getName(),
                    taxon.getRank(),
                    taxon.getCommonNames(),
                    taxon.getPath(),
                    taxon.getPathIds(),
                    taxon.getPathNames(),
                    taxon.getExternalUrl(),
                    taxon.getThumbnailUrl()
            };
            System.out.println(StringUtils.join(row, "\t"));
            System.err.println("externalId [" + externalId + "] resolved.");
        } catch (PropertyEnricherException | IOException e) {
            System.err.println("failed to resolve taxon id [" + externalId + "]: [" + e.getMessage());
        }
    }
}

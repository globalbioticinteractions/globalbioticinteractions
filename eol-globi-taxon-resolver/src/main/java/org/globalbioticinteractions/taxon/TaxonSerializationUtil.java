package org.globalbioticinteractions.taxon;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;

public class TaxonSerializationUtil {
    public static Taxon arrayToTaxon(String[] enrichedSingle) {
        Taxon enrichedSingleTaxon = new TaxonImpl();
        enrichedSingleTaxon.setExternalId(enrichedSingle[0]);
        enrichedSingleTaxon.setName(enrichedSingle[1]);
        enrichedSingleTaxon.setAuthorship(enrichedSingle[2]);
        enrichedSingleTaxon.setRank(enrichedSingle[3]);
        enrichedSingleTaxon.setPath(enrichedSingle[4]);
        enrichedSingleTaxon.setPathIds(enrichedSingle[5]);
        enrichedSingleTaxon.setPathNames(enrichedSingle[6]);
        enrichedSingleTaxon.setCommonNames(enrichedSingle[7]);
        enrichedSingleTaxon.setStatus(new TermImpl(enrichedSingle[8], enrichedSingle[9]));
        enrichedSingleTaxon.setNameSource(enrichedSingle[10]);
        enrichedSingleTaxon.setNameSourceURL(enrichedSingle[11]);
        enrichedSingleTaxon.setNameSourceAccessedAt(enrichedSingle[12]);
        enrichedSingleTaxon.setExternalUrl(enrichedSingle[13]);
        enrichedSingleTaxon.setThumbnailUrl(enrichedSingle[14]);
        return enrichedSingleTaxon;
    }

    public static String[] taxonToArray(Taxon taxon) {
        return new String[]{
                taxon.getExternalId(),
                taxon.getName(),
                taxon.getAuthorship(),
                taxon.getRank(),
                taxon.getPath(),
                taxon.getPathIds(),
                taxon.getPathNames(),
                taxon.getCommonNames(),
                taxon.getStatus() == null ? null : taxon.getStatus().getId(),
                taxon.getStatus() == null ? null : taxon.getStatus().getName(),
                taxon.getNameSource(),
                taxon.getNameSourceURL(),
                taxon.getNameSourceAccessedAt(),
                taxon.getExternalUrl(),
                taxon.getThumbnailUrl()
        };
    }
}

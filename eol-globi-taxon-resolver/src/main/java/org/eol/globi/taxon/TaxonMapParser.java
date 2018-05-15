package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.CSVTSVUtil;

public class TaxonMapParser {

    public static Taxon parseResolvedTaxon(String line[]) {
        Taxon resolvedTaxon = new TaxonImpl();
        resolvedTaxon.setExternalId(CSVTSVUtil.valueOrNull(line, 2));;
        resolvedTaxon.setName(CSVTSVUtil.valueOrNull(line, 3));
        return resolvedTaxon;
    }

    public static Taxon parseProvidedTaxon(String line[]) {
        Taxon providedTaxon = new TaxonImpl();
        providedTaxon.setExternalId(CSVTSVUtil.valueOrNull(line, 0));
        providedTaxon.setName(CSVTSVUtil.valueOrNull(line, 1));
        return providedTaxon;
    }

}

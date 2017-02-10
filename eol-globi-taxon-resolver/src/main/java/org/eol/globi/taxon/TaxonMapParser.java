package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.CSVTSVUtil;

public class TaxonMapParser {

    public static Taxon parseResolvedTaxon(LabeledCSVParser labeledCSVParser) {
        Taxon resolvedTaxon = new TaxonImpl();
        resolvedTaxon.setExternalId(CSVTSVUtil.valueOrNull(labeledCSVParser, "resolvedTaxonId"));
        resolvedTaxon.setName(CSVTSVUtil.valueOrNull(labeledCSVParser, "resolvedTaxonName"));
        return resolvedTaxon;
    }

    public static Taxon parseProvidedTaxon(LabeledCSVParser labeledCSVParser) {
        Taxon providedTaxon = new TaxonImpl();
        providedTaxon.setExternalId(CSVTSVUtil.valueOrNull(labeledCSVParser, "providedTaxonId"));
        providedTaxon.setName(CSVTSVUtil.valueOrNull(labeledCSVParser, "providedTaxonName"));
        return providedTaxon;
    }

}

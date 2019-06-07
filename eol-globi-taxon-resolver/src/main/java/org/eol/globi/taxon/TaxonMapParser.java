package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;

import java.util.function.Function;

public class TaxonMapParser {

    public static Taxon parseResolvedTaxon(String line[]) {
        Taxon resolvedTaxon = new TaxonImpl();
        resolvedTaxon.setExternalId(CSVTSVUtil.valueOrNull(line, 2));
        ;
        resolvedTaxon.setName(CSVTSVUtil.valueOrNull(line, 3));
        return resolvedTaxon;
    }

    public static Taxon parseProvidedTaxon(String line[]) {
        Taxon providedTaxon = new TaxonImpl();
        providedTaxon.setExternalId(CSVTSVUtil.valueOrNull(line, 0));
        providedTaxon.setName(CSVTSVUtil.valueOrNull(line, 1));
        return providedTaxon;
    }

    public static Triple<Taxon, NameType, Taxon> parse(String line) {
        String[] strings = CSVTSVUtil.splitTSV(line);
        Taxon provided = parseProvidedTaxon(strings);
        Taxon resolved = parseResolvedTaxon(strings);
        // erase unlikely id
        if (ExternalIdUtil.isUnlikelyId(provided.getExternalId())) {
            provided = new TaxonImpl(provided.getName(), "");
        }
        return new ImmutableTriple<>(provided, NameType.SAME_AS, resolved);
    }
}

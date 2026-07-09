package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
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
        if (line.length == 4) {
            resolvedTaxon.setExternalId(CSVTSVUtil.valueOrNull(line, 2));
            resolvedTaxon.setName(CSVTSVUtil.valueOrNull(line, 3));
        } else if (line.length == 6) {
            resolvedTaxon.setExternalId(CSVTSVUtil.valueOrNull(line, 3));
            resolvedTaxon.setName(CSVTSVUtil.valueOrNull(line, 4));
            resolvedTaxon.setPath(CSVTSVUtil.valueOrNull(line, 5));

        }
        return resolvedTaxon;
    }

    public static Taxon parseProvidedTaxon(String line[]) {
        Taxon providedTaxon = new TaxonImpl();
        if (line.length == 4) {
            providedTaxon.setExternalId(CSVTSVUtil.valueOrNull(line, 0));
            providedTaxon.setName(CSVTSVUtil.valueOrNull(line, 1));
        } else if (line.length == 6) {
            providedTaxon.setExternalId(CSVTSVUtil.valueOrNull(line, 0));
            providedTaxon.setName(CSVTSVUtil.valueOrNull(line, 1));
            providedTaxon.setPath(CSVTSVUtil.valueOrNull(line, 2));
        }
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

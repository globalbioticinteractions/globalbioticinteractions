package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.CSVUtil;

import java.io.BufferedReader;
import java.io.IOException;

public class TaxonMapParser {
    public void parse(BufferedReader reader, TaxonMapListener listener) throws IOException {
            LabeledCSVParser labeledCSVParser = CSVUtil.createLabeledCSVParser(reader);
            listener.start();
            while (labeledCSVParser.getLine() != null) {
                Taxon providedTaxon = new TaxonImpl();
                providedTaxon.setExternalId(CSVUtil.valueOrNull(labeledCSVParser, "providedTaxonId"));
                providedTaxon.setName(CSVUtil.valueOrNull(labeledCSVParser, "providedTaxonName"));
                Taxon resolvedTaxon = new TaxonImpl();
                resolvedTaxon.setExternalId(CSVUtil.valueOrNull(labeledCSVParser, "resolvedTaxonId"));
                resolvedTaxon.setName(CSVUtil.valueOrNull(labeledCSVParser, "resolvedTaxonName"));
                listener.addMapping(providedTaxon, resolvedTaxon);
            }
            listener.finish();
        }

}

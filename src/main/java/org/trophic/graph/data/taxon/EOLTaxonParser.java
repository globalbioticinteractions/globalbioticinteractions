package org.trophic.graph.data.taxon;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.trophic.graph.service.EOLTaxonImageService;

import java.io.BufferedReader;
import java.io.IOException;

public  class EOLTaxonParser implements TaxonParser {
    @Override
    public void parse(BufferedReader reader, TaxonTermListener listener) throws IOException {
        LabeledCSVParser labeledCSVParser = new LabeledCSVParser(new CSVParser(reader));
        labeledCSVParser.changeDelimiter('\t');
        while (labeledCSVParser.getLine() != null) {
            TaxonTerm term = new TaxonTerm();
            term.setId(EOLTaxonImageService.EOL_LSID_PREFIX + labeledCSVParser.getValueByLabel("taxonID"));
            term.setName(labeledCSVParser.getValueByLabel("scientificName"));
            term.setRank(labeledCSVParser.getValueByLabel("taxonRank"));
            listener.notifyTerm(term);
        }

    }

    @Override
    public int getExpectedMaxTerms() {
        return 2474168;
    }
}

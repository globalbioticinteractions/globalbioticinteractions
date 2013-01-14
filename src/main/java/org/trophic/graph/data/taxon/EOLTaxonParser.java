package org.trophic.graph.data.taxon;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.trophic.graph.service.EOLTaxonImageService;

import java.io.BufferedReader;
import java.io.IOException;

public class EOLTaxonParser implements TaxonParser {

    @Override
    public void parse(BufferedReader reader, TaxonImportListener listener) throws IOException {
        LabeledCSVParser labeledCSVParser = new LabeledCSVParser(new CSVParser(reader));
        labeledCSVParser.changeDelimiter('\t');
        listener.start();
        while (labeledCSVParser.getLine() != null) {
            long taxonID = Long.parseLong(labeledCSVParser.getValueByLabel("taxonID"));
            listener.addTerm(labeledCSVParser.getValueByLabel("scientificName"), taxonID);
        }
        listener.finish();
    }

    @Override
    public int getExpectedMaxTerms() {
        return 2474168;
    }
}

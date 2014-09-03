package org.eol.globi.data.taxon;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;

import java.io.BufferedReader;
import java.io.IOException;

public class GulfBaseTaxonParser implements TaxonParser {
    @Override
    public void parse(BufferedReader reader, TaxonImportListener listener) throws IOException {
        LabeledCSVParser labeledCSVParser = new LabeledCSVParser(new CSVParser(reader));
        listener.start();
        while (labeledCSVParser.getLine() != null) {
            StringBuilder rankPathBuffer = new StringBuilder();

            String[] nonSpeciesRankFieldNames = {"Kingdom", "Phylum", "Subphylum", "Class", "Subclass", "Infraclass", "Superorder", "Order", "Suborder", "Infraorder", "Section", "Subsection", "Superfamily", "Above family", "Family", "Subfamily", "Tribe", "Supergenus", "Genus"};
            for (String label : nonSpeciesRankFieldNames) {
                String value = labeledCSVParser.getValueByLabel(label);
                if (value == null) {
                    throw new IOException("failed to field [" + label + "] at line [" + labeledCSVParser.getLastLineNumber() + "]");
                }
                if (StringUtils.isNotBlank(value)) {
                    rankPathBuffer.append(value);
                    rankPathBuffer.append(' ');
                }
            }
            Taxon term = new TaxonImpl();
            term.setExternalId(labeledCSVParser.getValueByLabel("Species number"));
            term.setName(labeledCSVParser.getValueByLabel("Scientific name"));
            term.setPath(rankPathBuffer.toString().trim());
            listener.addTerm(term);
        }
        listener.finish();

    }

    @Override
    public int getExpectedMaxTerms() {
        return 14469;
    }
}

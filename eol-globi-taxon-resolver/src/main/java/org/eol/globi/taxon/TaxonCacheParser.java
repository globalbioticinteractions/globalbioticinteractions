package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.CSVUtil;

import java.io.BufferedReader;
import java.io.IOException;

public class TaxonCacheParser {

    public void parse(BufferedReader reader, TaxonCacheListener listener) throws IOException {
        LabeledCSVParser labeledCSVParser = CSVUtil.createLabeledCSVParser(reader);
        listener.start();
        while (labeledCSVParser.getLine() != null) {
            Taxon taxa = new TaxonImpl();
            taxa.setExternalId(CSVUtil.valueOrNull(labeledCSVParser, "id"));
            taxa.setName(CSVUtil.valueOrNull(labeledCSVParser, "name"));
            taxa.setRank(CSVUtil.valueOrNull(labeledCSVParser, "rank"));
            taxa.setPath(CSVUtil.valueOrNull(labeledCSVParser, "path"));
            taxa.setPathIds(CSVUtil.valueOrNull(labeledCSVParser, "pathIds"));
            taxa.setPathNames(CSVUtil.valueOrNull(labeledCSVParser, "pathNames"));
            taxa.setExternalUrl(CSVUtil.valueOrNull(labeledCSVParser, "externalUrl"));
            taxa.setThumbnailUrl(CSVUtil.valueOrNull(labeledCSVParser, "thumbnailUrl"));
            listener.addTaxon(taxa);
        }
        listener.finish();
    }

}

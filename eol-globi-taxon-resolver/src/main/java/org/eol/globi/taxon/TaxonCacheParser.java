package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.CSVUtil;

public class TaxonCacheParser {

    public static final String MISSING_THUMBNAIL = "";

    public static Taxon parseLine(LabeledCSVParser labeledCSVParser) {
        Taxon taxa = new TaxonImpl();
        taxa.setExternalId(CSVUtil.valueOrNull(labeledCSVParser, "id"));
        taxa.setName(CSVUtil.valueOrNull(labeledCSVParser, "name"));
        taxa.setRank(CSVUtil.valueOrNull(labeledCSVParser, "rank"));
        taxa.setPath(CSVUtil.valueOrNull(labeledCSVParser, "path"));
        taxa.setPathIds(CSVUtil.valueOrNull(labeledCSVParser, "pathIds"));
        taxa.setPathNames(CSVUtil.valueOrNull(labeledCSVParser, "pathNames"));
        taxa.setExternalUrl(CSVUtil.valueOrNull(labeledCSVParser, "externalUrl"));
        taxa.setThumbnailUrl(CSVUtil.valueOrDefault(labeledCSVParser, "thumbnailUrl", MISSING_THUMBNAIL));
        return taxa;
    }

}

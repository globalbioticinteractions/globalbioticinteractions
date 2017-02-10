package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.CSVTSVUtil;

public class TaxonCacheParser {

    public static final String MISSING_THUMBNAIL = "";

    public static Taxon parseLine(LabeledCSVParser labeledCSVParser) {
        Taxon taxa = new TaxonImpl();
        taxa.setExternalId(CSVTSVUtil.valueOrNull(labeledCSVParser, "id"));
        taxa.setName(CSVTSVUtil.valueOrNull(labeledCSVParser, "name"));
        taxa.setRank(CSVTSVUtil.valueOrNull(labeledCSVParser, "rank"));
        taxa.setPath(CSVTSVUtil.valueOrNull(labeledCSVParser, "path"));
        taxa.setPathIds(CSVTSVUtil.valueOrNull(labeledCSVParser, "pathIds"));
        taxa.setPathNames(CSVTSVUtil.valueOrNull(labeledCSVParser, "pathNames"));
        taxa.setCommonNames(CSVTSVUtil.valueOrNull(labeledCSVParser, "commonNames"));
        taxa.setExternalUrl(CSVTSVUtil.valueOrNull(labeledCSVParser, "externalUrl"));
        taxa.setThumbnailUrl(CSVTSVUtil.valueOrDefault(labeledCSVParser, "thumbnailUrl", MISSING_THUMBNAIL));
        return taxa;
    }

}

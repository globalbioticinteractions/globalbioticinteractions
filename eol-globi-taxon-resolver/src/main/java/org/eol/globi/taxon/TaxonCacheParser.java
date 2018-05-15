package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.CSVTSVUtil;

public class TaxonCacheParser {

    public static final String MISSING_THUMBNAIL = "";

    public static Taxon parseLine(LabeledCSVParser labeledCSVParser) {
        Taxon taxon = new TaxonImpl();
        taxon.setExternalId(CSVTSVUtil.valueOrNull(labeledCSVParser, "id"));
        taxon.setName(CSVTSVUtil.valueOrNull(labeledCSVParser, "name"));
        taxon.setRank(CSVTSVUtil.valueOrNull(labeledCSVParser, "rank"));
        taxon.setPath(CSVTSVUtil.valueOrNull(labeledCSVParser, "path"));
        taxon.setPathIds(CSVTSVUtil.valueOrNull(labeledCSVParser, "pathIds"));
        taxon.setPathNames(CSVTSVUtil.valueOrNull(labeledCSVParser, "pathNames"));
        taxon.setCommonNames(CSVTSVUtil.valueOrNull(labeledCSVParser, "commonNames"));
        taxon.setExternalUrl(CSVTSVUtil.valueOrNull(labeledCSVParser, "externalUrl"));
        taxon.setThumbnailUrl(CSVTSVUtil.valueOrDefault(labeledCSVParser, "thumbnailUrl", MISSING_THUMBNAIL));
        return taxon;
    }

    public static Taxon parseLine(String aline) {
        Taxon taxon = null;
        if (StringUtils.isNotBlank(aline)) {
            taxon = new TaxonImpl();
            String[] line = CSVTSVUtil.splitTSV(aline);
            taxon.setExternalId(CSVTSVUtil.valueOrNull(line, 0));
            taxon.setName(CSVTSVUtil.valueOrNull(line, 1));
            taxon.setRank(CSVTSVUtil.valueOrNull(line, 2));
            taxon.setCommonNames(CSVTSVUtil.valueOrNull(line, 3));
            taxon.setPath(CSVTSVUtil.valueOrNull(line, 4));
            taxon.setPathIds(CSVTSVUtil.valueOrNull(line, 5));
            taxon.setPathNames(CSVTSVUtil.valueOrNull(line, 6));
            taxon.setExternalUrl(CSVTSVUtil.valueOrNull(line, 7));
            taxon.setThumbnailUrl(CSVTSVUtil.valueOrDefault(line, 8, MISSING_THUMBNAIL));
        }
        return taxon;
    }

}

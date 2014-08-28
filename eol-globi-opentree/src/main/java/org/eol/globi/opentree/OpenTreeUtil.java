package org.eol.globi.opentree;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.io.InputStream;

public class OpenTreeUtil {
    public static void readTaxonomy(TaxonListener listener, InputStream inputStream) throws IOException {
        LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(inputStream, '\t'));
        while (parser.getLine() != null) {
            String taxonId = parser.getValueByLabel("uid");
            String[] externalIds = StringUtils.split(parser.getValueByLabel("sourceinfo"), ",");
            for (String otherTaxonId : externalIds) {
                String otherId = StringUtils.replace(otherTaxonId, "gbif:", TaxonomyProvider.ID_PREFIX_GBIF);
                otherId = StringUtils.replace(otherId, "ncbi:", TaxonomyProvider.NCBI.getIdPrefix());
                listener.taxonSameAs(taxonId, otherId);
            }
        }
    }
}

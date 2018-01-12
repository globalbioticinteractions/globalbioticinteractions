package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.TaxonomyProvider;
import org.mapdb.Fun;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;

public class NODCTaxonParser implements Iterator<Fun.Tuple2<String, String>> {
    private static final Log LOG = LogFactory.getLog(NODCTaxonParser.class);

    private String currentLine = null;
    private BufferedReader reader = null;

    NODCTaxonParser(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = false;
        try {
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.length() == 73) {
                    String controlCode = currentLine.substring(61, 62);
                    if (StringUtils.isBlank(controlCode)) {
                        hasNext = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("unexpected exception while reading nodc file", e);
        }
        return hasNext;
    }

    @Override
    public Fun.Tuple2<String, String> next() {
        String nodcCode = StringUtils.trim(currentLine.substring(48, 60));
        String tsn = StringUtils.trim(currentLine.substring(67, 73));
        return new Fun.Tuple2<String, String>(
                TaxonomyProvider.NATIONAL_OCEANOGRAPHIC_DATA_CENTER.getIdPrefix() + nodcCode.replace("00", ""),
                TaxonomyProvider.ID_PREFIX_ITIS + tsn.replaceFirst("^0+", ""));
    }

    @Override
    public void remove() {
            throw new UnsupportedOperationException("remove");
        }


}

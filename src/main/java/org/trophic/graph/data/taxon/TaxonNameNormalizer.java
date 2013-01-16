package org.trophic.graph.data.taxon;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TaxonNameNormalizer {

    private Map<String, String> corrections;

    private static String clean(String name) {
        name = name.replaceAll("\\(.*\\)", "");
        name = name.replaceAll("[¬†*?]", "");
        name = name.replaceAll("spp\\.$", "");
        name = name.replaceAll("spp$", "");
        name = name.replaceAll("sp\\.$", "");
        name = name.replaceAll("^'", "");
        name = name.replaceAll("'$", "");
        name = name.replaceAll(" cf ", " ");
        String trim = name.trim();
        return trim.replaceAll("(\\s+)", " ");
    }

    public String normalize(String taxonName) {
        String cleanName = clean(taxonName);
        if (!isInitialized()) {
            doInit();
        }
        String suggestedReplacement = corrections.get(cleanName);
        if (suggestedReplacement != null) {
            cleanName = suggestedReplacement;
        }
        if (StringUtils.isBlank(cleanName)) {
            cleanName = "NomenNescio";
        }

        return cleanName;
    }

    private void doInit() {
        try {
            InputStream is = getClass().getResourceAsStream("taxonNameCorrections.csv");
            LabeledCSVParser labeledCSVParser = new LabeledCSVParser(new CSVParser(is));
            String[] line = null;

            corrections = new HashMap<String, String>();
            while ((line = labeledCSVParser.getLine()) != null) {
                if (line.length > 1) {
                    corrections.put(line[0], line[1]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to initialize taxon name normalizer", e);
        }
    }

    private boolean isInitialized() {
        return corrections != null;
    }
}

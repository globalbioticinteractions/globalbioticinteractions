package org.eol.globi.data.taxon;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TaxonNameNormalizer {

    private Map<String, String> corrections;

    private static String clean(String name) {
        name = removePartsInParentheses(name);
        // replace all kind of non character and non-numbers with ordinary whitespace
        name = keepOnlyLettersAndNumbers(name);
        name = name.replaceAll("\\s(spp|sp)\\.*($|\\s.*)", "");
        name = name.replaceAll(" cf ", " ");
        String trim = name.trim();
        return replaceMultipleWhiteSpacesWithSingleWhitespace(trim);
    }

    private static String replaceMultipleWhiteSpacesWithSingleWhitespace(String trim) {
        return trim.replaceAll("(\\s+)", " ");
    }

    private static String removePartsInParentheses(String name) {
        return name.replaceAll("\\(.*\\)", "");
    }

    private static String keepOnlyLettersAndNumbers(String name) {
        name = name.replaceAll("[^\\p{L}\\p{N}]", " ");
        return name;
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
            InputStream resourceAsStream = getClass().getResourceAsStream("taxonNameCorrections.csv");
            BufferedReader is = org.eol.globi.data.FileUtils.getUncompressedBufferedReaderUTF_8(resourceAsStream);
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

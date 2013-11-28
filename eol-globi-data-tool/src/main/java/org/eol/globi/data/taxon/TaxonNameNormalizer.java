package org.eol.globi.data.taxon;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.CharsetConstant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TaxonNameNormalizer {

    private static final Log LOG = LogFactory.getLog(TaxonNameNormalizer.class);

    private Map<String, String> corrections;

    private static String clean(String name) {
        name = name.replaceAll("<quotes>", "");
        name = name.replaceAll("\\s.*/.*($|\\s)", "");
        name = name.replaceAll("\\s×", " x");
        name = name.replaceAll("\\sX", " x");
        name = removePartsInParentheses(name);
        name = keepOnlyLettersAndNumbers(name);
        name = name.replaceAll("\\s(spp|sp)\\.*($|\\s.*)", "");
        name = name.replaceAll(" cf .*", " ");
        name = name.replaceAll(" var ", " var. ");
        name = name.replaceAll(" variety ", " var. ");
        name = name.replaceAll(" varietas ", " var. ");
        name = name.replaceAll(" ssp ", " ssp. ");
        name = name.replaceAll(" subsp ", " ssp. ");
        name = name.replaceAll(" subsp. ", " ssp. ");
        name = name.replaceAll(" subspecies ", " ssp. ");
        name = name.replaceAll("^\\w$", "");
        name = name.replaceAll("ü", "ue");
        name = name.replaceAll("ë", "e");
        String trim = name.trim();
        return replaceMultipleWhiteSpacesWithSingleWhitespace(trim);
    }

    private static String replaceMultipleWhiteSpacesWithSingleWhitespace(String trim) {
        return trim.replaceAll("(\\s+)", " ");
    }

    private static String removePartsInParentheses(String name) {
        if (!name.matches(".*\\((bot\\.|bot|Bot|Bot\\.|Zool\\.|zoo)\\)")) {
            name = name.replaceAll("\\(.*\\)", "");
        }
        return name;
    }

    private static String keepOnlyLettersAndNumbers(String name) {
        name = name.replaceAll("[^\\p{L}\\p{N}-\\.\\(\\)]", " ");
        return name;
    }

    public String normalize(String taxonName) {
        String cleanName = clean(taxonName);
        if (cleanName.length() < 2) {
            cleanName = "no name";
        } else {
            if (!isInitialized()) {
                doInit();
            }
            String suggestedReplacement = corrections.get(cleanName);
            if (suggestedReplacement != null) {
                cleanName = suggestedReplacement;
            }
        }
        return cleanName.trim();
    }

    private void doInit() {
        try {
            InputStream resourceAsStream = getClass().getResourceAsStream("taxonNameCorrections.csv");
            BufferedReader is = org.eol.globi.data.FileUtils.getUncompressedBufferedReader(resourceAsStream, CharsetConstant.UTF8);
            LabeledCSVParser labeledCSVParser = new LabeledCSVParser(new CSVParser(is));
            String[] line;

            corrections = new HashMap<String, String>();
            while ((line = labeledCSVParser.getLine()) != null) {
                if (line.length > 1) {
                    String original = line[0];
                    String correction = line[1];
                    if (StringUtils.isBlank(correction) || correction.trim().length() < 2) {
                        throw new RuntimeException("found invalid blank or single character conversion for [" + original + "]");
                    }
                    corrections.put(original, correction);
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

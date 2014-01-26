package org.eol.globi.data.taxon;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.service.NameSuggestor;
import org.eol.globi.service.UKSISuggestionService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxonNameCorrector implements CorrectionService {

    private List<NameSuggestor> suggestors = null;

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

    @Override
    public String correct(String taxonName) {
        if (suggestors == null) {
            suggestors = new ArrayList<NameSuggestor>() {
                {
                    add(new UKSISuggestionService());
                    add(new NameScrubber());
                    add(new ManualSuggestor());
                }
            };
        }
        String nameSuggestion = taxonName;
        for (NameSuggestor suggestor : suggestors) {
            nameSuggestion = suggestor.suggest(nameSuggestion);
            if (StringUtils.length(nameSuggestion) < 2) {
                nameSuggestion = "no name";
                break;
            }
        }
        return nameSuggestion.trim();
    }

    private static class NameScrubber implements NameSuggestor {
        @Override
        public String suggest(final String name) {
            return clean(name);
        }
    }

    private class ManualSuggestor implements NameSuggestor {
        private Map<String, String> corrections;

        @Override
        public String suggest(final String name) {
            if (!isInitialized()) {
                doInit();
            }
            String suggestedReplacement = corrections.get(name);
            return StringUtils.isBlank(suggestedReplacement) ? name : suggestedReplacement;
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
}

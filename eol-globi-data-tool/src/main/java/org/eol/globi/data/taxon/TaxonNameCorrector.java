package org.eol.globi.data.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.NameSuggestor;
import org.eol.globi.service.UKSISuggestionService;

import java.util.ArrayList;
import java.util.List;

public class TaxonNameCorrector implements CorrectionService {

    private static final Log LOG = LogFactory.getLog(TaxonNameCorrector.class);

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
        name = name.replaceAll(" cf\\. .*", " ");
        name = name.replaceAll(" var ", " var. ");
        name = name.replaceAll(" variety ", " var. ");
        name = name.replaceAll(" varietas ", " var. ");
        name = name.replaceAll(" ssp ", " ssp. ");
        name = name.replaceAll(" subsp ", " ssp. ");
        name = name.replaceAll(" subsp. ", " ssp. ");
        name = name.replaceAll(" subspecies ", " ssp. ");
        name = name.replaceAll("^\\w$", "");
        name = name.replaceAll("ë", "e");
        name = name.replaceAll("ü", "u");
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
        String suggestion;
        if (StringUtils.isBlank(taxonName)) {
            suggestion = PropertyAndValueDictionary.NO_NAME;
        } else if (StringUtils.equals(taxonName, PropertyAndValueDictionary.NO_MATCH)) {
            suggestion = PropertyAndValueDictionary.NO_MATCH;
        } else {
            suggestion = suggestCorrection(taxonName);
        }
        return suggestion;
    }

    private String suggestCorrection(String taxonName) {
        String suggestion;
        if (suggestors == null) {
            suggestors = new ArrayList<NameSuggestor>() {
                {
                    add(new UKSISuggestionService());
                    add(new ManualSuggestor());
                    add(new NameScrubber());
                    add(new ManualSuggestor());
                }
            };
        }
        List<String> suggestions = new ArrayList<String>();
        suggestion = taxonName;
        suggestions.add(suggestion);
        boolean isCircular = false;
        while (!isCircular) {
            String newSuggestion = suggest(suggestion);
            if (StringUtils.equals(newSuggestion, suggestion)) {
                break;
            } else if (suggestions.contains(newSuggestion)) {
                isCircular = true;
                suggestions.add(newSuggestion);
                LOG.warn("found circular suggestion path " + suggestions + ": choosing original [" + taxonName + "] instead");
            } else {
                suggestions.add(newSuggestion);
                suggestion = newSuggestion;
            }
        }
        suggestion = isCircular ? suggestions.get(0) : suggestions.get(suggestions.size() - 1);
        return suggestion;
    }

    private String suggest(String nameSuggestion) {
        for (NameSuggestor suggestor : suggestors) {
            nameSuggestion = StringUtils.trim(suggestor.suggest(nameSuggestion));
            if (StringUtils.length(nameSuggestion) < 2) {
                nameSuggestion = PropertyAndValueDictionary.NO_NAME;
                break;
            }
        }
        return nameSuggestion;
    }

    private static class NameScrubber implements NameSuggestor {
        @Override
        public String suggest(final String name) {
            return clean(name);
        }
    }

}

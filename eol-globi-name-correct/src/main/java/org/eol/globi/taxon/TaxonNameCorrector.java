package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.NameSuggester;
import org.eol.globi.service.UKSISuggestionService;

import java.util.ArrayList;
import java.util.List;

public class TaxonNameCorrector implements CorrectionService {

    private static final Log LOG = LogFactory.getLog(TaxonNameCorrector.class);

    private List<NameSuggester> suggestors = null;

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
            suggestors = new ArrayList<NameSuggester>() {
                {
                    add(new UKSISuggestionService());
                    add(new ManualSuggester());
                    add(new NameScrubber());
                    add(new GlobalNamesCanon());
                    add(new ManualSuggester());
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
        for (NameSuggester suggestor : suggestors) {
            nameSuggestion = StringUtils.trim(suggestor.suggest(nameSuggestion));
            if (StringUtils.length(nameSuggestion) < 2) {
                nameSuggestion = PropertyAndValueDictionary.NO_NAME;
                break;
            }
        }
        return nameSuggestion;
    }

}

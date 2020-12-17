package org.eol.globi.process;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.AssociatedTaxaUtil;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.StudyImporterException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_CATALOG_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_COLLECTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_COLLECTION_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_INSTITUTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_OCCURRENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_CATALOG_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_COLLECTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_COLLECTION_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_INSTITUTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;

public class InteractionExpander implements InteractionProcessor {

    private final InteractionListener listener;
    private final ImportLogger logger;

    public InteractionExpander(InteractionListener listener, ImportLogger logger) {
        this.listener = listener;
        this.logger = logger;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        List<Map<String, String>> propertiesList = AssociatedTaxaUtil.expandIfNeeded(interaction);
        for (Map<String, String> expandedLink : propertiesList) {
            addPlaceholderNamesIfNeeded(expandedLink);
            emit(expandedLink);
        }
    }

    @Override
    public void emit(Map<String, String> interaction) throws StudyImporterException {
        listener.on(interaction);
    }

    void addPlaceholderNamesIfNeeded(Map<String, String> expandedLink) {
        if (InteractionValidator.createSourceTaxonPredicate(null).negate().test(expandedLink)) {
            Stream<String> placeholderNames = Stream.of(
                    SOURCE_INSTITUTION_CODE,
                    SOURCE_COLLECTION_CODE,
                    SOURCE_COLLECTION_ID,
                    SOURCE_CATALOG_NUMBER,
                    SOURCE_OCCURRENCE_ID);
            addPlaceholderNamesIfPossible(expandedLink, placeholderNames, "source", SOURCE_TAXON_NAME);
        }
        if (InteractionValidator.createTargetTaxonPredicate(null).negate().test(expandedLink)) {
            Stream<String> placeholderNames = Stream.of(
                    TARGET_INSTITUTION_CODE,
                    TARGET_COLLECTION_CODE,
                    TARGET_COLLECTION_ID,
                    TARGET_CATALOG_NUMBER,
                    TARGET_OCCURRENCE_ID);
            addPlaceholderNamesIfPossible(expandedLink, placeholderNames, "target", TARGET_TAXON_NAME);
        }
    }

    private void addPlaceholderNamesIfPossible(Map<String, String> expandedLink, Stream<String> placeholderNames, String sourceOrTarget, String nameToBeFilled) {
        final String placeholderMessage = " using institutionCode/collectionCode/collectionId/catalogNumber/occurrenceId as placeholder";

        String targetNamePlaceholder = placeholderNames
                .map(expandedLink::get)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(CharsetConstant.SEPARATOR));
        if (StringUtils.isNotBlank(targetNamePlaceholder)) {
            expandedLink.putIfAbsent(nameToBeFilled, targetNamePlaceholder);
            LogUtil.logWarningIfPossible(expandedLink, sourceOrTarget + " taxon name missing:" + placeholderMessage, logger);
        }
    }

}

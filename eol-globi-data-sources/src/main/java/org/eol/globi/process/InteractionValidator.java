package org.eol.globi.process;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.LogUtil;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.InteractType;

import java.util.Map;
import java.util.function.Predicate;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;

public class InteractionValidator extends InteractionProcessorAbstract {


    public InteractionValidator(InteractionListener listener, ImportLogger logger) {
        super(listener, logger);
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        if (interaction != null && isValidInteraction(interaction, this.logger)) {
            org.eol.globi.process.LogUtil.logIfPossible(interaction, "biotic interaction found", this.logger);
            emit(interaction);
        }
    }

    private boolean isValidInteraction(Map<String, String> link, ImportLogger logger) {
        Predicate<Map<String, String>> hasSourceTaxon = createSourceTaxonPredicate(logger);

        Predicate<Map<String, String>> hasTargetTaxon = createTargetTaxonPredicate(logger);

        Predicate<Map<String, String>> hasInteractionType = createInteractionTypePredicate(logger);

        Predicate<Map<String, String>> hasReferenceId = createReferencePredicate(logger);

        return hasSourceTaxon
                .and(hasTargetTaxon)
                .and(hasInteractionType)
                .and(hasReferenceId)
                .test(link);
    }

    static Predicate<Map<String, String>> createSourceTaxonPredicate(ImportLogger logger) {
        return (Map<String, String> l) -> {
            String sourceTaxonName = l.get(SOURCE_TAXON_NAME);
            String sourceTaxonId = l.get(SOURCE_TAXON_ID);
            boolean isValid = StringUtils.isNotBlank(sourceTaxonName) || StringUtils.isNotBlank(sourceTaxonId);
            if (!isValid && logger != null) {
                logger.warn(LogUtil.contextFor(l), "source taxon name missing");
            }
            return isValid;
        };
    }

    static Predicate<Map<String, String>> createTargetTaxonPredicate(ImportLogger logger) {
        return (Map<String, String> l) -> {
            String targetTaxonName = l.get(TARGET_TAXON_NAME);
            String targetTaxonId = l.get(TARGET_TAXON_ID);

            boolean isValid = StringUtils.isNotBlank(targetTaxonName) || StringUtils.isNotBlank(targetTaxonId);
            if (!isValid && logger != null) {
                logger.warn(LogUtil.contextFor(l), "target taxon name missing");
            }
            return isValid;
        };
    }

    private static Predicate<Map<String, String>> createReferencePredicate(ImportLogger logger) {
        return (Map<String, String> l) -> {
            boolean isValid = StringUtils.isNotBlank(l.get(REFERENCE_ID));
            if (!isValid && logger != null) {
                logger.warn(LogUtil.contextFor(l), "missing [" + REFERENCE_ID + "]");
            }
            return isValid;
        };
    }

    static Predicate<Map<String, String>> createInteractionTypePredicate(ImportLogger logger) {
        return (Map<String, String> l) -> {
            String interactionTypeId = l.get(INTERACTION_TYPE_ID);
            boolean hasValidId = false;
            if (StringUtils.isBlank(interactionTypeId) && logger != null) {
                if (StringUtils.isBlank(l.get(INTERACTION_TYPE_NAME))) {
                    logger.warn(LogUtil.contextFor(l), "missing interaction type");
                } else {
                    logger.warn(LogUtil.contextFor(l), "found unsupported interaction type with name: [" + l.get(INTERACTION_TYPE_NAME) + "]");
                }
            } else {
                hasValidId = InteractType.typeOf(interactionTypeId) != null;
                if (!hasValidId && logger != null) {
                    StringBuilder msg = new StringBuilder("found unsupported interaction type with id: [" + interactionTypeId + "]");
                    if (StringUtils.isNotBlank(l.get(INTERACTION_TYPE_NAME))) {
                        msg.append(" and name: [")
                                .append(l.get(INTERACTION_TYPE_NAME))
                                .append("]");
                    }
                    logger.warn(LogUtil.contextFor(l), msg.toString());
                }
            }
            return hasValidId;
        };
    }


}

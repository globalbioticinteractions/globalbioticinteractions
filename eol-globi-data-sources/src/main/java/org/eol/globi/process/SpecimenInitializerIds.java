package org.eol.globi.process;

import org.eol.globi.domain.Specimen;

import java.util.Map;
import java.util.function.Consumer;

import static org.eol.globi.domain.PropertyAndValueDictionary.CATALOG_NUMBER;
import static org.eol.globi.domain.PropertyAndValueDictionary.COLLECTION_CODE;
import static org.eol.globi.domain.PropertyAndValueDictionary.COLLECTION_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.INSTITUTION_CODE;
import static org.eol.globi.domain.PropertyAndValueDictionary.OCCURRENCE_ID;

public class SpecimenInitializerIds implements Consumer<Specimen> {
    private final Map<String, String> interaction;
    private final String occurrenceIdLabel;
    private final String catalogNumberLabel;
    private final String collectionCodeLabel;
    private final String collectionIdLabel;
    private final String institutionCodeLabel;

    public SpecimenInitializerIds(Map<String, String> interaction, String sourceOccurrenceIdLabel, String catalogNumberLabel, String sourceCollectionCodeLabel, String collectionIdLabel, String institutionCodeLabel) {
        this.interaction = interaction;
        this.occurrenceIdLabel = sourceOccurrenceIdLabel;
        this.catalogNumberLabel = catalogNumberLabel;
        this.collectionCodeLabel = sourceCollectionCodeLabel;
        this.collectionIdLabel = collectionIdLabel;
        this.institutionCodeLabel = institutionCodeLabel;
    }

    @Override
    public void accept(Specimen source) {
        InteractionImporter.setExternalIdNotBlank(interaction, occurrenceIdLabel, source);
        InteractionImporter.setPropertyIfAvailable(interaction, source, occurrenceIdLabel, OCCURRENCE_ID);
        InteractionImporter.setPropertyIfAvailable(interaction, source, catalogNumberLabel, CATALOG_NUMBER);
        InteractionImporter.setPropertyIfAvailable(interaction, source, collectionCodeLabel, COLLECTION_CODE);
        InteractionImporter.setPropertyIfAvailable(interaction, source, collectionIdLabel, COLLECTION_ID);
        InteractionImporter.setPropertyIfAvailable(interaction, source, institutionCodeLabel, INSTITUTION_CODE);
    }
}

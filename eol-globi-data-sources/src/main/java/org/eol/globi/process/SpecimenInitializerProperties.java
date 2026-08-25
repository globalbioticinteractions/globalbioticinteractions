package org.eol.globi.process;

import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Specimen;

import java.util.Map;
import java.util.function.Consumer;

public class SpecimenInitializerProperties implements Consumer<Specimen> {

    private final Map<String, String> interaction;
    private final String bodyPartName;
    private final String bodyPartId;
    private final String lifeStageName;
    private final String lifeStageId;
    private final String sexLabel;
    private final String sexId;
    private final Consumer<Specimen> initializer;
    private final NodeFactory nodeFactory;
    private ImportLogger logger;

    public SpecimenInitializerProperties(NodeFactory nodeFactory1,
                                         Consumer<Specimen> specimenInitializer,
                                         Map<String, String> interaction,
                                         String bodyPartName,
                                         String bodyPartId,
                                         String lifeStageName,
                                         String lifeStageId,
                                         String sexLabel,
                                         String sexId,
                                         ImportLogger importLogger) {
        this.interaction = interaction;
        this.bodyPartName = bodyPartName;
        this.bodyPartId = bodyPartId;
        this.lifeStageName = lifeStageName;
        this.lifeStageId = lifeStageId;
        this.sexLabel = sexLabel;
        this.sexId = sexId;
        initializer = specimenInitializer;
        nodeFactory = nodeFactory1;
        logger = importLogger;
    }


    @Override
    public void accept(Specimen specimen) {
        initializer.accept(specimen);
        InteractionImporter.setBasisOfRecordIfAvailable(interaction, specimen);
        try {
            InteractionImporter.setDateTimeIfAvailable(nodeFactory, logger, interaction, specimen);
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
        InteractionImporter.setBodyPartIfAvailable(interaction, specimen, bodyPartName, bodyPartId);
        InteractionImporter.setLifeStageIfAvailable(interaction, specimen, lifeStageName, lifeStageId);
        InteractionImporter.setSexIfAvailable(interaction, specimen, sexLabel, sexId);
    }
}

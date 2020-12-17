package org.eol.globi.process;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.GeoNamesService;
import org.joda.time.Interval;

import java.util.Map;

public class InteractionListenerImpl implements InteractionListener {

    private final InteractionListener processor;

    public InteractionListenerImpl(NodeFactory nodeFactory, GeoNamesService geoNamesService, ImportLogger logger) {
        this.processor =
                new InteractionExpander(
                        new InteractionValidator(
                                new InteractionImporter(nodeFactory, logger, geoNamesService),
                                logger
                        ), logger
                );
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        try {
            processor.on(interaction);
        } catch (StudyImporterException e) {
            throw new StudyImporterException("failed to import: " + interaction, e);
        }
    }

}

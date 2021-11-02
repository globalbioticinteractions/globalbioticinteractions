package org.eol.globi.process;

import org.eol.globi.data.AssociatedTaxaUtil;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.StudyImporterException;

import java.util.List;
import java.util.Map;

public class InteractionExpander extends InteractionProcessorAbstract {

    public InteractionExpander(InteractionListener listener, ImportLogger logger) {
        super(listener, logger);
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        List<Map<String, String>> propertiesList = AssociatedTaxaUtil.expandIfNeeded(interaction);
        for (Map<String, String> expandedLink : propertiesList) {
            emit(expandedLink);
        }
    }

}

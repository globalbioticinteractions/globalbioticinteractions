package org.eol.globi.taxon;

import org.eol.globi.service.PropertyEnricherException;

public interface RowHandler {
    void onRow(String[] row) throws PropertyEnricherException;
}

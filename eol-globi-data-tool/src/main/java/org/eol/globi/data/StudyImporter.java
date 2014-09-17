package org.eol.globi.data;

import org.eol.globi.domain.Study;

public interface StudyImporter {

    Study importStudy() throws StudyImporterException;

    void setFilter(ImportFilter importFilter);

    void setLogger(ImportLogger importLogger);

    boolean shouldCrossCheckReference();
}

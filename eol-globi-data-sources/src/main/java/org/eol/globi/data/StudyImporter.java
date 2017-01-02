package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.service.Dataset;

public interface StudyImporter {

    Study importStudy() throws StudyImporterException;

    void setFilter(ImportFilter importFilter);

    void setLogger(ImportLogger importLogger);

    void setDataset(Dataset dataset);

    boolean shouldCrossCheckReference();
}

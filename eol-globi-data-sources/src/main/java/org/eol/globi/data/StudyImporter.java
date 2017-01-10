package org.eol.globi.data;

import org.eol.globi.service.Dataset;

public interface StudyImporter {

    void importStudy() throws StudyImporterException;

    void setFilter(ImportFilter importFilter);

    void setLogger(ImportLogger importLogger);

    void setDataset(Dataset dataset);

}

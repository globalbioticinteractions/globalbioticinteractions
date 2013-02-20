package org.eol.globi.data;

import org.eol.globi.domain.Study;

public interface StudyImporter {

    Study importStudy() throws StudyImporterException;

    void setImportFilter(ImportFilter importFilter);
}

package org.eol.globi.service;

import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.StudyImporterException;
import org.globalbioticinteractions.dataset.Dataset;

public interface StudyImporterFactory {
    DatasetImporter createImporter(Dataset dataset) throws StudyImporterException;
}

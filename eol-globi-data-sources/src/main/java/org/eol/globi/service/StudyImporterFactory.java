package org.eol.globi.service;

import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.globalbioticinteractions.dataset.Dataset;

public interface StudyImporterFactory {
    StudyImporter createImporter(Dataset dataset) throws StudyImporterException;
}

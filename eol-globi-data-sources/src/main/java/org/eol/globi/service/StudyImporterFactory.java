package org.eol.globi.service;

import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;

public interface StudyImporterFactory {

    StudyImporter createImporter() throws StudyImporterException;
}

package org.eol.globi.export;

import org.eol.globi.domain.StudyNode;

import java.io.IOException;

public interface StudyExporter {
    void exportStudy(StudyNode study, ExportUtil.Appender appender, boolean includeHeader) throws IOException;
}

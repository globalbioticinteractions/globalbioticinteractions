package org.eol.globi.export;

import org.eol.globi.domain.Study;

import java.io.IOException;
import java.io.Writer;

public interface StudyExporter {
    void exportStudy(Study study, ExportUtil.Appender appender, boolean includeHeader) throws IOException;
}

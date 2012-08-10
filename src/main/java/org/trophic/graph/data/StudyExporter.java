package org.trophic.graph.data;

import org.trophic.graph.domain.Study;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public interface StudyExporter {

    public void exportStudy(Study study, Writer writer) throws IOException;

}

package org.trophic.graph.data;

import org.trophic.graph.domain.Study;

import java.io.IOException;

public interface StudyImporter {

    Study importStudy() throws IOException;
}

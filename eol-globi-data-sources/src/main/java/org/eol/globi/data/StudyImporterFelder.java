package org.eol.globi.data;

import org.eol.globi.domain.StudyNode;

public class StudyImporterFelder extends BaseStudyImporter {

    public StudyImporterFelder(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public StudyNode importStudy() throws StudyImporterException {
        return null;
    }
}

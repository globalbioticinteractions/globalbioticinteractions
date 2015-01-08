package org.eol.globi.data;

import org.eol.globi.domain.Study;

public class StudyImporterFelder extends BaseStudyImporter {

    public StudyImporterFelder(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        return null;
    }
}

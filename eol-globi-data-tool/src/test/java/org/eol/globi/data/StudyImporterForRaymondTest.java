package org.eol.globi.data;

import org.junit.Test;

public class StudyImporterForRaymondTest extends GraphDBTestCase{

    @Test
    public void importStudy() throws StudyImporterException {
        StudyImporter importer = new StudyImporterForRaymond(new ParserFactoryImpl(), nodeFactory);
        importer.importStudy();
    }
}

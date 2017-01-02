package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.domain.StudyImpl;
import org.junit.Test;

public class StudyImportLoggerIT extends GraphDBTestCase {

    @Test
    public void logMsg() {
        StudyImportLogger logger = new StudyImportLogger();
        logger.info(nodeFactory.createStudy(new StudyImpl("bla", null, null, null)), "my info message");
    }
}

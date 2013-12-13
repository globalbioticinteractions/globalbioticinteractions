package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.junit.Test;

public class StudyImportLoggerIT extends GraphDBTestCase {

    @Test
    public void logMsg() {
        StudyImportLogger logger = new StudyImportLogger(nodeFactory);
        logger.info(nodeFactory.createStudy("bla"), "my info message");
    }
}

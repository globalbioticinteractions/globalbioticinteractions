package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GraphExporterTest extends GraphDBTestCase {

    @Test
    public void exportAll() throws StudyImporterException, NodeFactoryException, IOException {
        File tmpDir = FileUtils.getTempDirectory();
        File tmpDirPath = new File(tmpDir, "test" + new Random().nextLong());
        FileUtils.forceMkdir(tmpDirPath);
        assertThat(tmpDirPath.list().length, is(0));
        Study study = nodeFactory.getOrCreateStudy2("a study", "a source", "doi:12345L");

        Specimen human = nodeFactory.createSpecimen(study, "Homo sapiens", "BLA:123");
        human.ate(nodeFactory.createSpecimen(study, "Canis familiaris", "BLA:444"));
        try {
            new GraphExporter().export(getGraphDb(), tmpDirPath.getAbsolutePath() + "/");
            assertThat(tmpDirPath.list().length, is(5));
        } finally {
            FileUtils.deleteQuietly(tmpDirPath);
        }

    }

}
package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GraphExporterImplTest extends GraphDBTestCase {

    @Test
    public void exportAll() throws StudyImporterException, IOException {
        File tmpDir = FileUtils.getTempDirectory();
        File tmpDirPath = new File(tmpDir, "test" + new Random().nextLong());
        FileUtils.forceMkdir(tmpDirPath);
        assertThat(tmpDirPath.list().length, is(0));
        Study study = nodeFactory.getOrCreateStudy(new StudyImpl("a study", "a source", "doi:12345L", null));

        Specimen human = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapiens", "NCBI:123"));
        human.ate(nodeFactory.createSpecimen(study, new TaxonImpl("Canis familiaris", "BLA:444")));
        resolveNames();
        try {
            new GraphExporterImpl().export(getGraphDb(), tmpDirPath.getAbsolutePath() + "/");
            assertThat(tmpDirPath.list().length, is(6));
        } finally {
            FileUtils.deleteQuietly(tmpDirPath);
        }

    }

}
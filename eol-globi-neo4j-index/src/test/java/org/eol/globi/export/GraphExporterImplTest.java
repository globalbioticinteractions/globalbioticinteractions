package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.globalbioticinteractions.doi.DOI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class GraphExporterImplTest extends GraphDBTestCase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void exportAll() throws StudyImporterException, IOException {
        File tmpDir = folder.newFolder();
        assertNotNull(tmpDir);
        assertThat(tmpDir.list().length, is(0));
        Study study = nodeFactory.getOrCreateStudy(new StudyImpl("a study", new DOI("12345", "123"), null));

        Specimen human = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapiens", "NCBI:123"));
        human.ate(nodeFactory.createSpecimen(study, new TaxonImpl("Canis familiaris", "BLA:444")));
        resolveNames();

        new GraphExporterImpl().export(getGraphDb(), tmpDir.getAbsolutePath() + "/");
        assertThat(tmpDir.list().length, is(8));

    }

}
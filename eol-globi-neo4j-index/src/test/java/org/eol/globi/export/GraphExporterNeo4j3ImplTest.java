package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.Neo4jIndexType;
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
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;

public class GraphExporterNeo4j3ImplTest extends GraphDBNeo4jTestCase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Override
    protected Neo4jIndexType getSchemaType() {
        return Neo4jIndexType.schema;
    }


    @Test
    public void exportAll() throws StudyImporterException, IOException {
        File tmpDir = folder.newFolder();
        assertNotNull(tmpDir);
        assertThat(tmpDir.list().length, is(0));
        Study study = nodeFactory.getOrCreateStudy(new StudyImpl("a study", new DOI("12345", "123"), null));

        Specimen human = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapiens", "NCBI:123"));
        human.ate(nodeFactory.createSpecimen(study, new TaxonImpl("Canis familiaris", "BLA:444")));
        resolveNames();

        new GraphExporterInteractionsTSVImpl("3").export(getGraphDb(), tmpDir);


        File tsvDir = new File(tmpDir, "tsv");
        assertThat(tsvDir.exists(), is(true));

        List<String> tsvExports = Arrays.asList(tsvDir.list());

        assertThat(tsvExports.size(), is(6));

        assertThat(tsvExports, hasItems(
                "verbatim-interactions.tsv.gz",
                "refuted-verbatim-interactions.tsv.gz",
                "datasets.tsv.gz",
                "interactions.tsv.gz",
                "refuted-interactions.tsv.gz",
                "citations.tsv.gz")
        );

    }

}
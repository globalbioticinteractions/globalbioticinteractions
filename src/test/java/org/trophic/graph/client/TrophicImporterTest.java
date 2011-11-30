package org.trophic.graph.client;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.trophic.graph.data.StudyImporterException;
import org.trophic.graph.db.GraphService;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

public class TrophicImporterTest {


    private File tempFile;

    @Before
    public void createTmpDir() throws IOException {
        tempFile = File.createTempFile("neo4j", "dir");
        FileUtils.deleteQuietly(tempFile);
        tempFile.mkdir();
    }

    @After
    public void deleteTmpDir() {
        FileUtils.deleteQuietly(tempFile);
    }


    @Test
    public void importStudies() throws IOException, StudyImporterException {
        TrophicImporter trophicImporter = new TrophicImporter();

        GraphService.setStoreDir(tempFile.getAbsolutePath());
        trophicImporter.startImportStop(new String[]{});

        GraphDatabaseService graphDatabaseService = null;
        try {
            graphDatabaseService = GraphService.getGraphService();
            assertNotNull(graphDatabaseService.getNodeById(1));
            assertNotNull(graphDatabaseService.getNodeById(200));
        } finally {
            if (graphDatabaseService != null) {
                graphDatabaseService.shutdown();
            }
        }
    }

}
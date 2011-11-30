package org.trophic.graph.client;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.StudyImporterException;
import org.trophic.graph.db.GraphService;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

public class TrophicImporterTest extends GraphDBTestCase {

    @Test
    public void importStudies() throws IOException, StudyImporterException {
        TrophicImporter trophicImporter = new TrophicImporter();

        GraphDatabaseService graphService = getGraphDb();
        trophicImporter.importStudies(graphService);

        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

}
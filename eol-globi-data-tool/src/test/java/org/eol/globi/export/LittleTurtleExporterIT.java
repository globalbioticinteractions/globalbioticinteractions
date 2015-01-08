package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.ImportFilter;
import org.eol.globi.data.NodeFactoryImpl;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSPIRE;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.EnvoLookupService;
import org.eol.globi.service.TermLookupService;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;

public class LittleTurtleExporterIT extends GraphDBTestCase {

    @Override
    protected TermLookupService getEnvoLookupService() {
        return new EnvoLookupService();
    }

    @Test
    public void exportSPIRE() throws IOException, StudyImporterException, NodeFactoryException {
        StudyImporterForSPIRE importer = new StudyImporterForSPIRE(null, nodeFactory);
        importer.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber < 100;
            }
        });
        importer.importStudy();
        List<Study> studies = NodeFactoryImpl.findAllStudies(getGraphDb());


        TaxonNode taxon = nodeFactory.findTaxonByName("Paracalliope fluviatalus");
        TaxonNode sameAsTaxon = nodeFactory.getOrCreateTaxon("bugus same as taxon", "EOL:123", null);

        Transaction tx = getGraphDb().beginTx();
        try {
            taxon.getUnderlyingNode().createRelationshipTo(sameAsTaxon.getUnderlyingNode(), RelTypes.SAME_AS);
            tx.success();
        } finally {
            tx.finish();
        }

        FileUtils.forceMkdir(new File("target"));
        File file = new File("target/spire-as-light-globi.ttl");
        FileUtils.deleteQuietly(file);
        Writer writer = new FileWriter(file);
        LittleTurtleExporter turtleExporter = new LittleTurtleExporter();
        for (Study study : studies) {
            turtleExporter.exportStudy(study, writer, true);
        }
        turtleExporter.exportDataOntology(writer);
        writer.flush();
        writer.close();

        assertTrue(file.exists());

        String content = IOUtils.toString(new FileInputStream(file));
        assertThat(content, not(containsString("no:match")));
        assertThat(content, containsString("OBO:ENVO_00000447"));
    }


}

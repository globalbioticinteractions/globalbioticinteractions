package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.ImportFilter;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSPIRE;
import org.eol.globi.domain.Study;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class GlobiOWLExporterIT extends GraphDBTestCase {

    @Test
    public void importSPIREExportTTL() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException, StudyImporterException {
        StudyImporterForSPIRE importer = new StudyImporterForSPIRE(null, nodeFactory);
        importer.setImportFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber < 100;
            }
        });
        importer.importStudy();
        List<Study> studies = NodeFactory.findAllStudies(getGraphDb());
        StringWriter writer = new StringWriter();
        GlobiOWLExporter globiOWLExporter = new GlobiOWLExporter();
        for (Study study : studies) {
            globiOWLExporter.exportStudy(study, writer, true);
        }
        assertThat(writer.toString(), containsString("has-agent"));

    }


}

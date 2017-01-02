package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.ImportFilter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSPIRE;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.EnvoLookupService;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ExporterRDFTest extends GraphDBTestCase {

    @Override
    protected TermLookupService getEnvoLookupService() {
        return new EnvoLookupService();
    }

    @Override
    protected TaxonIndex getOrCreateTaxonIndex() {
        return getOrCreateTaxonIndex(new PropertyEnricher() {
            @Override
            public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                Taxon taxon = TaxonUtil.mapToTaxon(properties);
                taxon.setPath("one | two | " + taxon.getName());
                taxon.setExternalId(TaxonomyProvider.ID_PREFIX_EOL + taxon.getName());
                taxon.setExternalUrl("http://host/" + taxon.getName());
                return TaxonUtil.taxonToMap(taxon);
            }

            @Override
            public void shutdown() {

            }
        });
    }

    @Test
    public void exportSPIRE() throws IOException, StudyImporterException {
        StudyImporterForSPIRE importer = new StudyImporterForSPIRE(null, nodeFactory);
        importer.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber < 5;
            }
        });
        importStudy(importer);

        List<Study> studies = NodeUtil.findAllStudies(getGraphDb());


        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("some taxon", null));
        Taxon sameAsTaxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("bugus same as taxon", "EOL:123"));

        Transaction tx = getGraphDb().beginTx();
        try {
            assertThat(taxon, is(notNullValue()));
            ((NodeBacked)taxon).getUnderlyingNode().createRelationshipTo(((NodeBacked)sameAsTaxon).getUnderlyingNode(), NodeUtil.asNeo4j(RelTypes.SAME_AS));
            tx.success();
        } finally {
            tx.finish();
        }

        File file = File.createTempFile("spire-as-light-globi", ".nq");
        try {
            Writer writer = new FileWriter(file);
            ExporterRDF turtleExporter = new ExporterRDF();
            for (Study study : studies) {
                turtleExporter.exportStudy(study, writer, true);
            }
            writer.flush();
            writer.close();

            assertTrue(file.exists());

            String content = IOUtils.toString(new FileInputStream(file));
            assertThat(content, not(containsString("no:match")));
            assertThat(content, containsString("http://purl.obolibrary.org/obo/ENVO_"));
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }


}

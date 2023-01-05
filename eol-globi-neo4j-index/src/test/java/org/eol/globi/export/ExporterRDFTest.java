package org.eol.globi.export;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.DatasetImporterForSPIRE;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.service.EnvoLookupService;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceServiceLocal;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.containsString;

public class ExporterRDFTest extends GraphDBNeo4jTestCase {

    @Override
    protected TermLookupService getEnvoLookupService() {
        return new EnvoLookupService(new ResourceServiceHTTP(is -> is));
    }

    @Test
    public void exportSPIRE() throws IOException, StudyImporterException {
        DatasetImporterForSPIRE importer = new DatasetImporterForSPIRE(null, nodeFactory);
        importer.setFilter(recordNumber -> recordNumber < 5);
        DatasetLocal dataset =
                new DatasetLocal(new ResourceServiceLocal(inStream -> inStream, DatasetImporterForSPIRE.class));
        importer.setDataset(dataset);
        importer.setGeoNamesService(new GeoNamesService() {
            @Override
            public boolean hasTermForLocale(String locality) {
                return true;
            }

            @Override
            public LatLng findLatLng(String locality) throws IOException {
                return new LatLng(10, 10);
            }
        });
        importStudy(importer);

        List<StudyNode> studies = NodeUtil.findAllStudies(getGraphDb());


        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("some taxon", null));
        Taxon sameAsTaxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("bugus same as taxon", "EOL:123"));

        assertThat(taxon, is(notNullValue()));
        ((NodeBacked) taxon).getUnderlyingNode().createRelationshipTo(((NodeBacked) sameAsTaxon).getUnderlyingNode(), NodeUtil.asNeo4j(RelTypes.SAME_AS));

        File file = File.createTempFile("spire-as-light-globi", ".nq");
        try {
            Writer writer = new FileWriter(file);
            ExporterRDF turtleExporter = new ExporterRDF();
            for (StudyNode study : studies) {
                turtleExporter.exportStudy(study, ExportUtil.AppenderWriter.of(writer, new ExportUtil.NQuadValueJoiner()), true);
            }
            writer.flush();
            writer.close();

            assertTrue(file.exists());

            String content = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);

            assertThat(content, endsWith("\n"));

            Model model = ModelFactory.createDefaultModel();
            model.read(IOUtils.toInputStream(content, StandardCharsets.UTF_8), "https://example.org", "N-TRIPLE");

            assertThat(content, not(containsString("no:match")));
            assertThat(content, containsString("http://purl.obolibrary.org/obo/ENVO_"));
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }


}

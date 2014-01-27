package org.eol.globi.tool;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSimons;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.geo.EcoRegionFinder;
import org.eol.globi.geo.EcoRegionFinderFactory;
import org.eol.globi.geo.EcoRegionType;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class NormalizerTest extends GraphDBTestCase {

    @Test
    public void doSingleImport() throws IOException, StudyImporterException {
        Normalizer dataNormalizationTool = new Normalizer();

        dataNormalizationTool.setEcoRegionFinderFactory(new EcoRegionFinderFactory() {

            @Override
            public EcoRegionFinder createEcoRegionFinder(EcoRegionType type) {
                return null;
            }

            @Override
            public List<EcoRegionFinder> createAll() {
                return new ArrayList<EcoRegionFinder>();
            }
        });

        GraphDatabaseService graphService = getGraphDb();
        dataNormalizationTool.importData(graphService, new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) throws IOException {
            }
        }, StudyImporterForSimons.class);


        List<Study> allStudies = NodeFactory.findAllStudies(graphService);
        assertThat(allStudies.size(), Is.is(1));
        assertThat(allStudies.get(0).getTitle(), Is.is("Simons 1997"));

        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

    @Test
    public void doSingleImportExport() throws IOException, StudyImporterException {
        Normalizer dataNormalizationTool = new Normalizer();

        GraphDatabaseService graphService = getGraphDb();
        dataNormalizationTool.importData(graphService, new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) throws IOException {
                taxon.setExternalId("test-taxon:" + taxon.getNodeID());
            }
        }, StudyImporterForSimons.class);


        String baseDir = "./target/normalizer-test/";
        FileUtils.deleteQuietly(new File(baseDir));
        dataNormalizationTool.exportData(graphService, baseDir);
    }

}
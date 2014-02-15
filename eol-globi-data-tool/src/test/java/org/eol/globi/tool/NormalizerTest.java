package org.eol.globi.tool;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSimons;
import org.eol.globi.data.taxon.TaxonNameCorrector;
import org.eol.globi.data.taxon.TaxonServiceImpl;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.geo.EcoRegion;
import org.eol.globi.geo.EcoRegionFinder;
import org.eol.globi.geo.EcoRegionFinderException;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class NormalizerTest extends GraphDBTestCase {

    @Test
    public void doSingleImport() throws IOException, StudyImporterException {
        Normalizer dataNormalizationTool = createNormalizer();

        final TaxonPropertyEnricher taxonEnricher = new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {
            }
        };
        dataNormalizationTool.importData(StudyImporterForSimons.class, new NodeFactory(getGraphDb(), new TaxonServiceImpl(taxonEnricher, new TaxonNameCorrector(), getGraphDb())));


        GraphDatabaseService graphService = getGraphDb();

        List<Study> allStudies = NodeFactory.findAllStudies(graphService);
        assertThat(allStudies.size(), Is.is(1));
        assertThat(allStudies.get(0).getTitle(), Is.is("Simons 1997"));

        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

    private Normalizer createNormalizer() {
        Normalizer dataNormalizationTool = new Normalizer();
        dataNormalizationTool.setEcoRegionFinder(new EcoRegionFinder() {
            @Override
            public Collection<EcoRegion> findEcoRegion(double lat, double lng) throws EcoRegionFinderException {
                final EcoRegion ecoRegion = new EcoRegion();
                ecoRegion.setName("some name");
                ecoRegion.setPath("some | path");
                ecoRegion.setId("someId");
                ecoRegion.setGeometry("POINT(1,2)");
                return new ArrayList<EcoRegion>() {{
                    add(ecoRegion);
                }};
            }

            @Override
            public void shutdown() {

            }
        });
        return dataNormalizationTool;
    }

    @Test
    public void doSingleImportExport() throws IOException, StudyImporterException {
        Normalizer dataNormalizationTool = createNormalizer();

        GraphDatabaseService graphService = getGraphDb();
        final TaxonPropertyEnricher taxonEnricher = new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {
                taxon.setExternalId("test-taxon:" + System.currentTimeMillis());
            }
        };
        dataNormalizationTool.importData(StudyImporterForSimons.class, new NodeFactory(graphService, new TaxonServiceImpl(taxonEnricher, new TaxonNameCorrector(), getGraphDb())));


        String baseDir = "./target/normalizer-test/";
        FileUtils.deleteQuietly(new File(baseDir));
        dataNormalizationTool.exportData(graphService, baseDir);
    }

}
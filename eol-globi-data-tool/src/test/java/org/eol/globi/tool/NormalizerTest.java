package org.eol.globi.tool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterTestFactory;
import org.eol.globi.data.StudyImporterForSimons;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NormalizerTest extends GraphDBTestCase {

    private final static Log LOG = LogFactory.getLog(NormalizerTest.class);

    @Test
    public void handleOptions() throws ParseException {
        CommandLine commandLine = Normalizer.parseOptions(new String[]{"-h"});
        assertThat(commandLine.hasOption("h"), is(true));
        commandLine = Normalizer.parseOptions(new String[]{"--help"});
        assertThat(commandLine.hasOption("h"), is(true));

        commandLine = Normalizer.parseOptions(new String[]{"--skipImport"});
        assertThat(commandLine.hasOption("-skipImport"), is(true));
        assertThat(commandLine.hasOption("skipImport"), is(true));

        commandLine = Normalizer.parseOptions(new String[]{"-skipImport", "--skipExport"});
        assertThat(commandLine.hasOption("-skipImport"), is(true));
        assertThat(commandLine.hasOption("skipImport"), is(true));
        assertThat(commandLine.hasOption("skipExport"), is(true));
        assertThat(commandLine.hasOption("skipLink"), is(false));

        commandLine = Normalizer.parseOptions(new String[]{"-skipLink", "--skipExport"});
        assertThat(commandLine.hasOption("skipLink"), is(true));

        commandLine = Normalizer.parseOptions(new String[]{"-datasetDir", "some/bla"});
        assertThat(commandLine.getOptionValue("datasetDir"), is("some/bla"));
    }

    @Test
    public void doSingleImport() throws IOException, StudyImporterException {
        importData(StudyImporterForSimons.class, new NodeFactoryNeo4j(getGraphDb()));
        GraphDatabaseService graphService = getGraphDb();

        Study study = getStudySingleton(graphService);
        assertThat(study.getTitle(), is("Simons 1997"));

        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

    private Normalizer createNormalizer() {
        Normalizer dataNormalizationTool = new Normalizer();
        dataNormalizationTool.setEcoregionFinder(new EcoregionFinder() {
            @Override
            public Collection<Ecoregion> findEcoregion(double lat, double lng) throws EcoregionFinderException {
                final Ecoregion ecoregion = new Ecoregion();
                ecoregion.setName("some name");
                ecoregion.setPath("some | path");
                ecoregion.setId("someId");
                ecoregion.setGeometry("POINT(1,2)");
                return new ArrayList<Ecoregion>() {{
                    add(ecoregion);
                }};
            }

            @Override
            public void shutdown() {

            }
        });
        return dataNormalizationTool;
    }

    @Test
    public void doSingleImportExport() throws IOException, StudyImporterException, URISyntaxException {
        Normalizer dataNormalizationTool = createNormalizer();

        GraphDatabaseService graphService = getGraphDb();

        URL resource = getClass().getResource("datasets-test/globalbioticinteractions/template-dataset/access.tsv");
        assertNotNull(resource);
        String datasetDirTest = new File(resource.toURI()).getParentFile().getParentFile().getParentFile().getAbsolutePath();
        dataNormalizationTool.importData(getGraphDb(), datasetDirTest);

        String baseDir = "./target/normalizer-test/";
        FileUtils.deleteQuietly(new File(baseDir));
        dataNormalizationTool.exportData(graphService, baseDir);
    }

    private static void importData(Class<? extends StudyImporter> importer, NodeFactoryNeo4j factory) throws StudyImporterException {
        StudyImporter studyImporter = createStudyImporter(importer, factory);
        LOG.info("[" + importer + "] importing ...");
        studyImporter.importStudy();
        LOG.info("[" + importer + "] imported.");
    }

    private static StudyImporter createStudyImporter(Class<? extends StudyImporter> studyImporter, NodeFactoryNeo4j factory) throws StudyImporterException {
        StudyImporter importer = new StudyImporterTestFactory(factory).instantiateImporter(studyImporter);
        importer.setLogger(new StudyImportLogger());
        return importer;
    }

}
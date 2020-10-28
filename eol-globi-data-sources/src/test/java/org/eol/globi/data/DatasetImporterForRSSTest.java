package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Interaction;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.AuthorIdResolver;
import org.eol.globi.util.DatasetImportUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.service.TermLookupService;
import org.junit.Test;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class DatasetImporterForRSSTest {

    @Test
    public void readRSS() throws StudyImporterException, IOException {
        final Dataset dataset = getDatasetGroup();


        List<Dataset> datasets = DatasetImporterForRSS.getDatasetsForFeed(dataset);
        assertThat(datasets.size(), is(3));
        assertThat(datasets.get(0).getOrDefault("hasDependencies", null), is("false"));

    }


    @Test
    public void titleIncludeExcludePattern() throws StudyImporterException, IOException {
        String configJson = "{ \"url\": \"classpath:/org/eol/globi/data/rss_vertnet.xml\", " +
                "\"include\": \".*(Arctos).*\", " +
                "\"exclude\": \".*GGBN.*\", " +
                "\"hasDependencies\": true }";
        final Dataset dataset = datasetFor(configJson);
        assertFalse(DatasetImporterForRSS.shouldIncludeTitleInDatasetCollection("bla (Arctos) GGBN", dataset));
        assertTrue(DatasetImporterForRSS.shouldIncludeTitleInDatasetCollection("bla (Arctos)", dataset));
        assertFalse(DatasetImporterForRSS.shouldIncludeTitleInDatasetCollection("bla", dataset));
    }

    @Test
    public void titleExcludePatternOnly() throws StudyImporterException, IOException {
        String configJson = "{ \"url\": \"classpath:/org/eol/globi/data/rss_vertnet.xml\", " +
                "\"exclude\": \".*GGBN.*\", " +
                "\"hasDependencies\": true }";
        final Dataset dataset = datasetFor(configJson);
        assertFalse(DatasetImporterForRSS.shouldIncludeTitleInDatasetCollection("bla (Arctos) GGBN", dataset));
        assertTrue(DatasetImporterForRSS.shouldIncludeTitleInDatasetCollection("bla (Arctos)", dataset));
        assertTrue(DatasetImporterForRSS.shouldIncludeTitleInDatasetCollection("bla", dataset));
    }

    @Test
    public void titleIncludePatternOnly() throws StudyImporterException, IOException {
        String configJson = "{ \"url\": \"classpath:/org/eol/globi/data/rss_vertnet.xml\", " +
                "\"include\": \".*(Arctos).*\", " +
                "\"hasDependencies\": true }";
        final Dataset dataset = datasetFor(configJson);
        assertTrue(DatasetImporterForRSS.shouldIncludeTitleInDatasetCollection("bla (Arctos) GGBN", dataset));
        assertTrue(DatasetImporterForRSS.shouldIncludeTitleInDatasetCollection("bla (Arctos)", dataset));
        assertFalse(DatasetImporterForRSS.shouldIncludeTitleInDatasetCollection("bla", dataset));
    }

    @Test
    public void readRSSVertnet() throws StudyImporterException, IOException {
        String configJson = "{ \"url\": \"classpath:/org/eol/globi/data/rss_vertnet.xml\", " +
                "\"include\": \".*(Arctos).*\", " +
                "\"exclude\": \".*GGBN.*\", " +
                "\"hasDependencies\": true }";
        final Dataset dataset = datasetFor(configJson);
        List<Dataset> datasets = DatasetImporterForRSS.getDatasetsForFeed(dataset);
        for (Dataset dataset1 : datasets) {
            assertThat(dataset1.getCitation(), containsString("(Arctos)"));
            assertThat(dataset1.getCitation(), not(containsString("GGBN")));
        }
        assertThat(datasets.size(), is(84));
        assertThat(datasets.get(0).getOrDefault("hasDependencies", null), is("true"));
    }

    @Test
    public void readRSSVertnetWithoutConfig() throws StudyImporterException, IOException {
        String configJson = "{ \"url\": \"classpath:/org/eol/globi/data/rss_vertnet.xml\" }";
        final Dataset dataset = datasetFor(configJson);
        List<Dataset> datasets = DatasetImporterForRSS.getDatasetsForFeed(dataset);
        assertThat(datasets.size(), is(263));
        assertThat(datasets.get(0).getOrDefault("hasDependencies", null), is("false"));
        assertThat(datasets.get(0).getOrDefault("url", null), is("http://ipt.vertnet.org:8080/ipt/archive.do?r=utep_mamm"));
        assertThat(datasets.get(0).getArchiveURI(), is(URI.create("http://ipt.vertnet.org:8080/ipt/archive.do?r=utep_mamm")));
    }

    @Test
    public void readFieldMuseum() throws StudyImporterException, IOException {
        String configJson = "{ \"url\": \"classpath:/org/eol/globi/data/rss_fieldmuseum.xml\" }";
        final Dataset dataset = datasetFor(configJson);
        List<Dataset> datasets = DatasetImporterForRSS.getDatasetsForFeed(dataset);
        assertThat(datasets.size(), is(14));
        assertThat(datasets.get(0).getOrDefault("hasDependencies", null), is("false"));
    }

    @Test
    public void embeddedDataset() throws IOException {
        Dataset embeddedDataset = DatasetImporterForRSS.embeddedDatasetFor(getDatasetGroup(), "some other citation", URI.create("http://example.com/archive.zip"));
        assertThat(embeddedDataset.getCitation(), is("some other citation"));
        assertThat(embeddedDataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "foo"), is("foo"));
        assertThat(embeddedDataset.getArchiveURI().toString(), is("http://example.com/archive.zip"));
    }

    @Test
    public void embeddedDatasetWithConfig() throws IOException {
        Dataset embeddedDataset = DatasetImporterForRSS.embeddedDatasetFor(getDatasetGroupWithProperty(), "some other citation", URI.create("http://example.com/archive.zip"));
        assertThat(embeddedDataset.getCitation(), is("some other citation"));
        assertThat(embeddedDataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "true"), is("false"));
        assertThat(embeddedDataset.getArchiveURI().toString(), is("http://example.com/archive.zip"));
    }

    @Test
    public void indexingInteractionListener() throws StudyImporterException {

        TreeMap<String, Map<String, String>> index = new TreeMap<>();
        DatasetImportUtil.IndexingInteractionListener indexingInteractionListener
                = new DatasetImportUtil.IndexingInteractionListener(index);

        indexingInteractionListener.newLink(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "http://arctos.database.museum/guid/MVZ:Bird:180448?seid=587053");
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "http://arctos.database.museum/guid/1234");
        }});

        assertTrue(index.containsKey("http://arctos.database.museum/guid/MVZ:Bird:180448"));
    }

    @Test
    public void indexingInteractionListenerDBMaker() throws StudyImporterException {

        final Map<String, Map<String, String>> index = DBMaker.newTempTreeMap();


        DatasetImportUtil.IndexingInteractionListener indexingInteractionListener
                = new DatasetImportUtil.IndexingInteractionListener(index);

        indexingInteractionListener.newLink(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "http://arctos.database.museum/guid/MVZ:Bird:180448?seid=587053");
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "http://arctos.database.museum/guid/1234");
        }});

        assertTrue(index.containsKey("http://arctos.database.museum/guid/MVZ:Bird:180448"));
    }

    @Test
    public void enrichingInteractionListener() throws StudyImporterException {
        DatasetImporterWithListener studyImporter = new DatasetImporterWithListener(new ParserFactory() {
            @Override
            public LabeledCSVParser createParser(URI studyResource, String characterEncoding) throws IOException {
                return null;
            }
        }, new NodeFactory() {
            @Override
            public Location findLocation(Location location) throws NodeFactoryException {
                return null;
            }

            @Override
            public Season createSeason(String seasonNameLower) {
                return null;
            }

            @Override
            public Specimen createSpecimen(Interaction interaction, Taxon taxon) throws NodeFactoryException {
                return null;
            }

            @Override
            public Specimen createSpecimen(Study study, Taxon taxon) throws NodeFactoryException {
                return null;
            }

            @Override
            public Specimen createSpecimen(Study study, Taxon taxon, RelTypes... types) throws NodeFactoryException {
                return null;
            }

            @Override
            public Study createStudy(Study study) {
                return null;
            }

            @Override
            public Study getOrCreateStudy(Study study) throws NodeFactoryException {
                return null;
            }

            @Override
            public Study findStudy(String title) {
                return null;
            }

            @Override
            public Season findSeason(String seasonName) {
                return null;
            }

            @Override
            public Location getOrCreateLocation(Location location) throws NodeFactoryException {
                return null;
            }

            @Override
            public void setUnixEpochProperty(Specimen specimen, Date date) throws NodeFactoryException {

            }

            @Override
            public Date getUnixEpochProperty(Specimen specimen) throws NodeFactoryException {
                return null;
            }

            @Override
            public List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException {
                return null;
            }

            @Override
            public List<Environment> addEnvironmentToLocation(Location location, List<Term> terms) {
                return null;
            }

            @Override
            public Term getOrCreateBodyPart(String externalId, String name) throws NodeFactoryException {
                return null;
            }

            @Override
            public Term getOrCreatePhysiologicalState(String externalId, String name) throws NodeFactoryException {
                return null;
            }

            @Override
            public Term getOrCreateLifeStage(String externalId, String name) throws NodeFactoryException {
                return null;
            }

            @Override
            public TermLookupService getTermLookupService() {
                return null;
            }

            @Override
            public AuthorIdResolver getAuthorResolver() {
                return null;
            }

            @Override
            public Term getOrCreateBasisOfRecord(String externalId, String name) throws NodeFactoryException {
                return null;
            }

            @Override
            public Dataset getOrCreateDataset(Dataset dataset) {
                return null;
            }

            @Override
            public Interaction createInteraction(Study study) throws NodeFactoryException {
                return null;
            }
        }) {
            @Override
            public void importStudy() throws StudyImporterException {
                //
            }
        };


        final List<Map<String, String>> receivedLinks = new ArrayList<>();
        studyImporter.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                receivedLinks.add(link);
            }
        });
        TreeMap<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<String, Map<String, String>>() {{
            put("1234", new TreeMap<String, String>() {
                {
                    put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "1234");
                    put(DatasetImporterForTSV.SOURCE_LIFE_STAGE_NAME, "lifeStageName");
                    put(DatasetImporterForTSV.SOURCE_LIFE_STAGE_ID, "lifeStageId");
                    put(DatasetImporterForTSV.SOURCE_BODY_PART_NAME, "bodyPartName");
                    put(DatasetImporterForTSV.SOURCE_BODY_PART_ID, "bodyPartId");
                    put(TaxonUtil.SOURCE_TAXON_NAME, "taxonName");
                    put(TaxonUtil.SOURCE_TAXON_ID, "taxonId");
                }
            });
        }};
        DatasetImportUtil.EnrichingInteractionListener listener = new DatasetImportUtil.EnrichingInteractionListener(interactionsWithUnresolvedOccurrenceIds, studyImporter.getInteractionListener());

        listener.newLink(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "1234");
        }});

        assertThat(receivedLinks.size(), is(1));
        Map<String, String> received = receivedLinks.get(0);
        assertThat(received.get(TaxonUtil.TARGET_TAXON_NAME), is("taxonName"));
        assertThat(received.get(TaxonUtil.TARGET_TAXON_ID), is("taxonId"));
        assertThat(received.get(DatasetImporterForTSV.TARGET_BODY_PART_NAME), is("bodyPartName"));
        assertThat(received.get(DatasetImporterForTSV.TARGET_BODY_PART_ID), is("bodyPartId"));
        assertThat(received.get(DatasetImporterForTSV.TARGET_LIFE_STAGE_NAME), is("lifeStageName"));
        assertThat(received.get(DatasetImporterForTSV.TARGET_LIFE_STAGE_ID), is("lifeStageId"));


    }

    private DatasetImpl getDatasetGroup() throws IOException {
        String configJson = "{ \"url\": \"classpath:/org/eol/globi/data/rss_amnh.xml\" }";
        return datasetFor(configJson);
    }

    private DatasetImpl getDatasetGroupWithProperty() throws IOException {
        String configJson = "{ \"" + DatasetConstant.SHOULD_RESOLVE_REFERENCES + "\": false, \"resources\": { \"rss\": \"http://amnh.begoniasociety.org/dwc/rss.xml\" } }";
        return datasetFor(configJson);
    }

    @Test
    public void useUrl() throws IOException {
        DatasetImpl dataset = datasetFor("{ \"url\": \"bar\", " +
                "\"resources\": { \"rss\": \"foo\" } " +
                "}");
        assertThat(DatasetImporterForRSS.getRSSEndpoint(dataset), is("bar"));
    }

    private DatasetImpl datasetFor(String configJson) throws IOException {
        JsonNode config = new ObjectMapper().readTree(configJson);
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"), inStream -> inStream);
        dataset.setConfig(config);
        return dataset;
    }

    @Test
    public void useRssNamedProperty() throws IOException {
        DatasetImpl dataset = datasetFor("{ \"url\": \"bar\", " +
                "\"resources\": { \"rss\": \"foo\" } " +
                "}");
        assertThat(DatasetImporterForRSS.getRSSEndpoint(dataset), is("bar"));
    }

    @Test
    public void useUrlOnly() throws IOException {
        DatasetImpl dataset = datasetFor("{ \"url\": \"bar\" }");
        assertThat(DatasetImporterForRSS.getRSSEndpoint(dataset), is("bar"));
    }

}
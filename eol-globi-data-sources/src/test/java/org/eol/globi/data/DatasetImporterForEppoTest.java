package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;

public class DatasetImporterForEppoTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void importDataset() throws StudyImporterException {
        List<Map<String, String>> foundInteractions = new ArrayList<>();
        DatasetImporterForEppo datasetImporterForEppo = new DatasetImporterForEppo(null, null);
        datasetImporterForEppo.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                foundInteractions.add(interaction);
            }
        });
        DatasetImpl dataset = new DatasetImpl("foo/bar", new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return getClass().getResourceAsStream("eppo/eppo-light.json");
            }
        }, URI.create("some:uri"));
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("url", "example.json");
        dataset.setConfig(objectNode);

        datasetImporterForEppo.setDataset(dataset);

        datasetImporterForEppo.importStudy();

        assertThat(foundInteractions.size(), is(1462));

        Map<String, String> first = foundInteractions.get(0);
        assertThat(first.get(TaxonUtil.SOURCE_TAXON_ID), is("EPPO:CUNNSP"));
        assertThat(first.get(TaxonUtil.SOURCE_TAXON_NAME), is("Cunninghamella sp."));
        assertThat(first.get(TaxonUtil.SOURCE_TAXON_PATH), is("Fungi | Zygomycota | Mucoromycotina | Mucorales | Cunninghamellaceae | Cunninghamella | Cunninghamella sp."));
        assertThat(first.get(INTERACTION_TYPE_NAME), is("Host"));
        assertThat(first.get(INTERACTION_TYPE_ID), is("9"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_ID), is("EPPO:EUWAWH"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_NAME), is("Euwallacea fornicatus sensu stricto"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_PATH), is("Animalia | Arthropoda | Hexapoda | Insecta | Coleoptera | Curculionidae | Scolytinae | Euwallacea | Euwallacea fornicatus sensu stricto"));
        assertThat(first.get(REFERENCE_CITATION), is("Smith SM, Gomez DF, Beaver RA, Hulcr J, Cognato AI (2019) Reassessment of the species in the Euwallacea fornicatus (Coleoptera: Curculionidae: Scolytinae) complex after the rediscovery of the ‘lost’ type specimen. Insects 10, 261. https://doi.org/10.3390/insects10090261"));
        assertThat(first.get(REFERENCE_ID), is("foo/barSmith SM, Gomez DF, Beaver RA, Hulcr J, Cognato AI (2019) Reassessment of the species in the Euwallacea fornicatus (Coleoptera: Curculionidae: Scolytinae) complex after the rediscovery of the ‘lost’ type specimen. Insects 10, 261. https://doi.org/10.3390/insects10090261"));
        assertThat(first.get(REFERENCE_URL), is("https://gd.eppo.int/taxon/CUNNSP/pests"));

        Map<String, String> last = foundInteractions.get(foundInteractions.size() - 1);
        assertThat(last.get(TaxonUtil.SOURCE_TAXON_NAME), is(nullValue()));
        assertThat(last.get(TaxonUtil.SOURCE_TAXON_ID), is("EPPO:MYNLA"));
        assertThat(last.get(TaxonUtil.SOURCE_TAXON_PATH), is(nullValue()));
        assertThat(last.get(INTERACTION_TYPE_NAME), is("Host"));
        assertThat(last.get(INTERACTION_TYPE_ID), is("9"));
        assertThat(last.get(TaxonUtil.TARGET_TAXON_ID), is("EPPO:EUWAWH"));
        assertThat(last.get(TaxonUtil.TARGET_TAXON_NAME), is("Euwallacea fornicatus sensu stricto"));
        assertThat(last.get(TaxonUtil.TARGET_TAXON_PATH), is("Animalia | Arthropoda | Hexapoda | Insecta | Coleoptera | Curculionidae | Scolytinae | Euwallacea | Euwallacea fornicatus sensu stricto"));
    }

    @Test
    public void parsePests() throws IOException, StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        };
        final InputStream inputStream = DatasetImporterForEppoTest.class.getResourceAsStream("eppo/pest.json");
        final JsonNode record = new ObjectMapper().readTree(inputStream);

        DatasetImporterForEppo.parseInteractionClaim(
                createDataset(),
                record,
                listener
        );

        assertThat(received.size(), is(1));
        Map<String, String> first = received.get(0);
        assertThat(first.get(TaxonUtil.SOURCE_TAXON_ID), is("EPPO:CUNNSP"));
        assertThat(first.get(INTERACTION_TYPE_ID), is("9"));
        assertThat(first.get(INTERACTION_TYPE_NAME), is("Host"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_ID), is("EPPO:EUWAWH"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_NAME), is("Euwallacea fornicatus sensu stricto"));
        assertThat(first.get(DatasetImporterForTSV.REFERENCE_CITATION), is("Smith SM, Gomez DF, Beaver RA, Hulcr J, Cognato AI (2019) Reassessment of the species in the Euwallacea fornicatus (Coleoptera: Curculionidae: Scolytinae) complex after the rediscovery of the ‘lost’ type specimen. Insects 10, 261. https://doi.org/10.3390/insects10090261"));
    }

    private static DatasetImpl createDataset() {
        return new DatasetImpl("foo/bar", null, null);
    }

    @Test
    public void parseTaxonomy() throws IOException, StudyImporterException {
        List<Taxon> taxa = new ArrayList<>();

        Consumer<Taxon> listener = new Consumer<Taxon>() {
            @Override
            public void accept(Taxon taxon) {
                taxa.add(taxon);
            }
        };
        final InputStream inputStream = DatasetImporterForEppoTest.class.getResourceAsStream("eppo/taxonomy.json");
        assertNotNull(inputStream);
        DatasetImporterForEppo.parseTaxonomyOnly(inputStream, listener);
        assertThat(taxa.size(), is(2));

        Taxon first = taxa.get(0);
        assertThat(first.getName(), is("Lichtheimia corymbifera"));
        assertThat(first.getRank(), is("species"));
        assertThat(first.getExternalId(), is("EPPO:ABSICO"));
        assertThat(first.getPath(), is("Fungi | Zygomycota | Mucoromycotina | Mucorales | Lichtheimiaceae | Lichtheimia | Lichtheimia corymbifera"));
        assertThat(first.getPathIds(), is("EPPO:1FUNGK | EPPO:1ZYGOP | EPPO:1MUCOC | EPPO:1MUCOO | EPPO:1LICTF | EPPO:1LICTG | EPPO:ABSICO"));
        assertThat(first.getPathNames(), is("kingdom | phylum | class | order | family | genus | species"));

        Taxon second = taxa.get(1);
        assertThat(second.getName(), is("Lichtheimia ramosa"));
        assertThat(second.getRank(), is("species"));
        assertThat(second.getExternalId(), is("EPPO:ABSIRA"));
        assertThat(second.getPath(), is("Fungi | Zygomycota | Mucoromycotina | Mucorales | Lichtheimiaceae | Lichtheimia | Lichtheimia ramosa"));
        assertThat(second.getPathIds(), is("EPPO:1FUNGK | EPPO:1ZYGOP | EPPO:1MUCOC | EPPO:1MUCOO | EPPO:1LICTF | EPPO:1LICTG | EPPO:ABSIRA"));
        assertThat(second.getPathNames(), is("kingdom | phylum | class | order | family | genus | species"));
    }

    @Test
    public void parseBiologicalControlAgent() throws IOException, StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        };
        final InputStream inputStream = DatasetImporterForEppo
                .class.getResourceAsStream("eppo/bca.json");
        final JsonNode record = new ObjectMapper().readTree(inputStream);

        DatasetImporterForEppo.parseInteractionClaim(createDataset(), record, listener);

        assertThat(received.size(), is(1));
        Map<String, String> first = received.get(0);
        assertThat(first.get(TaxonUtil.SOURCE_TAXON_ID), is("EPPO:ABAGAL"));
        assertThat(first.get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002627"));
        assertThat(first.get(INTERACTION_TYPE_NAME), is("killedBy"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_ID), is("EPPO:COTEMA"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_NAME), is("Cotesia marginiventris (as Noctuidae)"));
        assertThat(first.get(DatasetImporterForTSV.REFERENCE_CITATION), is("EPPO (online) Appendix 1 - Commercially or officially used biological control agents. EPPO Standard PM 6/3 (5) Biological control agents safely used in the EPPO region. https://gd.eppo.int/standards/PM6/"));
    }

    @Test
    public void parseVector() throws IOException, StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        };
        final InputStream inputStream = DatasetImporterForEppo
                .class.getResourceAsStream("eppo/vector.json");
        final JsonNode record = new ObjectMapper().readTree(inputStream);

        DatasetImporterForEppo.parseInteractionClaim(createDataset(), record, listener);

        assertThat(received.size(), is(1));
        Map<String, String> first = received.get(0);
        assertThat(first.get(TaxonUtil.SOURCE_TAXON_ID), is("EPPO:CERAFA"));
        assertThat(first.get(INTERACTION_TYPE_ID), is("P"));
        assertThat(first.get(INTERACTION_TYPE_NAME), is("Potential vector"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_ID), is("EPPO:ARRHMI"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_NAME), is("Arrhenodes minutus"));
        assertThat(first.get(DatasetImporterForTSV.REFERENCE_CITATION), is("Buchanan WD (1957) Brentids may be vectors of the oak wilt disease. Plant Disease Reporter 41, 707-708."));
    }

    @Test
    public void parsePestsRecordWithMultipleReferences() throws IOException, StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        };
        final InputStream inputStream = DatasetImporterForEppoTest.class.getResourceAsStream("eppo/pest-multiple-refs.json");
        final JsonNode record = new ObjectMapper().readTree(inputStream);

        DatasetImporterForEppo.parseInteractionClaim(createDataset(), record, listener);

        assertThat(received.size(), is(2));
        Map<String, String> first = received.get(0);
        assertThat(first.get(TaxonUtil.SOURCE_TAXON_ID), is("EPPO:CEYDE"));
        assertThat(first.get(INTERACTION_TYPE_ID), is("6"));
        assertThat(first.get(INTERACTION_TYPE_NAME), is("Wild/Weed"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_ID), is("EPPO:POMAIN"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_NAME), is("Pomacea maculata"));
        assertThat(first.get(DatasetImporterForTSV.REFERENCE_CITATION), is("Baker P, Zimmanck F, Baker SM (2010) Feeding rates of an introduced freshwater gastropod Pomacea insularum on native and nonindigenous aquatic plants in Florida. Journal of Molluscan Studies 76(2), 138-143."));

        Map<String, String> second = received.get(1);
        assertThat(second.get(DatasetImporterForTSV.REFERENCE_CITATION), is("Burlakova LE, Karatayev AY, Padilla DK, Cartwright LD, Hollas DN (2009) Wetland restoration  and invasive species: Apple snail (Pomacea insularum) feeding on native and invasive aquatic plants. Restoration Ecology 17, 433-440."));
    }


}
package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.tool.NullImportLogger;
import org.eol.globi.util.HttpUtil;
import org.eol.globi.util.InteractTypeMapper;
import org.eol.globi.util.InteractTypeMapperFactoryImpl;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class StudyImporterForPensoftIT {

    @Test
    public void importStudy() throws StudyImporterException, URISyntaxException {
        final StudyImporterForPensoft importer = new StudyImporterForPensoft(new ParserFactoryLocal(), null);
        final Dataset dataset = new DatasetImpl("some/name", URI.create("some:uri"), in -> in);
        final ObjectNode objectNode = new ObjectMapper().createObjectNode();
        final URL resource = getClass().getResource("pensoft/annotated-tables-first-two.json");
        objectNode.put("url", resource.toURI().toString());
        objectNode.put("citation", "some dataset citation");
        dataset.setConfig(objectNode);
        List<Map<String, String>> links = new ArrayList<>();

        importer.setDataset(dataset);
        importer.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                links.add(link);
            }
        });
        importer.importStudy();

        assertThat(links.size(), is(146));

        assertThat(links.get(0), hasEntry("Family Name", "Acanthaceae"));
        assertThat(links.get(0), hasEntry("Family Name_taxon_name", "Acanthaceae"));
        assertThat(links.get(0), hasEntry("Family Name_taxon_path", "Plantae | Tracheophyta | Magnoliopsida | Lamiales | Acanthaceae"));
        assertThat(links.get(0), hasEntry("Family Name_taxon_pathNames", "kingdom | phylum | class | order | family | genus"));
        assertThat(links.get(0), hasEntry("referenceUrl", "http://openbiodiv.net/FB706B4E-BAC2-4432-AD28-48063E7753E4"));
        assertThat(links.get(0), hasEntry("referenceDoi", "10.3897/zookeys.306.5455"));
        assertThat(links.get(0), hasEntry("referenceCitation", "Identification of the terebrantian thrips (Insecta, Thysanoptera) associated with cultivated plants in Java, Indonesia. http://openbiodiv.net/D37E8D1A-221B-FFA6-FFE7-4458FFA0FFC2. 10.3897/zookeys.306.5455"));
    }
}
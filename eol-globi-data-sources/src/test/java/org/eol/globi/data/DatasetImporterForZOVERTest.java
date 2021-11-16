package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;

public class DatasetImporterForZOVERTest {

    @Test
    public void getLeafIds() throws IOException {
        InputStream resource = getClass().getResourceAsStream("zover/tick.json");
        JsonNode jsonNode = new ObjectMapper().readTree(resource);

        List<Long> ids = new ArrayList<>();
        DatasetImporterForZOVER.getLeafIds(jsonNode, ids::add);

        assertThat(ids.size(), is(54));
        assertThat(ids, hasItem(1941235L));
        assertThat(ids, hasItem(39718L));
    }


    @Test
    public void importDataset() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();
        DatasetImporterForZOVER importer = new DatasetImporterForZOVER(null, null);

        importer.setDatabases(Collections.singletonList(DatasetImporterForZOVER.ZOVER_CHIROPTERA));
        importer.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        });

        DatasetImpl dataset = new DatasetImpl("some/namespace", new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {

                InputStream is = null;
                if (StringUtils.equals("http://www.mgc.ac.cn/cgi-bin/ZOVER/lineage2json1.pl?type=viruses&host=chiroptera", resourceName.toString())) {
                    is = getClass().getResourceAsStream("zover/chiroptera_chopped.json");
                } else if (StringUtils.equals("http://www.mgc.ac.cn/ZOVER/json/chiroptera_viruses_363628.json", resourceName.toString())) {
                    is = getClass().getResourceAsStream("zover/chiroptera_viruses_363628.json");
                }
                return is;
            }
        }, URI.create("some:uri")) {

        };
        importer.setDataset(dataset);

        importer.importStudy();

        assertThat(interactions.size(), is(1));

        Map<String, String> interaction = interactions.get(0);

        assertThat(interaction.get(SOURCE_TAXON_NAME), is("Torque teno Tadarida brasiliensis virus"));
        assertThat(interaction.get(SOURCE_TAXON_ID), is("NCBI:1543419"));
        assertThat(interaction.get(TARGET_TAXON_NAME), is("Tadarida brasiliensis"));
        assertThat(interaction.get(TARGET_TAXON_ID), is("NCBI:9438"));
        assertThat(interaction.get(INTERACTION_TYPE_NAME), is("pathogenOf"));
        assertThat(interaction.get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002556"));
        assertThat(interaction.get(REFERENCE_CITATION), is(nullValue()));
        assertThat(interaction.get(REFERENCE_URL), is("https://www.ncbi.nlm.nih.gov/nuccore/KM434181"));
        assertThat(interaction.get(REFERENCE_DOI), is(nullValue()));
        assertThat(interaction.get(REFERENCE_ID), is("urn:lsid:cn.ac.mgc:tick:363628"));

    }

    @Test
    public void parseVirusHostRecord() throws IOException, StudyImporterException {

        String hostLabel = "tick";
        long virusId = 35237L;
        String virusData = DatasetImporterForZOVER.getVirusData(hostLabel, virusId, new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return getClass().getResourceAsStream("zover/tick_viruses_35237.json");
            }
        }, "http://www.mgc.ac.cn");

        List<Map<String, String>> interactions = new ArrayList<>();

        InteractionListener listener = new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        };


        JsonNode jsonNode = new ObjectMapper().readTree(virusData);
        DatasetImporterForZOVER.parseData(hostLabel, listener, jsonNode, virusId);

        assertThat(interactions.size(), is(281));

        Map<String, String> first = interactions.get(0);
        assertThat(first.get(SOURCE_TAXON_NAME), is("African swine fever virus"));
        assertThat(first.get(SOURCE_TAXON_ID), is("NCBI:10497"));
        assertThat(first.get(SOURCE_TAXON_PATH), is("Asfarviridae | African swine fever virus"));
        assertThat(first.get(INTERACTION_TYPE_NAME), is("pathogenOf"));
        assertThat(first.get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002556"));
        assertThat(first.get(TARGET_TAXON_NAME), is("Ornithodoros porcinus"));
        assertThat(first.get(TARGET_TAXON_ID), is("NCBI:34594"));
        assertThat(first.get(TARGET_TAXON_PATH), is("Ornithodoros | Ornithodoros porcinus"));
        assertThat(first.get(LOCALITY_NAME), is("South Africa"));
        assertThat(first.get(REFERENCE_URL), is("https://www.ncbi.nlm.nih.gov/nuccore/GQ867183"));
        assertThat(first.get(REFERENCE_ID), is("urn:lsid:cn.ac.mgc:tick:35237"));
    }

}
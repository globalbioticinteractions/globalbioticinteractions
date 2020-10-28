package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.STUDY_SOURCE_CITATION;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class DatasetImporterForGrayTest extends GraphDBTestCase {

    static DatasetImporterForGray createImporter(NodeFactory nodeFactory) throws IOException {
        DatasetImporterForGray gray = new DatasetImporterForGray(new ParserFactoryLocal(), nodeFactory);

        JsonNode config = new ObjectMapper().readTree("{ \"citation\": \"Gray C, Ma A, Perkins D, Hudson L, Figueroa D, Woodward G (2015). Database of trophic interactions. Zenodo. https://doi.org/10.5281/zenodo.13751\",\n" +
                "  \"doi\": \"https://doi.org/10.5281/zenodo.13751\",\n" +
                "  \"format\": \"gray\",\n" +
                "  \"resources\": {\n" +
                "    \"links\": \"https://zenodo.org/record/13751/files/trophic.links.2014-11-10.csv\"  \n" +
                "  }\n" +
                "}");
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"), inStream -> inStream);
        dataset.setConfig(config);
        gray.setDataset(dataset);
        return gray;
    }

    @Test
    public void importLines() throws IOException, StudyImporterException {
        DatasetImporterForGray gray = createImporter(nodeFactory);
        final List<Map<String, String>> maps = new ArrayList<Map<String, String>>();

        gray.importLinks(IOUtils.toInputStream(firstFewLines(), StandardCharsets.UTF_8), new InteractionListener() {

            @Override
            public void newLink(final Map<String, String> link) {
                maps.add(link);
            }
        }, null);
        resolveNames();
        assertThat(maps.size(), is(4));
        Map<String, String> firstLink = maps.get(0);
        assertThat(firstLink.get(TaxonUtil.SOURCE_TAXON_NAME), is("Ancylus fluviatilis"));
        assertThat(firstLink.get(DatasetImporterForTSV.SOURCE_LIFE_STAGE_NAME), is(nullValue()));
        assertThat(firstLink.get(TaxonUtil.TARGET_TAXON_NAME), is("FPOM"));
        assertThat(firstLink.get(DatasetImporterForTSV.TARGET_LIFE_STAGE_NAME), is(nullValue()));
        assertThat(firstLink.get(DatasetImporterForTSV.REFERENCE_CITATION), is("Ledger, M.E., Brown, L.E., Edwards, F., Milner, A.M. & Woodward, G. (2012) Drought alters the structure and functioning of complex food webs. Nature Climate Change."));
        assertThat(firstLink.get(DatasetImporterForTSV.REFERENCE_ID), is("10.5281/zenodo.13751/source.id/50"));
        assertStaticInfo(firstLink);

        Map<String, String> secondLink = maps.get(1);
        assertThat(secondLink.get(TaxonUtil.SOURCE_TAXON_NAME), is("Ancylus fluviatilis"));
        assertThat(secondLink.get(TaxonUtil.TARGET_TAXON_NAME), is("CPOM"));
        assertThat(secondLink.get(DatasetImporterForTSV.BASIS_OF_RECORD_NAME), is("observed"));
        assertStaticInfo(secondLink);
    }

    private void assertStaticInfo(Map<String, String> firstLink) {
        assertThat(firstLink.get(STUDY_SOURCE_CITATION), containsString("Gray C, Ma A, Perkins D, Hudson L, Figueroa D, Woodward G (2015). Database of trophic interactions. Zenodo. https://doi.org/10.5281/zenodo.13751."));
        assertThat(firstLink.get(STUDY_SOURCE_CITATION), containsString(" Accessed at <http://example.com>"));
        assertThat(firstLink.get(REFERENCE_CITATION), containsString("Ledger"));
        assertThat(firstLink.get(INTERACTION_TYPE_ID), is("RO:0002470"));
        assertThat(firstLink.get(INTERACTION_TYPE_NAME), is("eats"));
    }

    private String firstFewLines() {
        return "link.id,resource,resource.lifestage,consumer,consumer.lifestage,link.evidence,source.id,full.source,res.genus,res.subfamily,res.family,res.order,res.class,res.category,con.genus,con.subfamily,con.family,con.order,con.class,con.category\n" +
                "7047,FPOM,NA,Ancylus fluviatilis,NA,observed,50,\"Ledger, M.E., Brown, L.E., Edwards, F., Milner, A.M. & Woodward, G. (2012) Drought alters the structure and functioning of complex food webs. Nature Climate Change.\",NA,NA,NA,NA,NA,NA,Ancylus,NA,Ancylidae,Basommatophora,Gastropoda,invertebrate\n" +
                "7048,CPOM,NA,Ancylus fluviatilis,NA,observed,50,\"Ledger, M.E., Brown, L.E., Edwards, F., Milner, A.M. & Woodward, G. (2012) Drought alters the structure and functioning of complex food webs. Nature Climate Change.\",NA,NA,NA,NA,NA,NA,Ancylus,NA,Ancylidae,Basommatophora,Gastropoda,invertebrate\n" +
                "7049,Navicula gregaria,NA,Ancylus fluviatilis,NA,observed,50,\"Ledger, M.E., Brown, L.E., Edwards, F., Milner, A.M. & Woodward, G. (2012) Drought alters the structure and functioning of complex food webs. Nature Climate Change.\",Navicula,NA,Naviculaceae,Naviculales,Bacillariophyceae,producer,Ancylus,NA,Ancylidae,Basommatophora,Gastropoda,invertebrate\n" +
                "7050,Navicula tripunctata,NA,Ancylus fluviatilis,NA,observed,50,\"Ledger, M.E., Brown, L.E., Edwards, F., Milner, A.M. & Woodward, G. (2012) Drought alters the structure and functioning of complex food webs. Nature Climate Change.\",Navicula,NA,Naviculaceae,Naviculales,Bacillariophyceae,producer,Ancylus,NA,Ancylidae,Basommatophora,Gastropoda,invertebrate";
    }
}
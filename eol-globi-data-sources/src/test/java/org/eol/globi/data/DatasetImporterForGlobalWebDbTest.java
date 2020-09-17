package org.eol.globi.data;

import org.apache.commons.collections4.list.TreeList;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DatasetImporterForGlobalWebDbTest {

    private String aDietMatrix = "\"B.A. Menge and J.P. Sutherland. Species Diversity Gradients: Synthesis of the Roles of Predation, Competition, and Temporal Heterogeneity. The American Naturalist, 110(973), pp. 351-369.\",,,\r\n" +
            "Exposed rocky shore - New England - U.S.A.,Balanus balanoides,Mytilus edulis,Thais lapillus\r\n" +
            "detritus,1,1,0\r\n" +
            "plankton,1,1,0\r\n" +
            "Balanus balanoides,0,0,1\r\n" +
            "Mytilus edulis,0,0,1\r\n";

    @Test
    public void dietMatrix() throws StudyImporterException {
        List<String> matrices = new ArrayList<>();
        DatasetImporterForGlobalWebDb importer = new DatasetImporterForGlobalWebDb(null, null);
        importer.setDataset(new DatasetLocal(inStream -> inStream));
        importer
                .importDietMatrices(URI.create("classpath:/org/eol/globi/data/globalwebdb-test.zip"), matrices::add);
        assertThat(matrices.size(), is(10));
        assertThat(matrices.get(0), is(aDietMatrix));
    }

    @Test
    public void dietMatrices() throws StudyImporterException {
        final AtomicInteger count = new AtomicInteger();
        DatasetImporterForGlobalWebDb importer = new DatasetImporterForGlobalWebDb(null, null);
        importer.setDataset(new DatasetLocal(inStream -> inStream));
        importer
                .importDietMatrices(URI.create("classpath:/org/eol/globi/data/globalwebdb-test.zip"), matrixString -> {
                    try {
                        DatasetImporterForGlobalWebDb.parseDietMatrix(new InteractionListener() {
                            @Override
                            public void newLink(Map<String, String> link) throws StudyImporterException {
                                count.incrementAndGet();
                            }
                        }, matrixString, "a source citation");
                    } catch (IOException e) {
                        throw new StudyImporterException(e);
                    }
                });
        assertThat(count.get(), is(207));
    }

    @Test
    public void parseMatrix() throws IOException, StudyImporterException {
        List<Map<String, String>> interactions = new TreeList<>();
        InteractionListener listener = interactions::add;

        DatasetImporterForGlobalWebDb.parseDietMatrix(listener, aDietMatrix, "some source citation");

        assertThat(interactions.size(), is(6));

        Map<String, String> first = interactions.get(0);
        assertThat(first.get(TaxonUtil.SOURCE_TAXON_NAME), is("Balanus balanoides"));
        assertThat(first.get(TaxonUtil.TARGET_TAXON_NAME), is("detritus"));
        assertThat(first.get(DatasetImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(first.get(DatasetImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(first.get(DatasetImporterForTSV.HABITAT_NAME), is("Exposed rocky shore"));
        assertThat(first.get(DatasetImporterForTSV.LOCALITY_NAME), is("New England, U.S.A."));
        assertThat(first.get(DatasetImporterForTSV.REFERENCE_ID), is("df06df18abafa63a6f0473d6d0e6ce68"));
        assertThat(first.get(DatasetImporterForTSV.REFERENCE_CITATION), is("B.A. Menge and J.P. Sutherland. Species Diversity Gradients: Synthesis of the Roles of Predation, Competition, and Temporal Heterogeneity. The American Naturalist, 110(973), pp. 351-369."));
        assertThat(first.get(DatasetImporterForTSV.STUDY_SOURCE_CITATION), is("some source citation"));

        Map<String, String> last = interactions.get(5);
        assertThat(last.get(TaxonUtil.SOURCE_TAXON_NAME), is("Thais lapillus"));
        assertThat(last.get(TaxonUtil.TARGET_TAXON_NAME), is("Mytilus edulis"));
        assertThat(last.get(DatasetImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(last.get(DatasetImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(last.get(DatasetImporterForTSV.HABITAT_NAME), is("Exposed rocky shore"));
        assertThat(last.get(DatasetImporterForTSV.LOCALITY_NAME), is("New England, U.S.A."));
        assertThat(last.get(DatasetImporterForTSV.REFERENCE_ID), is("df06df18abafa63a6f0473d6d0e6ce68"));
        assertThat(last.get(DatasetImporterForTSV.REFERENCE_CITATION), is("B.A. Menge and J.P. Sutherland. Species Diversity Gradients: Synthesis of the Roles of Predation, Competition, and Temporal Heterogeneity. The American Naturalist, 110(973), pp. 351-369."));
        assertThat(last.get(DatasetImporterForTSV.STUDY_SOURCE_CITATION), is("some source citation"));
    }

}
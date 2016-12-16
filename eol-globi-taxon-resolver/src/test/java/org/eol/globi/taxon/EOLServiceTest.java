package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.TaxonomyProvider;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EOLServiceTest {

    @Test
    public void parseRank() throws IOException {
        String response = "{\"sourceIdentifier\":\"6845885\",\"taxonID\":52191458,\"parentNameUsageID\":52191457,\"taxonConceptID\":1045608,\"scientificName\":\"Apis mellifera Linnaeus 1758\",\"taxonRank\":\"Species\",\"source\":\"http://eol.org/pages/1045608/hierarchy_entries/52191458/overview\",\"nameAccordingTo\":[\"Species 2000 & ITIS Catalogue of Life: April 2013\",\"ITIS Bees: World Bee Checklist by Ruggiero M. (project leader), Ascher J. et al.\"],\"vernacularNames\":[{\"vernacularName\":\"Honey bee\",\"language\":\"en\"},{\"vernacularName\":\"Abeille domestique\",\"language\":\"fr\"}],\"synonyms\":[],\"ancestors\":[{\"sourceIdentifier\":\"13021388\",\"taxonID\":51521761,\"parentNameUsageID\":0,\"taxonConceptID\":1,\"scientificName\":\"Animalia\",\"taxonRank\":\"kingdom\",\"source\":\"http://eol.org/pages/1/hierarchy_entries/51521761/overview\"},{\"sourceIdentifier\":\"13021389\",\"taxonID\":51521762,\"parentNameUsageID\":51521761,\"taxonConceptID\":164,\"scientificName\":\"Arthropoda\",\"taxonRank\":\"phylum\",\"source\":\"http://eol.org/pages/164/hierarchy_entries/51521762/overview\"},{\"sourceIdentifier\":\"13021596\",\"taxonID\":51635639,\"parentNameUsageID\":51521762,\"taxonConceptID\":344,\"scientificName\":\"Insecta\",\"taxonRank\":\"class\",\"source\":\"http://eol.org/pages/344/hierarchy_entries/51635639/overview\"},{\"sourceIdentifier\":\"13021807\",\"taxonID\":52130142,\"parentNameUsageID\":51635639,\"taxonConceptID\":648,\"scientificName\":\"Hymenoptera\",\"taxonRank\":\"order\",\"source\":\"http://eol.org/pages/648/hierarchy_entries/52130142/overview\"},{\"sourceIdentifier\":\"13025307\",\"taxonID\":52175888,\"parentNameUsageID\":52130142,\"taxonConceptID\":676,\"scientificName\":\"Apoidea\",\"taxonRank\":\"superfamily\",\"source\":\"http://eol.org/pages/676/hierarchy_entries/52175888/overview\"},{\"sourceIdentifier\":\"13025349\",\"taxonID\":52188324,\"parentNameUsageID\":52175888,\"taxonConceptID\":677,\"scientificName\":\"Apidae\",\"taxonRank\":\"family\",\"source\":\"http://eol.org/pages/677/hierarchy_entries/52188324/overview\"},{\"sourceIdentifier\":\"13091160\",\"taxonID\":52191457,\"parentNameUsageID\":52188324,\"taxonConceptID\":104135,\"scientificName\":\"Apis\",\"taxonRank\":\"genus\",\"source\":\"http://eol.org/pages/104135/hierarchy_entries/52191457/overview\"}],\"children\":[]}";

        ArrayList<String> ranks = new ArrayList<String>();
        ArrayList<String> rankNames = new ArrayList<String>();
        new EOLService().parseRankResponse(response, ranks, rankNames, new ArrayList<String>());

        assertThat(StringUtils.join(ranks, " | "), is("Animalia" + CharsetConstant.SEPARATOR
                + "Arthropoda" + CharsetConstant.SEPARATOR
                + "Insecta" + CharsetConstant.SEPARATOR
                + "Hymenoptera" + CharsetConstant.SEPARATOR
                + "Apoidea" + CharsetConstant.SEPARATOR
                + "Apidae" + CharsetConstant.SEPARATOR
                + "Apis" + CharsetConstant.SEPARATOR
                + "Apis mellifera"));

        assertThat(StringUtils.join(rankNames, " | "), is("kingdom" + CharsetConstant.SEPARATOR
                        + "phylum" + CharsetConstant.SEPARATOR
                        + "class" + CharsetConstant.SEPARATOR
                        + "order" + CharsetConstant.SEPARATOR
                        + "superfamily" + CharsetConstant.SEPARATOR
                        + "family" + CharsetConstant.SEPARATOR
                        + "genus" + CharsetConstant.SEPARATOR
                        + "species"));

    }
}

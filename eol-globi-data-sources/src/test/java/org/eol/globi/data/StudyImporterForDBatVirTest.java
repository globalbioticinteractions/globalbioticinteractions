package org.eol.globi.data;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_URL;
import static org.eol.globi.service.TaxonUtil.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class StudyImporterForDBatVirTest {


    @Test
    public void extactPubmed() {
        String pubMed = "<a href='http://www.ncbi.nlm.nih.gov/pubmed/28393313' title='View abstract in PubMed' target=_blank><i>Virol Sin</i> 2017, <b>32</b>(2):101-114</a>";

        String referenceId = StudyImporterForDBatVir.extractFirstPubMedReferenceId(pubMed);
        assertThat(referenceId, Is.is("http://www.ncbi.nlm.nih.gov/pubmed/28393313"));
    }

    @Test
    public void extractCitation() {
        String refs = "&nbsp;&nbsp;Waruhiu C, Ommeh S, Obanda V, Agwanda B, Gakuya F, Ge XY, Yang XL, Wu LJ, Zohaib A, Hu B, Shi ZL,  Molecular detection of viruses in Kenyan bats and discovery of novel astroviruses, caliciviruses and rotaviruses. <a href='http://www.ncbi.nlm.nih.gov/pubmed/28393313' title='View abstract in PubMed' target=_blank><i>Virol Sin</i> 2017, <b>32</b>(2):101-114</a>.  <a href='javascript:void(0)' onClick='javascript:menu2tab4seq(\"P28393313\",\"<i>Virol Sin</i> 2017, <b>32</b>(2):101-114<br>\",\"PMID=28393313\")' title='Show all sequences reported in this reference'><img src='/DBatVir/images/green_plus.gif' width=16 height=14 border=0></a>";
        String citationClean = StudyImporterForDBatVir.scrubReferenceCitation(refs);
        assertThat(citationClean, Is.is("Waruhiu C, Ommeh S, Obanda V, Agwanda B, Gakuya F, Ge XY, Yang XL, Wu LJ, Zohaib A, Hu B, Shi ZL, Molecular detection of viruses in Kenyan bats and discovery of novel astroviruses, caliciviruses and rotaviruses. Virol Sin 2017, 32(2):101-114."));

    }

    @Test
    public void parseInteractions() throws IOException, StudyImporterException {
        InputStream first2 = getClass().getResourceAsStream("/org/eol/globi/data/dbatvir/dbatvir-first2.json");

        List<Map<String, String>> links = new ArrayList<>();
        InteractionListener interactionListener = links::add;

        StudyImporterForDBatVir.parseInteractions(first2, interactionListener);

        assertThat(links.size(), Is.is(2));

        assertThat(links.get(0).get(SOURCE_TAXON_NAME), Is.is("Hipposideros caffer"));
        assertThat(links.get(0).get(SOURCE_TAXON_ID), Is.is("NCBI:302402"));
        assertThat(links.get(0).get(SOURCE_TAXON_PATH), Is.is("Rhinolophidae | Hipposideros caffer"));
        assertThat(links.get(0).get(SOURCE_TAXON_PATH_IDS), Is.is(" | NCBI:302402"));
        assertThat(links.get(0).get(SOURCE_TAXON_PATH_NAMES), Is.is("family | species"));

        assertThat(links.get(0).get(INTERACTION_TYPE_ID), Is.is("http://purl.obolibrary.org/obo/RO_0002453"));
        assertThat(links.get(0).get(INTERACTION_TYPE_NAME), Is.is("hostOf"));

        assertThat(links.get(0).get(TARGET_TAXON_NAME), Is.is("BatCoV5743/KEN/Kwale"));
        assertThat(links.get(0).get(TARGET_TAXON_ID), Is.is("NCBI:1739614"));
        assertThat(links.get(0).get(TARGET_TAXON_PATH), Is.is("Coronaviridae | 229E-related bat coronavirus | BatCoV5743/KEN/Kwale"));
        assertThat(links.get(0).get(TARGET_TAXON_PATH_IDS), Is.is(" | NCBI:1739614 | "));
        assertThat(links.get(0).get(TARGET_TAXON_PATH_NAMES), Is.is("family | species | strain"));

        assertThat(links.get(0).get(StudyImporterForMetaTable.EVENT_DATE), Is.is("2016-03"));
        assertThat(links.get(0).get(LOCALITY_NAME), Is.is("Kenya"));
        assertThat(links.get(0).get(REFERENCE_ID), Is.is("http://www.ncbi.nlm.nih.gov/pubmed/28393313"));
        assertThat(links.get(0).get(REFERENCE_URL), Is.is("http://www.ncbi.nlm.nih.gov/pubmed/28393313"));
        assertThat(links.get(0).get(REFERENCE_CITATION), Is.is("Waruhiu C, Ommeh S, Obanda V, Agwanda B, Gakuya F, Ge XY, Yang XL, Wu LJ, Zohaib A, Hu B, Shi ZL, Molecular detection of viruses in Kenyan bats and discovery of novel astroviruses, caliciviruses and rotaviruses. Virol Sin 2017, 32(2):101-114."));

    }

    @Test
    public void parseInteractionsFull() throws IOException, StudyImporterException {
        InputStream first2 = new GZIPInputStream(getClass().getResourceAsStream("/org/eol/globi/data/dbatvir/dbatvir.json.gz"));

        List<Map<String, String>> links = new ArrayList<>();
        InteractionListener interactionListener = links::add;

        StudyImporterForDBatVir.parseInteractions(first2, interactionListener);

        assertThat(links.size(), Is.is(11164));

        assertThat(links.get(0).get(SOURCE_TAXON_NAME), Is.is("Eptesicus nilssoni"));
        assertThat(links.get(0).get(SOURCE_TAXON_ID), Is.is("NCBI:59451"));

        assertThat(links.get(0).get(INTERACTION_TYPE_ID), Is.is("http://purl.obolibrary.org/obo/RO_0002453"));
        assertThat(links.get(0).get(INTERACTION_TYPE_NAME), Is.is("hostOf"));

        assertThat(links.get(0).get(TARGET_TAXON_NAME), Is.is("ZV2011"));
        assertThat(links.get(0).get(TARGET_TAXON_ID), Is.is("NCBI:2706560"));
        assertThat(links.get(0).get(TARGET_TAXON_PATH), Is.is("Phenuiviridae | Zwiesel bat banyangvirus | ZV2011"));
        assertThat(links.get(0).get(TARGET_TAXON_PATH_IDS), Is.is(" | NCBI:2706560 | "));
        assertThat(links.get(0).get(TARGET_TAXON_PATH_NAMES), Is.is("family | species | strain"));

        assertThat(links.get(0).get(StudyImporterForMetaTable.EVENT_DATE), Is.is("2011"));
        assertThat(links.get(0).get(LOCALITY_NAME), Is.is("Germany"));
        assertThat(links.get(0).get(REFERENCE_ID), Is.is("http://www.ncbi.nlm.nih.gov/pubmed/31992832"));
        assertThat(links.get(0).get(REFERENCE_URL), Is.is("http://www.ncbi.nlm.nih.gov/pubmed/31992832"));
        assertThat(links.get(0).get(REFERENCE_CITATION), Is.is("Kohl C, Brinkmann A, Radonic A, Dabrowski PW, Nitsche A, Muhldorfer K, Wibbelt G, Kurth A, Zwiesel bat banyangvirus, a potentially zoonotic Huaiyangshan banyangvirus (Formerly known as SFTS)-like banyangvirus in Northern bats from Germany. Sci Rep 2020, 10(1):1370."));

    }


}
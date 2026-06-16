package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceServiceLocal;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;

public class DatasetImporterForWebOfLifeTest extends GraphDBNeo4jTestCase {

    @Test
    public void generateArchiveURL() {
        final List<URI> networkNames = Arrays.asList(URI.create("A_HP_002"), URI.create("A_HP_003"));
        URI generatedArchiveURL = DatasetImporterForWebOfLife.generateArchiveURL(networkNames);

        String expectedArchiveURL = "http://www.web-of-life.es/map_download_fast2.php?format=csv&networks=" + "A_HP_002,A_HP_003" + "&species=yes&type=All&data=All&speciesrange=&interactionsrange=&searchbox=&checked=false";

        assertThat(generatedArchiveURL, is(URI.create(expectedArchiveURL)));
    }

    @Test
    public void importSome() throws StudyImporterException {
        DatasetImporterForWebOfLife importer = new DatasetImporterForWebOfLife(null, nodeFactory);
        importer.setDataset(new DatasetLocal(new ResourceServiceLocal(new InputStreamFactoryNoop(), DatasetImporterForWebOfLifeTest.class)));
        importer.importNetworks(URI.create("weboflife/web-of-life_2016-01-15_192434.zip"));
        resolveNames();

        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        List<String> references = new ArrayList<>();
        for (Study allStudy : allStudies) {
            references.add(allStudy.getCitation());
        }

        assertThat(references, hasItem("Arroyo, M.T.K., R. Primack & J.J. Armesto. 1982. Community studies in pollination ecology in the high temperate Andes of central Chile. I. Pollination mechanisms and altitudinal variation. Amer. J. Bot. 69:82-97."));
        assertThat(taxonIndex.findTaxonByName("Diplopterys pubipetala"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Juniperus communis"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Turdus torquatus"), is(notNullValue()));
    }


    @Test
    public void importSomeTrailingSpace() throws StudyImporterException {
        DatasetImporterForWebOfLife importer = new DatasetImporterForWebOfLife(null, nodeFactory);
        importer.setDataset(new DatasetLocal(new ResourceServiceLocal(new InputStreamFactoryNoop(), DatasetImporterForWebOfLifeIT.class)));
        importer.importNetworks(URI.create("weboflife/web-of-life_2026-06-16_200835.zip"));
        resolveNames();

        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        List<String> references = new ArrayList<>();
        for (Study allStudy : allStudies) {
            references.add(allStudy.getCitation());
        }

        assertThat(references, hasItem("Medan, D., N. H. Montaldo, M. Devoto, A. Mantese, V. Vasellati, and N. H. Bartoloni. 2002. Plant-pollinator relationships at two altitudes in the Andes of Mendoza, Argentina. Arctic Antarctic and Alpine Research 34:233-241."));
        assertThat(taxonIndex.findTaxonByName("Agrotis ipsilon "), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Sisyrinchium junceum"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Tarasa humilis "), is(nullValue()));
        assertThat(taxonIndex.findTaxonByName("Tarasa humilis"), is(notNullValue()));
    }


}


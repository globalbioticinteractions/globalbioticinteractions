package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.Dataset;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

public class DatasetImporterForHurlbertTest extends GraphDBNeo4j2TestCase {

    @Test
    public void importSome() throws StudyImporterException, IOException {
        doImport("some/namespace");

        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(2));

        Study study = allStudies.get(0);
        assertThat(study.getCitation(), is("Brown, B. T., W. C. Leibfried, T. R. Huels, and J. A. Olivera. 1991. Prey remains from Bald Eagle nests in Sonora, Mexico. Southwestern Naturalist 36:259-262."));

        study = allStudies.get(1);
        assertThat(study.getCitation(), containsString("Cash, K. J., J. P. Austin-Smith"));

        assertThat(taxonIndex.findTaxonByName("Haliaeetus leucocephalus"), is(notNullValue()));
        Taxon preyTaxon = taxonIndex.findTaxonByName("Ictalurus");
        assertThat(preyTaxon, is(notNullValue()));
        assertThat(preyTaxon.getName(), is("Ictalurus"));
        assertThat(preyTaxon.getExternalId(), is("ITIS:163996"));

        preyTaxon = taxonIndex.findTaxonByName("Cyprinus carpio");
        assertThat(preyTaxon, is(notNullValue()));
        assertThat(preyTaxon.getName(), is("Cyprinus carpio"));
        assertThat(preyTaxon.getExternalId(), not(is("ITIS:unverified")));
    }

    public static InputStream getResource() {
        return DatasetImporterForHurlbertTest.class.getResourceAsStream("hurlbert/avianDietFirst50.txt");
    }

    @Test
    public void importTwice() throws StudyImporterException {
        doImport("some/namespace");
        doImport("some/namespace2");
    }

    public DatasetImporter doImport(final String namespace) throws StudyImporterException {
        DatasetImporter importer = new DatasetImporterForHurlbert(null, nodeFactory);
        Dataset dataset = new DatasetWithResourceMapping(namespace, URI.create("some:uri"), new ResourceServiceLocalAndRemote(inStream -> inStream)) {
            @Override
            public InputStream retrieve(URI name){
                return DatasetImporterForHurlbertTest.getResource();
            }

        };
        importer.setDataset(dataset);
        importStudy(importer);
        return importer;
    }

    @Test
    public void parseDateStringChopDecimal() {
        String dateString = DatasetImporterForHurlbert.getDateString("2018", "1.75");
        assertThat(dateString, is("2018-1"));
    }

    @Test
    public void parseDateStringPropagateUnexpected() {
        String dateString = DatasetImporterForHurlbert.getDateString("2018", "1.FOO");
        assertThat(dateString, is("2018-1.FOO"));
    }

    @Test
    public void parseDateString() {
        String dateString = DatasetImporterForHurlbert.getDateString("2018", "1");
        assertThat(dateString, is("2018-1"));
    }

}

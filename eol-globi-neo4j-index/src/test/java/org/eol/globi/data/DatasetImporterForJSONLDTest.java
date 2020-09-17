package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DatasetImporterForJSONLDTest extends GraphDBTestCase {

    @Test
    public void importStatic() throws StudyImporterException, URISyntaxException {
        DatasetImporter importer = new DatasetImporterForJSONLD(null, nodeFactory);
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"), inStream -> inStream);
        dataset.setConfigURI(URI.create("classpath:/org/eol/globi/data/globi-jsonld/globi-dataset.jsonld"));
        importer.setDataset(dataset);

        importStudy(importer);
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (Study study : allStudies) {
            assertThat(study.getExternalId(), is("http://arctos.database.museum/guid/CUMV:Bird:25225"));
            assertThat(study.getCitation(), is("http://arctos.database.museum/guid/CUMV:Bird:25225"));
            assertThat(study.getSource(), startsWith("Christopher Mungall. 2015. Accessed at <classpath:/org/eol/globi/data/globi-jsonld/globi-dataset.jsonld> on "));
        }
        assertThat(taxonIndex.findTaxonById("NCBI:8782"), not(is(nullValue())));
    }

    @Test(expected = StudyImporterException.class)
    public void importStaticInvalid() throws StudyImporterException, URISyntaxException {
        DatasetImporter importer = new DatasetImporterForJSONLD(null, nodeFactory);
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"), inStream -> inStream);
        dataset.setConfigURI(URI.create("classpath:/org/eol/globi/data/globi-jsonld/globi-dataset.jsonld.invalid"));
        importer.setDataset(dataset);

        importStudy(importer);
    }

}

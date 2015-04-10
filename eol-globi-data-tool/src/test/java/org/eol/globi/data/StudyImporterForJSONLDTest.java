package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForJSONLDTest extends GraphDBTestCase {

    @Test
    public void importStatic() throws StudyImporterException, NodeFactoryException, URISyntaxException {
        StudyImporter importer = new StudyImporterForJSONLD(null, nodeFactory) {
            {
                setResourceUrl("globi-jsonld/globi-dataset.jsonld");
            }
        };
        importer.importStudy();
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (Study allStudy : allStudies) {
            assertThat(allStudy.getExternalId(), is("http://dx.doi.org/http://arctos.database.museum/guid/CUMV:Bird:25225"));
            assertThat(allStudy.getCitation(), is("citation:doi:http://arctos.database.museum/guid/CUMV:Bird:25225"));
            assertThat(allStudy.getSource(), startsWith("Christopher Mungall. 2015. Accessed at globi-jsonld/globi-dataset.jsonld on "));
        }
        assertThat(nodeFactory.findTaxonById("NCBI:8782"), not(is(nullValue())));
    }

}

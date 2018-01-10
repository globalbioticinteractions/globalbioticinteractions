package org.eol.globi.data;

import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetFinderException;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class StudyImporterForHurlbertIT extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, DatasetFinderException {
        Dataset dataset = datasetFor("hurlbertlab/dietdatabase");
        ParserFactory parserFactory = new ParserFactoryForDataset(dataset);
        StudyImporter importer = new StudyImporterForHurlbert(parserFactory, nodeFactory);
        importer.setDataset(dataset);
        importer.setLogger(new ImportLogger() {
            @Override
            public void warn(LogContext study, String message) {
                fail("did not expect warning [" + message + "]");
            }

            @Override
            public void info(LogContext study, String message) {

            }

            @Override
            public void severe(LogContext study, String message) {
                fail("did not expect error [" + message + "]");
            }
        });
        importStudy(importer);
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size() > 10, is(true));

        assertThat(allStudies.get(0).getOriginatingDataset(), is(notNullValue()));

        Taxon formicidae = taxonIndex.findTaxonByName("Formicidae");
        assertThat(formicidae.getExternalId(), is(notNullValue()));
    }

}

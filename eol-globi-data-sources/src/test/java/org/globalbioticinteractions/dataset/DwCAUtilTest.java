package org.globalbioticinteractions.dataset;

import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetImpl;
import org.gbif.dwc.Archive;
import org.gbif.dwc.MetadataException;
import org.gbif.dwc.record.Record;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DwCAUtilTest {

    @Test
    public void emitRecords() throws IOException, URISyntaxException {
        URI archiveURI = getClass().getResource("dwca.zip").toURI();
        String tmpDir = "target/tmp/myarchive";

        Archive dwcArchive = DwCAUtil.archiveFor(archiveURI, tmpDir);
        assertHasRecords(dwcArchive);
    }

    @Test
    public void emitRecordsUnpacked() throws IOException, URISyntaxException {
        URI archiveURI = getClass().getResource("vampire-moth-dwca-master/meta.xml").toURI();

        Archive dwcArchive =  DwCAUtil.archiveFor(Paths.get(archiveURI).getParent().toUri(), null);
        assertHasRecords(dwcArchive);
    }

    private void assertHasRecords(Archive dwcArchive) {
        // Loop over core core records and display id, basis of record and scientific name
        boolean hasRecords = false;
        for (Record rec : dwcArchive.getCore()) {
            hasRecords = true;
            break;
        }
        assertThat(hasRecords, is(true));
    }

    @Test
    public void metaToMetaTables() throws URISyntaxException, IOException, MetadataException {
        Dataset origDataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        URI archiveURI = getClass().getResource("dwca.zip").toURI();
        String tmpDir = "target/tmp/myarchive";

        Archive dwcArchive = DwCAUtil.archiveFor(archiveURI, tmpDir);

        String emlString = dwcArchive.getMetadata();

        Dataset proxy = DwCAUtil.datasetWithEML(origDataset, emlString);

        assertThat(proxy.getCitation(), is ("Occurrence Records for vampire-moths-and-their-fruit-piercing-relatives. 2018-09-18. South Central California Network - 5f573b1a-0e9a-43cf-95d7-299207f98522."));
        assertThat(proxy.getFormat(), is ("application/dwca"));
    }

}

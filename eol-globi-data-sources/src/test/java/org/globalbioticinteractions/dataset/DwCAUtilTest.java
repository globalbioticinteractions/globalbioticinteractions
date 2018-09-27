package org.globalbioticinteractions.dataset;

import org.gbif.dwc.Archive;
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

}

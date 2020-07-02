package org.globalbioticinteractions.dataset;

import org.gbif.dwc.Archive;
import org.gbif.dwc.record.Record;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.containsString;
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

    @Test(expected = IOException.class)
    public void throwIOExceptionNotRuntimeExceptionOnBadDwCA() throws IOException, URISyntaxException {
        URI archiveURI = getClass().getResource("dwcaInvalid.zip").toURI();
        String tmpDir = "target/tmp/myarchive";

        try {
            DwCAUtil.archiveFor(archiveURI, tmpDir);
        } catch(Throwable th) {
            assertThat(th.getMessage(), containsString("dwcaInvalid.zip"));
            assertThat(th.getMessage(), containsString("failed to read"));
            throw th;
        }
    }

    @Test
    public void emitRecordsUnpacked() throws IOException, URISyntaxException {
        URI archiveURI = getClass().getResource("vampire-moth-dwca-main/meta.xml").toURI();

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

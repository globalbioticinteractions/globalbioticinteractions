package org.globalbioticinteractions.dataset;

import org.gbif.dwc.Archive;
import org.gbif.dwc.record.Record;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
public class DwCAUtilTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void emitRecordsForArchive() throws IOException, URISyntaxException {
        URI archiveURI = getClass().getResource("dwca.zip").toURI();

        Archive dwcArchive = DwCAUtil.archiveFor(archiveURI, getTmpDir());
        assertHasRecords(dwcArchive);
    }

    private String getTmpDir() throws IOException {
        String tmpDir = folder.newFolder().getAbsolutePath();
        return tmpDir;
    }

    @Test
    public void emitRecordsForArchiveWithRootDirectory() throws IOException, URISyntaxException {
        URI archiveURI = getClass().getResource("dwca-with-root-directory.zip").toURI();

        Archive dwcArchive = DwCAUtil.archiveFor(archiveURI, getTmpDir());
        assertHasRecords(dwcArchive);
    }

    @Test(expected = IOException.class)
    public void throwIOExceptionNotRuntimeExceptionOnBadDwCA() throws IOException, URISyntaxException {
        URI archiveURI = getClass().getResource("dwcaInvalid.zip").toURI();

        try {
            DwCAUtil.archiveFor(archiveURI, getTmpDir());
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

package org.globalbioticinteractions.dataset;

import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetImpl;
import org.gbif.dwc.Archive;
import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.MetadataException;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DwcTerm;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DwCAUtilTest {

    @Test
    public void emitRecords() throws IOException, URISyntaxException {
        Archive dwcArchive = loadTestArchive();

        // Loop over core core records and display id, basis of record and scientific name
        for (Record rec : dwcArchive.getCore()) {
            System.out.println(String.format("%s: %s (%s)", rec.id(), rec.value(DwcTerm.basisOfRecord), rec.value(DwcTerm.scientificName)));
        }
    }

    @Test
    public void metaToMetaTables() throws URISyntaxException, IOException, MetadataException {
        Dataset origDataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        Archive dwcArchive = loadTestArchive();

        String emlString = dwcArchive.getMetadata();

        Dataset proxy = DwCAUtil.datasetWithEML(origDataset, emlString);

        assertThat(proxy.getCitation(), is ("Occurrence Records for vampire-moths-and-their-fruit-piercing-relatives. 2018-09-18. South Central California Network - 5f573b1a-0e9a-43cf-95d7-299207f98522."));
        assertThat(proxy.getFormat(), is ("application/dwca"));
    }

    private Archive loadTestArchive() throws URISyntaxException, IOException {
        URI archiveURI = getClass().getResource("dwca.zip").toURI();
        String tmpDir = "target/tmp/myarchive";

        return initArchive(archiveURI, tmpDir);
    }

    private Archive initArchive(URI archiveURI, String tmpDir) throws IOException {
        Path myArchiveFile = Paths.get(archiveURI);
        Path extractToFolder = Paths.get(tmpDir);
        return DwcFiles.fromCompressed(myArchiveFile, extractToFolder);
    }

}

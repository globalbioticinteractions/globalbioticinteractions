package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.HttpUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StudyImporterForThessen extends BaseStudyImporter {

    private static final Log LOG = LogFactory.getLog(StudyImporterForThessen.class);

    public static final String RESOURCE = "https://raw.github.com/EOL/pseudonitzchia/master/associations_all_revised.txt";

    public StudyImporterForThessen(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy("Thessen 2014", RESOURCE, null);
        study.setCitationWithTx("A. Thessen. 2014. Species associations extracted from EOL text data objects via text mining. Accessed at " + RESOURCE + " .");
        HttpGet httpGet = new HttpGet(RESOURCE);
        File tmpFile = null;
        InputStream is = null;
        try {
            tmpFile = File.createTempFile("thessen", ".csv");
            LOG.info("remote file to [" + tmpFile.getAbsolutePath() + "] caching...");
            saveResponseToTempFile(httpGet, tmpFile);
            LOG.info("remote file to [" + tmpFile.getAbsolutePath() + "] cached.");
            is = new FileInputStream(tmpFile);
            CSVParser parser = new CSVParser(is, '\t');
            String[] line;
            while ((line = parser.getLine()) != null) {
                if (importFilter.shouldImportRecord((long)parser.lastLineNumber())) {
                    if (line.length == 2) {
                        try {
                            Specimen source = nodeFactory.createSpecimen(null, "EOL:" + line[0]);
                            Specimen target = nodeFactory.createSpecimen(null, "EOL:" + line[1]);
                            source.interactsWith(target, InteractType.INTERACTS_WITH);
                            study.collected(source);
                        } catch (NodeFactoryException e) {
                            throw new StudyImporterException("failed to create nodes on line [" + parser.getLastLineNumber() + "]", e);
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new StudyImporterException("failed to access [" + RESOURCE + "]", e);
        } finally {
            IOUtils.closeQuietly(is);
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
        return study;
    }

    private void saveResponseToTempFile(HttpGet httpGet, File tmpFile) throws IOException {
        HttpResponse response = HttpUtil.createHttpClient().execute(httpGet);
        FileOutputStream fos = new FileOutputStream(tmpFile);
        IOUtils.copy(response.getEntity().getContent(), fos);
        fos.flush();
        IOUtils.closeQuietly(fos);
    }
}

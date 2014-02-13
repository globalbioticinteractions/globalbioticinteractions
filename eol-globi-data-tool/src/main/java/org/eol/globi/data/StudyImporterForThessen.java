package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import org.apache.commons.io.IOUtils;
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
        try {
            tmpFile = File.createTempFile("thessen", ".csv");
            InputStream is = cachedRemoteResource(httpGet, tmpFile);
            CSVParser parser = new CSVParser(is, '\t');
            String[] line;
            while (parser.getLine() != null) {
                if (importFilter.shouldImportRecord((long)parser.lastLineNumber())) {
                    line = parser.getLine();
                    if (line.length == 2) {
                        try {
                            String sourceTaxon = "EOL:" + line[0];
                            String targetTaxon = "EOL:" + line[1];
                            Specimen source = nodeFactory.createSpecimen(sourceTaxon, sourceTaxon);
                            Specimen target = nodeFactory.createSpecimen(targetTaxon, targetTaxon);
                            source.interactsWith(target, InteractType.INTERACTS_WITH);
                            study.collected(source);
                        } catch (NodeFactoryException e) {
                            throw new StudyImporterException("failed to create nodes on line [" + parser.getLastLineNumber() + "]");
                        }
                    }
                }
            }
            IOUtils.closeQuietly(is);
        } catch (IOException e) {
            throw new StudyImporterException("failed to access [" + RESOURCE + "]");
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
        return study;
    }

    private InputStream cachedRemoteResource(HttpGet httpGet, File thessen) throws IOException {
        HttpResponse response = HttpUtil.createHttpClient().execute(httpGet);
        File tmpFile = thessen;
        FileOutputStream fos = new FileOutputStream(tmpFile);
        fos.flush();
        IOUtils.copy(response.getEntity().getContent(), fos);
        return new FileInputStream(tmpFile);
    }
}

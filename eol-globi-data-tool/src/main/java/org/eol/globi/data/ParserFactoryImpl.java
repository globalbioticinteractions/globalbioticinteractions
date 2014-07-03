package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class ParserFactoryImpl implements ParserFactory {

    public LabeledCSVParser createParser(String studyResource, String characterEncoding) throws IOException {
        InputStream is;
        if (studyResource.startsWith("http://")) {
            is = getRemoteInputStream(studyResource);
        } else {
            is = getClass().getResourceAsStream(studyResource);
            if (is == null) {
                throw new IOException("failed to open study resource [" + studyResource + "]");
            }
        }

        Reader reader;
        if (studyResource.endsWith(".gz")) {
            reader = FileUtils.getBufferedReader(is, characterEncoding);
        } else {
            reader = FileUtils.getUncompressedBufferedReader(is, characterEncoding);
        }
        return new LabeledCSVParser(new CSVParser(reader));

    }

    protected InputStream getRemoteInputStream(String studyResource) throws IOException {
        InputStream is;HttpResponse response = HttpUtil.createHttpClient().execute(new HttpGet(studyResource));
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() >= 300) {
            throw new HttpResponseException(statusLine.getStatusCode(),
                    statusLine.getReasonPhrase());
        }
        is = response.getEntity().getContent();
        return is;
    }

}

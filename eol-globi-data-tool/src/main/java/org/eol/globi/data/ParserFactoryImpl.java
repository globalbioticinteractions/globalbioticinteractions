package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.util.HttpUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

public class ParserFactoryImpl implements ParserFactory {

    public LabeledCSVParser createParser(String studyResource, String characterEncoding) throws IOException {
        InputStream is;
        if (StringUtils.startsWith(studyResource, "http://")
                || StringUtils.startsWith(studyResource, "https://")) {
            is = getCachedRemoteInputStream(studyResource);
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

    protected InputStream getCachedRemoteInputStream(String studyResource) throws IOException {
        URI resourceURI = URI.create(studyResource);
        HttpResponse response = HttpUtil.createHttpClient().execute(new HttpGet(resourceURI));
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() >= 300) {
            throw new HttpResponseException(statusLine.getStatusCode(),
                    statusLine.getReasonPhrase());
        }
        return openCachedStream(response);
    }

    private InputStream openCachedStream(HttpResponse response) throws IOException {
        File tempFile = File.createTempFile("globiRemote", "tmp");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        IOUtils.copy(response.getEntity().getContent(), fos);
        fos.flush();
        IOUtils.closeQuietly(fos);
        return new FileInputStream(tempFile);
    }

}

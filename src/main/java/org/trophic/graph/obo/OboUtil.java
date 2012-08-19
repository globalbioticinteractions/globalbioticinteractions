package org.trophic.graph.obo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class OboUtil {

    static public BufferedReader getDefaultBufferedReader() throws IOException {
        InputStream resourceAsStream = OboUtil.class.getResourceAsStream("/org/obofoundry/ncbi_taxonomy.obo.gz");
        GZIPInputStream gzipInputStream = new GZIPInputStream(resourceAsStream);
        return new BufferedReader(new InputStreamReader(gzipInputStream));
    }
}

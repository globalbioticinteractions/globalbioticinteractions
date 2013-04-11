package org.eol.globi.data.taxon;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SingleResourceTaxonReaderFactory implements TaxonReaderFactory {

    private String resourceName = null;

    public SingleResourceTaxonReaderFactory(String resourceName) {
        this.resourceName = resourceName;
    }

    protected BufferedReader getFirstReader() throws IOException {
        InputStream is = getClass().getResourceAsStream(resourceName);
        if (null == is) {
            throw new IOException("failed to find [" + resourceName + "]");
        }
        return FileUtils.getBufferedReader(is, CharsetConstant.UTF8);
    }

    @Override
    public Map<String, BufferedReader> getAllReaders() throws IOException {
        return new HashMap<String, BufferedReader>() {{
            put(resourceName, getFirstReader());
        }};
    }

}

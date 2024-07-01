package org.eol.globi.util;

import org.apache.commons.io.input.BOMInputStream;

import java.io.IOException;
import java.io.InputStream;

public class BOMUtil {

    public static InputStreamFactory factorySkipBOM = new InputStreamFactory() {
        @Override
        public InputStream create(InputStream inStream) throws IOException {
            return new BOMInputStream(inStream);
        }
    };

}

package org.eol.globi.util;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamFactoryNoop implements InputStreamFactory {
    @Override
    public InputStream create(InputStream inStream) throws IOException {
        return inStream;
    }
}

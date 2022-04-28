package org.eol.globi.util;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamFactory {

    InputStream create(InputStream inStream) throws IOException;

}

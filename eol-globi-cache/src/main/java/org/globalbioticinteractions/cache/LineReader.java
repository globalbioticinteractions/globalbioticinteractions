package org.globalbioticinteractions.cache;

import java.io.Closeable;
import java.io.IOException;

public interface LineReader extends Closeable {

    String readLine() throws IOException;


}
